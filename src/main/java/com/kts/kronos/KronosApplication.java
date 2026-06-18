package com.kts.kronos;

import com.kts.kronos.adapter.out.storage.S3DocumentBucketProperties;
import com.kts.kronos.application.config.KronosRedisProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableConfigurationProperties({
        S3DocumentBucketProperties.class,
        KronosRedisProperties.class
})
public class KronosApplication {

	public static void main(String[] args) {
		SpringApplication.run(KronosApplication.class, args);
	}

    @Bean
    public HttpExchangeRepository httpExchangeRepository() {
        return new InMemoryHttpExchangeRepository();
    }

}
