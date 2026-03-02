package com.gemstore.backend.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka topic configuration.
 * Topics are auto-created on application startup.
 */
@Configuration
public class KafkaTopicConfig {

    @Value("${messaging.kafka.topics.messages:gemstore.messages}")
    private String messagesTopic;

    @Value("${messaging.kafka.topics.message-status:gemstore.message-status}")
    private String messageStatusTopic;

    @Value("${messaging.kafka.topics.typing:gemstore.typing}")
    private String typingTopic;

    @Value("${messaging.kafka.partitions:3}")
    private int partitions;

    @Value("${messaging.kafka.replicas:1}")
    private int replicas;

    /**
     * Main messages topic.
     * Partitioned by conversation for message ordering.
     */
    @Bean
    public NewTopic messagesTopic() {
        return TopicBuilder.name(messagesTopic)
                .partitions(partitions)
                .replicas(replicas)
                .config("retention.ms", "604800000")  // 7 days retention
                .config("cleanup.policy", "delete")
                .build();
    }

    /**
     * Message status updates topic.
     * For delivery confirmations and read receipts.
     */
    @Bean
    public NewTopic messageStatusTopic() {
        return TopicBuilder.name(messageStatusTopic)
                .partitions(partitions)
                .replicas(replicas)
                .config("retention.ms", "86400000")  // 1 day retention
                .config("cleanup.policy", "delete")
                .build();
    }

    /**
     * Typing indicators topic.
     * Compacted to keep only latest typing state per user pair.
     */
    @Bean
    public NewTopic typingTopic() {
        return TopicBuilder.name(typingTopic)
                .partitions(partitions)
                .replicas(replicas)
                .config("retention.ms", "60000")  // 1 minute retention
                .config("cleanup.policy", "compact")
                .config("min.cleanable.dirty.ratio", "0.1")
                .config("segment.ms", "60000")
                .build();
    }
}