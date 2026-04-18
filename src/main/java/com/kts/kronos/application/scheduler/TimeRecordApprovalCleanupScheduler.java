package com.kts.kronos.application.scheduler;
import com.kts.kronos.adapter.out.persistence.TimeRecordApprovalRepository;
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
public class TimeRecordApprovalCleanupScheduler {
    private final TimeRecordApprovalRepository repository;
    private static final int DAYS_TO_KEEP = 31;

    @Scheduled(cron = "0 30 2 * * ?", zone = "America/Sao_Paulo")
    @Transactional
    public void cleanupOldApprovals() {
        log.info("Iniciando tarefa agendada de limpeza de solicitações de aprovação de ponto não resolvidas.");
        var threshold = LocalDateTime.now(SAO_PAULO).minusDays(DAYS_TO_KEEP);

        try {
            repository.deleteByCreatedAtBefore(threshold);
            log.info("Limpeza de solicitações de aprovação concluída. Registros anteriores a {} foram removidos.", threshold);
        } catch (RuntimeException e) {
            log.error("Erro durante a limpeza de solicitações de aprovação agendada: {}", e.getMessage(), e);
        }
    }
}
