package com.kts.kronos.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class AsyncConfigTest {

    @Test
    @DisplayName("mailTaskExecutor: deve configurar pool dedicado para envio de email")
    void shouldCreateMailTaskExecutor() {
        var executor = new AsyncConfig().mailTaskExecutor();
        ThreadPoolTaskExecutor taskExecutor = assertInstanceOf(ThreadPoolTaskExecutor.class, executor);

        assertEquals(2, taskExecutor.getCorePoolSize());
        assertEquals(8, taskExecutor.getMaxPoolSize());
        assertEquals("mail-async-", taskExecutor.getThreadNamePrefix());

        taskExecutor.shutdown();
    }
}
