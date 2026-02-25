package com.kts.kronos.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class EmailRetryConfig {

    @Bean
    public RetryTemplate emailRetryTemplate(
            @Value("${mail.retry.max-attempts:3}") int maxAttempts,
            @Value("${mail.retry.initial-interval-ms:1000}") long initialIntervalMs,
            @Value("${mail.retry.multiplier:2.0}") double multiplier,
            @Value("${mail.retry.max-interval-ms:15000}") long maxIntervalMs
    ) {
        var template = new RetryTemplate();

        var retryPolicy = new SimpleRetryPolicy(maxAttempts);
        template.setRetryPolicy(retryPolicy);

        var backoff = new ExponentialBackOffPolicy();
        backoff.setInitialInterval(initialIntervalMs);
        backoff.setMultiplier(multiplier);
        backoff.setMaxInterval(maxIntervalMs);
        template.setBackOffPolicy(backoff);

        return template;
    }
}
