package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.platform.PlatformHealthMetricsResponse;
import com.kts.kronos.adapter.in.web.dto.platform.PlatformHealthResponse;
import com.kts.kronos.adapter.in.web.dto.platform.PlatformHealthSignalResponse;
import com.kts.kronos.adapter.out.persistence.CompanyRepository;
import com.kts.kronos.adapter.out.persistence.LgpdRequestRepository;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.observability.application.ObservabilityStatusUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlatformHealthService {

    private static final ZoneId SAO_PAULO_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final List<LgpdRequestStatus> PENDING_LGPD_STATUSES = List.of(
            LgpdRequestStatus.OPEN,
            LgpdRequestStatus.IN_ANALYSIS,
            LgpdRequestStatus.WAITING_CONTROLLER,
            LgpdRequestStatus.WAITING_LEGAL_REVIEW,
            LgpdRequestStatus.WAITING_DATA_SUBJECT,
            LgpdRequestStatus.APPROVED_FOR_EXPORT
    );

    private final ObservabilityStatusUseCase observabilityStatusUseCase;
    private final CompanyRepository companyRepository;
    private final LgpdRequestRepository lgpdRequestRepository;

    public PlatformHealthResponse getPlatformHealth() {
        var checkedAt = OffsetDateTime.now(SAO_PAULO_ZONE);
        var signals = new ArrayList<PlatformHealthSignalResponse>();

        String databaseState = "OPERATIONAL";
        String databaseDescription = "Consulta básica executada com sucesso.";

        try {
            var observabilityStatus = observabilityStatusUseCase.getStatus();
            if (!"UP".equalsIgnoreCase(observabilityStatus.status())) {
                databaseState = "ERROR";
                databaseDescription = "Health check retornou status " + observabilityStatus.status() + ".";
            }
        } catch (Exception exception) {
            databaseState = "ERROR";
            databaseDescription = "Falha ao consultar o health check da aplicação.";
        }

        signals.add(new PlatformHealthSignalResponse(
                "database",
                "Banco de dados",
                databaseState,
                databaseDescription
        ));

        long activeCompanies = companyRepository.findByActiveTrue().size();
        long inactiveCompanies = companyRepository.findByActiveFalse().size();
        signals.add(new PlatformHealthSignalResponse(
                "companies",
                "Empresas",
                "OPERATIONAL",
                activeCompanies + " empresa(s) ativa(s) e " + inactiveCompanies + " inativa(s)."
        ));

        long pendingLgpdRequests = lgpdRequestRepository.countByStatusIn(PENDING_LGPD_STATUSES);
        signals.add(new PlatformHealthSignalResponse(
                "pending-lgpd",
                "LGPD",
                pendingLgpdRequests > 0 ? "ATTENTION_REQUIRED" : "OPERATIONAL",
                pendingLgpdRequests > 0
                        ? pendingLgpdRequests + " solicitação(ões) pendente(s)."
                        : "Sem solicitações LGPD pendentes."
        ));

        String state = resolveOverallState(signals);

        return new PlatformHealthResponse(
                state,
                checkedAt,
                List.copyOf(signals),
                new PlatformHealthMetricsResponse(
                        activeCompanies,
                        inactiveCompanies,
                        pendingLgpdRequests,
                        null
                )
        );
    }

    private String resolveOverallState(List<PlatformHealthSignalResponse> signals) {
        if (signals.stream().anyMatch(signal -> "ERROR".equals(signal.state()))) {
            return "ERROR";
        }

        if (signals.stream().anyMatch(signal -> "ATTENTION_REQUIRED".equals(signal.state()))) {
            return "ATTENTION_REQUIRED";
        }

        return "OPERATIONAL";
    }
}
