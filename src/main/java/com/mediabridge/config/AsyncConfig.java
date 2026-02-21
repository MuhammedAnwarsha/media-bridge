package com.mediabridge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("videoExecutor")
    public Executor videoExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);        // Only 2 conversions at a time
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);      // Queue others
        executor.setThreadNamePrefix("Video-");
        executor.initialize();

        return executor;
    }
}
