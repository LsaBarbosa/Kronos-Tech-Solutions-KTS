package com.kts.kronos.application.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;

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
        client.setDefaultTimeout(timeout);

        try {
            client.open();
            var hostAddr = InetAddress.getByName(ntpServer);

            // Faz a consulta
            var info = client.getTime(hostAddr);
            info.computeDetails(); // Essencial para calcular o offset

            // Offset: Diferença entre (NTP) e (Sistema Local)
            var offset = info.getOffset();

            log.debug("Sincronismo NTP realizado com sucesso. Server: {}, Offset: {}ms", ntpServer, offset);
            return offset;

        } catch (IOException e) {
            log.warn("Falha ao consultar servidor NTP ({}): {}. O sistema continuará operando com o relógio local.", ntpServer, e.getMessage());
            return null;
        } finally {
            // Garante o fechamento da porta UDP
            if (client.isOpen()) {
                client.close();
            }
        }
    }

    /**
     * Valida se o relógio está dentro do limite aceitável.
     * Atualmente apenas LOGA o erro crítico, mas pode ser configurado para lançar exceção.
     * * @param maxDriftSeconds Limite aceitável de desvio em segundos.
     */
    public void validateSystemTime(int maxDriftSeconds) {
        var offset = getNetworkTimeOffset();

        if (offset != null && Math.abs(offset) > (maxDriftSeconds * 1000L)) {
            var msg = String.format("ALERTA CRÍTICO: RELÓGIO DO SERVIDOR DESSINCRONIZADO! Diferença de %d ms detectada. O limite é %d ms.", offset, maxDriftSeconds * 1000);
            log.error(msg);

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