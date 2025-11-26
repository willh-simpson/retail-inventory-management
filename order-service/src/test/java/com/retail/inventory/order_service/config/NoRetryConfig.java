package com.retail.inventory.order_service.config;

import com.retail.inventory.order_service.application.service.RetryService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class NoRetryConfig {
    @Bean
    public RetryService retryService() {
        return mock(RetryService.class);
    }
}
