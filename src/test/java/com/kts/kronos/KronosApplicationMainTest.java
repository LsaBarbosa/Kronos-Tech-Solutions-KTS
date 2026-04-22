package com.kts.kronos;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mockStatic;

class KronosApplicationMainTest {

    @Test
    void shouldDelegateMainToSpringApplication() {
        String[] args = {"--spring.profiles.active=test"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            KronosApplication.main(args);

            springApplication.verify(() -> SpringApplication.run(KronosApplication.class, args));
        }
    }

    @Test
    void shouldExposeHttpExchangeRepositoryBean() {
        assertInstanceOf(
                InMemoryHttpExchangeRepository.class,
                new KronosApplication().httpExchangeRepository()
        );
    }
}
