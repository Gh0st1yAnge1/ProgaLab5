package org.Gh0st1yAnge1.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.Gh0st1yAnge1.AuditEvent;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * Продюсер аудита — отправляет AuditEvent в топик Kafka в виде JSON.
 *
 * Основано на официальной документации Kafka Java Producer API:
 * https://kafka.apache.org/documentation/#producerapi
 *
 * Ключевые настройки продюсера (из документации):
 *   bootstrap.servers  — адрес брокера (kafka.apache.org/documentation/#producerconfigs)
 *   acks=all           — ждать подтверждения от всех реплик (надёжность)
 *   retries            — количество попыток при временных ошибках
 *   key.serializer     — сериализатор ключа (тип операции → String)
 *   value.serializer   — сериализатор значения (JSON → String)
 *
 * Отправка асинхронная (fire-and-forget с колбэком на ошибку) —
 * аудит не должен блокировать основную логику сервера.
 */
public class AuditProducer implements AutoCloseable {

    private static final String TOPIC = "audit-log";
    private static final Logger logger = Logger.getLogger(AuditProducer.class.getName());

    private final KafkaProducer<String, String> producer;
    private final ObjectMapper objectMapper;

    // Множество CRUD-команд, которые нужно аудировать
    private static final java.util.Set<String> AUDITED_COMMANDS = java.util.Set.of(
            "INSERT", "UPDATE", "REMOVE_KEY", "REMOVE_GREATER_KEY",
            "REMOVE_GREATER", "REPLACE_IF_LOWER", "CLEAR"
    );

    public AuditProducer(String bootstrapServers) {
        this.objectMapper = new ObjectMapper();

        // Конфигурация продюсера согласно документации Kafka:
        // https://kafka.apache.org/documentation/#producerconfigs
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // acks=all — ждём подтверждения от лидера и всех реплик
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        // Повторные попытки при временных ошибках сети
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        // Если Kafka недоступна — не ждать дольше 2 секунд
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2000);

        this.producer = new KafkaProducer<>(props);
        logger.info("AuditProducer initialized. Bootstrap: " + bootstrapServers);
    }

    /**
     * Отправляет событие аудита в Kafka если команда требует аудита.
     * Отправка асинхронная — не блокирует основной поток сервера.
     *
     * @param commandType тип команды из CommandType enum
     * @param argument    аргумент команды (ключ, ID и т.д.)
     * @param success     успешно ли выполнена команда
     * @param message     ответное сообщение
     */
    public void sendIfAuditable(String commandType, String argument, boolean success, String message) {
        if (!AUDITED_COMMANDS.contains(commandType)) return;

        try {
            AuditEvent event = new AuditEvent(commandType, argument, success, message);
            String json = objectMapper.writeValueAsString(event);

            // Ключ = commandType — позволяет партиционировать по типу операции
            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, commandType, json);

            // Асинхронная отправка с колбэком на ошибку (документация Kafka, Producer API)
            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    logger.warning("Failed to send audit event: " + exception.getMessage());
                } else {
                    logger.fine("Audit sent: topic=" + metadata.topic()
                            + " partition=" + metadata.partition()
                            + " offset=" + metadata.offset());
                }
            });
        } catch (Exception e) {
            // Аудит не должен ронять сервер — только логируем
            logger.warning("Audit serialization error: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        // flush() гарантирует отправку всех буферизованных сообщений перед закрытием
        // (документация Kafka: KafkaProducer.flush())
        producer.flush();
        producer.close();
        logger.info("AuditProducer closed.");
    }
}
