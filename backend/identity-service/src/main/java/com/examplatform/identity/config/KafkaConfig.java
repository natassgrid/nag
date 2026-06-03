package com.examplatform.identity.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic auditEventsTopic() {
        return TopicBuilder.name("exam.audit.events")
                .partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name("exam.notifications.outbound")
                .partitions(3).replicas(1).build();
    }
}
