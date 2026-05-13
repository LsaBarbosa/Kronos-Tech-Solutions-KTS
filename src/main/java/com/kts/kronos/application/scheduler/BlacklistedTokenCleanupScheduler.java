package com.kts.kronos.application.scheduler;

import com.kts.kronos.adapter.out.persistence.BlacklistedTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.kts.kronos.constants.Messages.SAO_PAULO;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistedTokenCleanupScheduler {
    private final BlacklistedTokenRepository repository;

    @Scheduled(cron = "0 30 3 * * ?", zone = "America/Sao_Paulo")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now(SAO_PAULO);
        repository.deleteExpiredTokens(now);
        log.info("Limpeza de tokens blacklist expirados concluída.");
    }
}
