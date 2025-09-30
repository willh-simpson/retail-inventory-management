package com.retail.inventory.order_service.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.retail.inventory.order_service.domain.model.ProductSnapshotEntity;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {
    @Bean
    public ConsumerFactory<String, ProductSnapshotEntity> consumerFactory(ObjectMapper mapper) {
        JsonDeserializer<ProductSnapshotEntity> deserializer = new JsonDeserializer<>(ProductSnapshotEntity.class, mapper, false);
        deserializer.addTrustedPackages("*"); // allows deserialization for package

        return new DefaultKafkaConsumerFactory<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092",
                        ConsumerConfig.GROUP_ID_CONFIG, "order-service",
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
                ),
                new StringDeserializer(),
                deserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProductSnapshotEntity> kafkaListenerContainerFactory(ConsumerFactory<String, ProductSnapshotEntity> cf) {
        ConcurrentKafkaListenerContainerFactory<String, ProductSnapshotEntity> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);

        return factory;
    }
}
