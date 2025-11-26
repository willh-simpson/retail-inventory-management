package com.retail.inventory.order_service.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.testcontainers.containers.MongoDBContainer;

@TestConfiguration
public class TestMongoConfig {
    private static final MongoDBContainer container = new MongoDBContainer("mongo:7.0.0").withReuse(true);

    static {
        container.start();
    }

    @Bean
    public MongoTemplate mongoTemplate() {
        String uri = container.getReplicaSetUrl("testdb");
        return new MongoTemplate(new SimpleMongoClientDatabaseFactory(uri));
    }
}
