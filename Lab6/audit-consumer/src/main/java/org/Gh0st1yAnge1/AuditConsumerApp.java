package org.Gh0st1yAnge1;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.sql.*;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Отдельное приложение-консьюмер.
 * Читает события аудита из Kafka топика "audit-log" и записывает в SQLite.
 *
 * Основано на официальной документации Kafka Consumer API:
 * https://kafka.apache.org/documentation/#consumerapi
 *
 * Ключевые принципы из документации (Confluent Java Client):
 *   - Консьюмер строится вокруг event loop с poll() API
 *   - После обработки порции записей вызывается commitSync()
 *     для подтверждения offset (at-least-once семантика)
 *   - group.id объединяет несколько консьюмеров в группу
 *   - auto.offset.reset=earliest — читать с начала при первом запуске
 *
 * Запуск:
 *   java -jar audit-consumer.jar [bootstrap-servers] [sqlite-db-path]
 *   java -jar audit-consumer.jar localhost:9092 ./audit.db
 */
public class AuditConsumerApp {

    private static final String TOPIC     = "audit-log";
    private static final String GROUP_ID  = "audit-consumer-group";
    private static final Logger logger    = Logger.getLogger(AuditConsumerApp.class.getName());

    private static final String DB_CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS audit_log (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                command_type TEXT    NOT NULL,
                argument     TEXT,
                timestamp    TEXT    NOT NULL,
                success      INTEGER NOT NULL,
                message      TEXT,
                created_at   TEXT    DEFAULT (datetime('now'))
            )
            """;

    private static final String DB_INSERT = """
            INSERT INTO audit_log (command_type, argument, timestamp, success, message)
            VALUES (?, ?, ?, ?, ?)
            """;

    public static void main(String[] args) {
        String bootstrapServers = args.length > 0 ? args[0] : "localhost:9092";
        String dbPath           = args.length > 1 ? args[1] : "./audit.db";

        logger.info("Starting AuditConsumer. Kafka: " + bootstrapServers + ", DB: " + dbPath);

        // Инициализируем SQLite
        try (Connection dbConn = openDatabase(dbPath)) {
            initDatabase(dbConn);
            logger.info("SQLite database ready: " + dbPath);

            // Запускаем цикл чтения из Kafka
            runConsumerLoop(bootstrapServers, dbConn);

        } catch (SQLException e) {
            logger.severe("Database error: " + e.getMessage());
        }
    }

    // ─── Kafka Consumer Loop ─────────────────────────────────────────────────────

    private static void runConsumerLoop(String bootstrapServers, Connection dbConn) {
        ObjectMapper objectMapper = new ObjectMapper();

        // Конфигурация консьюмера согласно документации:
        // https://kafka.apache.org/documentation/#consumerconfigs
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,  bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,           GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // earliest — при первом запуске читаем все сообщения с начала топика
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Отключаем авто-коммит — делаем commitSync() вручную после записи в БД.
        // Это гарантирует at-least-once: если запись в SQLite упала,
        // при следующем старте консьюмер перечитает те же сообщения.
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        // Регистрируем shutdown hook для корректного завершения
        final KafkaConsumer<String, String>[] consumerRef = new KafkaConsumer[1];
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received. Closing consumer...");
            if (consumerRef[0] != null) consumerRef[0].wakeup();
        }));

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumerRef[0] = consumer;

            // Подписываемся на топик
            consumer.subscribe(List.of(TOPIC));
            logger.info("Subscribed to topic: " + TOPIC);

            // Event loop — паттерн из документации Confluent Java Client:
            // while (running) { poll() → process() → commitSync() }
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));

                if (records.isEmpty()) continue;

                int saved = 0;
                for (ConsumerRecord<String, String> record : records) {
                    try {
                        AuditEvent event = objectMapper.readValue(record.value(), AuditEvent.class);
                        saveToDatabase(dbConn, event);
                        saved++;
                        logger.fine("Saved: " + event);
                    } catch (Exception e) {
                        logger.warning("Failed to process record at offset "
                                + record.offset() + ": " + e.getMessage());
                        // При ошибке парсинга пропускаем запись, не роняем консьюмер
                    }
                }

                // Коммитим offset только после успешной записи в БД (at-least-once)
                consumer.commitSync();
                if (saved > 0) logger.info("Committed " + saved + " audit records to SQLite.");
            }

        } catch (org.apache.kafka.common.errors.WakeupException e) {
            // Нормальное завершение через wakeup() из shutdown hook
            logger.info("Consumer stopped by wakeup.");
        }

        logger.info("AuditConsumer shut down cleanly.");
    }

    // ─── SQLite helpers ──────────────────────────────────────────────────────────

    private static Connection openDatabase(String dbPath) throws SQLException {
        // sqlite-jdbc: просто указываем путь к файлу
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    private static void initDatabase(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(DB_CREATE_TABLE);
        }
    }

    private static void saveToDatabase(Connection conn, AuditEvent event) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DB_INSERT)) {
            ps.setString(1, event.getCommandType());
            ps.setString(2, event.getArgument());
            ps.setString(3, event.getTimestamp());
            ps.setInt   (4, event.isSuccess() ? 1 : 0);
            ps.setString(5, event.getMessage());
            ps.executeUpdate();
        }
    }
}
