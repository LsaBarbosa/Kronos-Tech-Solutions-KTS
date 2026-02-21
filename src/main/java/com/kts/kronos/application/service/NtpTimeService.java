package com.kts.kronos.application.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;

import static com.kts.kronos.constants.Logs.*;
import static com.kts.kronos.constants.Messages.INTERNAL_CLOCK_OUT_OF_SYNC;

@Slf4j
@Service
public class NtpTimeService {


    @Value("${kronos.ntp.server:a.st1.ntp.br}")
    private String ntpServer;

    @Value("${kronos.ntp.timeout:5000}")
    private int timeout;

    /**
     * Consulta o servidor NTP e retorna o "offset" (diferença) em milissegundos.
     * Retorna null se falhar (para não travar a aplicação).
     */
    public Long getNetworkTimeOffset() {
        // Usa o método protegido para permitir Mock nos testes
        var client = createClient();
        var timeoutDuration = Duration.ofMillis(timeout);
        try {
            client.open();
            var hostAddr = InetAddress.getByName(ntpServer);
            client.setSoTimeout(timeoutDuration);
            // Faz a consulta
            var info = client.getTime(hostAddr);
            info.computeDetails(); // Essencial para calcular o offset

            // Offset: Diferença entre (NTP) e (Sistema Local)
            var offset = info.getOffset();

            log.debug(LOG_SYNC_SUCCESS, ntpServer, offset);
            return offset;

        } catch (IOException e) {
            log.warn(LOG_SYNC_WARN, ntpServer, e.getMessage());
            return null;
        } finally {
            // Garante o fechamento da porta UDP para evitar vazamento de memória (Memory Leak)
            if (client.isOpen()) {
                client.close();
            }
        }
    }

    /**
     * Valida se o relógio está dentro do limite aceitável.
     * Lança exceção se o desvio for maior que o permitido.
     * * @param maxDriftSeconds Limite aceitável de desvio em segundos.
     */
    public void validateSystemTime(int maxDriftSeconds) {
        var offset = getNetworkTimeOffset();

        // Otimização: calcula os milissegundos apenas uma vez
        long maxDriftMillis = maxDriftSeconds * 1000L;

        if (offset != null && Math.abs(offset) > maxDriftMillis) {
            // Otimização: delega a formatação da string para o SLF4J, evitando o String.format
            log.error(LOG_SYNC_CRITICAL, offset, maxDriftMillis);

            throw new IllegalStateException(INTERNAL_CLOCK_OUT_OF_SYNC);
        }
    }

    /**
     * Método protegido para facilitar testes unitários (Mock do Client).
     */
    protected NTPUDPClient createClient() {
        return new NTPUDPClient();
    }
}