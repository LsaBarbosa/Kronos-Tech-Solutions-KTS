package com.kts.kronos.application.scheduler;
import com.kts.kronos.application.port.out.provider.MessageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageCleanupScheduler {
    private final MessageProvider messageProvider;

    /**
     * Agenda a exclusão de mensagens antigas.
     * O cron "0 0 1 * * ?" significa: à 1h da manhã, todos os dias.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void cleanupOldMessages() {
        log.info("Iniciando tarefa agendada de limpeza de mensagens antigas.");

        // Define o limite de 30 dias atrás
        var thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        try {
            messageProvider.deleteByCreationDateBefore(thirtyDaysAgo);
            log.info("Limpeza de mensagens concluída com sucesso.");
        } catch (Exception e) {
            log.error("Erro durante a limpeza de mensagens agendada: {}", e.getMessage());
        }
    }
}
