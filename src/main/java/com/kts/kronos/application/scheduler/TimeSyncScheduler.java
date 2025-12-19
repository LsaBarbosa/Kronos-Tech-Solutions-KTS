package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.service.NtpTimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimeSyncScheduler {

    private final NtpTimeService ntpService;

    @Value("${kronos.ntp.max-drift-seconds:5}")
    private int maxDriftSeconds;

    // Executa a cada 1 hora (3600000 ms)
    @Scheduled(fixedRate = 3600000) 
    public void checkTimeSynchronization() {
        log.info("Iniciando verificação periódica de sincronismo de tempo (NTP)...");
        
        Long offset = ntpService.getNetworkTimeOffset();
        
        if (offset == null) {
            log.warn("Não foi possível verificar o sincronismo de tempo. Verifique a conexão com a internet ou firewall (Porta UDP 123).");
            return;
        }

        long absOffset = Math.abs(offset);
        if (absOffset > (maxDriftSeconds * 1000L)) {
            log.error("ALERTA CRÍTICO DE CONFORMIDADE: O relógio do servidor está desviado em {}ms (Limite: {}ms). Ajuste o horário do servidor imediatamente para garantir validade jurídica dos pontos.", offset, maxDriftSeconds * 1000);
        } else {
            log.info("Relógio sincronizado. Desvio atual: {}ms (Dentro do limite de {}s).", offset, maxDriftSeconds);
        }
    }
}