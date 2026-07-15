package com.streamsync.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka Topic Configuration
 *
 * Topics are created automatically by the Config Server on startup
 * if they don't already exist (requires auto.create.topics.enable=true
 * on the broker, or AdminClient permissions).
 *
 * Key topic settings:
 *
 * cleanup.policy=compact
 *   Log compaction: Kafka retains only the LATEST message per key.
 *   This means "config:timeout_ms = 3000" overwrites "config:timeout_ms = 5000"
 *   in the compacted log. A newly-joining consumer replaying from offset 0
 *   gets current state, not full history. This is what makes Kafka work as a
 *   config store, not just an event log.
 *
 * partitions=3
 *   3 partitions = up to 3 consumers in a consumer group can process in parallel.
 *   In our case, each microservice is its own consumer group, so partitioning
 *   mainly helps if you scale to many config keys with high update rate.
 *
 * replicas=1
 *   Single-node local setup. In production: set to 3 for HA.
 *
 * min.insync.replicas=1
 *   With replicas=1 this must be 1. In production with replicas=3, set to 2.
 */
@Configuration
public class KafkaTopicConfig {

    private static final int PARTITIONS = 3;
    private static final int REPLICAS   = 1;  // Change to 3 in production

    @Bean
    public NewTopic paymentsConfigTopic() {
        return TopicBuilder.name("config.payments")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG,
                        TopicConfig.CLEANUP_POLICY_COMPACT)
                .build();
    }

    @Bean
    public NewTopic authConfigTopic() {
        return TopicBuilder.name("config.auth")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG,
                        TopicConfig.CLEANUP_POLICY_COMPACT)
                .build();
    }

    @Bean
    public NewTopic inventoryConfigTopic() {
        return TopicBuilder.name("config.inventory")
                .partitions(PARTITIONS)
                .replicas(REPLICAS)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG,
                        TopicConfig.CLEANUP_POLICY_COMPACT)
                .build();
    }
}
