package com.example.transactionservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.testcontainers.containers.KafkaContainer;

@Import(Containers.class)
@TestConfiguration
public class KafkaTestConfig {

    @Autowired
    KafkaContainer kafka;

    @Bean
    KafkaTemplate<String, Object> kafkaTemplate(KafkaProperties properties) {
        var producerProperties = properties.buildProducerProperties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        DefaultKafkaProducerFactory<String, Object> pf = new DefaultKafkaProducerFactory<>(
                producerProperties,
                new StringSerializer(),
                new JsonSerializer<>());
        return new KafkaTemplate<>(pf);
    }
}
