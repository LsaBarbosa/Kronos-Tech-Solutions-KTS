package com.kts.kronos.application.service;

import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
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

    private final KronosMetrics kronosMetrics;
    private final KronosTracing kronosTracing;

    @Value("${kronos.ntp.server:a.st1.ntp.br}")
    private String ntpServer;

    @Value("${kronos.ntp.timeout:5000}")
    private int timeout;

    public NtpTimeService(KronosMetrics kronosMetrics, KronosTracing kronosTracing) {
        this.kronosMetrics = kronosMetrics;
        this.kronosTracing = kronosTracing;
    }

    protected NtpTimeService() {
        this(new KronosMetrics(), new KronosTracing());
    }

    /**
     * Consulta o servidor NTP e retorna o "offset" (diferença) em milissegundos.
     * Retorna null se falhar (para não travar a aplicação).
     */
    public Long getNetworkTimeOffset() {
        return kronosTracing.observe("kronos.ntp.check", () -> {
            var client = createClient();
            client.setDefaultTimeout(timeout);

            try {
                client.open();
                var hostAddr = InetAddress.getByName(ntpServer);
                var info = client.getTime(hostAddr);
                info.computeDetails();

                var offset = info.getOffset();
                kronosMetrics.setNtpDriftMillis(offset);
                log.info("event=ntp_check result=success");
                return offset;
            } catch (IOException e) {
                kronosMetrics.setNtpDriftMillis(null);
                log.warn("event=ntp_check result=failure reason=io");
                return null;
            } finally {
                if (client.isOpen()) {
                    client.close();
                }
            }
        });
    }

    /**
     * Valida se o relógio está dentro do limite aceitável.
     * Atualmente apenas LOGA o erro crítico, mas pode ser configurado para lançar exceção.
     * * @param maxDriftSeconds Limite aceitável de desvio em segundos.
     */
    public void validateSystemTime(int maxDriftSeconds) {
        var offset = getNetworkTimeOffset();

        if (offset != null && Math.abs(offset) > (maxDriftSeconds * 1000L)) {
            log.error("event=ntp_check result=failure reason=drift_limit_exceeded");

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
