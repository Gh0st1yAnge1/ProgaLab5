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

        try (Connection dbConn = openDatabase(dbPath)) {
            initDatabase(dbConn);
            logger.info("SQLite database ready: " + dbPath);

            runConsumerLoop(bootstrapServers, dbConn);

        } catch (SQLException e) {
            logger.severe("Database error: " + e.getMessage());
        }
    }

    private static void runConsumerLoop(String bootstrapServers, Connection dbConn) {
        ObjectMapper objectMapper = new ObjectMapper();

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,  bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,           GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        final KafkaConsumer<String, String>[] consumerRef = new KafkaConsumer[1];
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown signal received. Closing consumer...");
            if (consumerRef[0] != null) consumerRef[0].wakeup();
        }));

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumerRef[0] = consumer;

            consumer.subscribe(List.of(TOPIC));
            logger.info("Subscribed to topic: " + TOPIC);

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
                    }
                }

                consumer.commitSync();
                if (saved > 0) logger.info("Committed " + saved + " audit records to SQLite.");
            }

        } catch (org.apache.kafka.common.errors.WakeupException e) {
            logger.info("Consumer stopped by wakeup.");
        }

        logger.info("AuditConsumer shut down cleanly.");
    }

    private static Connection openDatabase(String dbPath) throws SQLException {
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
