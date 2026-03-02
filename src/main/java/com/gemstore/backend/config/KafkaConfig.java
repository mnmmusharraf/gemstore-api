package com.gemstore.backend.config;

import com.gemstore.backend.dtos.message.MessageEventDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for messaging system.
 */
@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:gemstore-messaging}")
    private String groupId;

    // ==================== PRODUCER CONFIG ====================

    @Bean
    public ProducerFactory<String, MessageEventDto> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Basic config
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Performance tuning
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        // Timeout settings
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, MessageEventDto> kafkaTemplate() {
        KafkaTemplate<String, MessageEventDto> template = new KafkaTemplate<>(producerFactory());
        template.setObservationEnabled(true);
        return template;
    }

    // ==================== CONSUMER CONFIG ====================

    @Bean
    public ConsumerFactory<String, MessageEventDto> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Basic config
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // Offset management
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // JSON deserializer settings
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.gemstore.backend.dtos.message");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, MessageEventDto.class.getName());
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);

        // Performance tuning
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1);
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);

        // Session management
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MessageEventDto> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, MessageEventDto> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        // Concurrency - number of consumer threads
        factory.setConcurrency(3);

        // Manual acknowledgment
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Error handling with retry
        factory.setCommonErrorHandler(errorHandler());

        // Batch listening disabled (process one at a time for ordering)
        factory.setBatchListener(false);

        return factory;
    }

    /**
     * Error handler with retry logic.
     */
    @Bean
    public DefaultErrorHandler errorHandler() {
        // Retry 3 times with 1 second interval, then give up
        FixedBackOff backOff = new FixedBackOff(1000L, 3L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler((record, exception) -> {
            // This is called after all retries are exhausted
            log.error("❌ Message processing failed after retries: topic={}, partition={}, offset={}, error={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    exception.getMessage());

            // TODO: Send to dead letter topic
            // deadLetterProducer.send(record, exception);
        }, backOff);

        // Don't retry for these exceptions
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class
        );

        return errorHandler;
    }
}