package com.practica.apx.connector.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declaracion del topico (perfil 'kafka'). Spring lo crea en el broker al
 * arrancar si no existe (idempotente).
 */
@Configuration
@Profile("kafka")
public class KafkaTopicConfig {

    @Bean
    public NewTopic topicoTransferencias(
            @Value("${app.kafka.topico-transferencias}") String topico) {
        return TopicBuilder.name(topico)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
