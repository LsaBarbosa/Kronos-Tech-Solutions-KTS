package com.kts.kronos.application.scheduler;


import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
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
public class PasswordTokenCleanupScheduler {
    private final PasswordResetTokenRepository repository;

    @Scheduled(cron = "0 0 2 * * ?", zone = "America/Sao_Paulo")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Iniciando tarefa agendada de limpeza de tokens de redefinição de senha expirados.");

        // Usa o fuso horário de São Paulo, o mesmo usado para calcular expiryDate.
        var now = LocalDateTime.now(SAO_PAULO);

        try {
            // O repositório executa a query DELETE FROM WHERE expiryDate <= :now
            repository.deleteExpiredTokens(now);
            log.info("Limpeza de tokens concluída com sucesso.");
        } catch (RuntimeException e) {
            log.error("Erro durante a limpeza de tokens agendada: {}", e.getMessage(), e);
        }
    }

}
