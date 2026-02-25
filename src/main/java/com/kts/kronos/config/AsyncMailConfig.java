package com.kts.kronos.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
@Configuration
public class AsyncMailConfig {

    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor(
            @Value("${mail.async.core-pool-size:2}") int corePoolSize,
            @Value("${mail.async.max-pool-size:6}") int maxPoolSize,
            @Value("${mail.async.queue-capacity:200}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("mail-async-");
        executor.initialize();
        return executor;
    }
}
