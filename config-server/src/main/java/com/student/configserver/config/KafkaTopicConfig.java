package com.student.configserver.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the three config topics on startup. Spring Kafka will create them
 * automatically against the local KRaft broker if they don't already exist
 * (as long as auto.create.topics.enable isn't disabled on the broker).
 *
 * One topic per service keeps things simple: each microservice only ever
 * subscribes to its own topic, so there's no filtering logic needed on the
 * consumer side.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic paymentsConfigTopic() {
        return TopicBuilder.name("config.payments").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic authConfigTopic() {
        return TopicBuilder.name("config.auth").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryConfigTopic() {
        return TopicBuilder.name("config.inventory").partitions(1).replicas(1).build();
    }
}
