package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.AejUseCase;
import com.kts.kronos.application.port.in.usecase.PointMirrorPdfUseCase;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.service.TechnicalCertificatePdfService;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.infrastructure.DigitalSignatureService;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import com.kts.kronos.observability.support.ObservabilityDefaults;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/legal")
@RequiredArgsConstructor
@Tag(name = "Fiscal - Arquivos Legais", description = "Geração de arquivos para fiscalização e espelhos de ponto (Portaria 671)")
public class LegalController {

    private final AdfUseCase afdUseCase;
    private final AejUseCase aejUseCase;
    private final PointMirrorPdfUseCase pointMirrorPdfUseCase; // Serviço do Espelho
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final EmployeeProvider employeeProvider;
    private final CompanyProvider companyProvider;
    private final DomainAuthorizationService domainAuthorizationService;
    private final TechnicalCertificatePdfService certificateService;
    private final DigitalSignatureService signatureService;
    private final KronosMetrics kronosMetrics;
    private final KronosTracing kronosTracing;

    public LegalController(
            AdfUseCase afdUseCase,
            AejUseCase aejUseCase,
            PointMirrorPdfUseCase pointMirrorPdfUseCase,
            JwtAuthenticatedUser jwtAuthenticatedUser,
            EmployeeProvider employeeProvider,
            CompanyProvider companyProvider,
            DomainAuthorizationService domainAuthorizationService,
            TechnicalCertificatePdfService certificateService,
            DigitalSignatureService signatureService
    ) {
        this(
                afdUseCase,
                aejUseCase,
                pointMirrorPdfUseCase,
                jwtAuthenticatedUser,
                employeeProvider,
                companyProvider,
                domainAuthorizationService,
                certificateService,
                signatureService,
                ObservabilityDefaults.metrics(),
                ObservabilityDefaults.tracing()
        );
    }

    @GetMapping("/technical-certificate")
    @PreAuthorize("hasAnyRole('MANAGER', 'CTO')")
    @Operation(summary = "Baixar Atestado Técnico (Portaria 671)", description = "Gera o atestado de conformidade técnica assinado digitalmente pelo desenvolvedor.")
    public void downloadTechnicalCertificate(HttpServletResponse response) throws IOException {
        long startedAt = System.nanoTime();

        try {
            UUID companyId = getCompanyIdFromLoggedUser();
            var company = companyProvider.findById(companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));

            byte[] signedBytes = kronosTracing.observe("kronos.legal.technical_certificate", () -> {
                byte[] pdfBytes = certificateService.generateCertificate(company);
                return signatureService.signData(pdfBytes);
            });

            String filename = "Atestado_Tecnico_Kronos_" + LocalDate.now().getYear() + ".p7s";
            response.setContentType("application/pkcs7-signature");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            response.getOutputStream().write(signedBytes);
            response.flushBuffer();

            kronosMetrics.legalSuccess("technical_certificate");
            kronosMetrics.recordLegalDuration("technical_certificate", Duration.ofNanos(System.nanoTime() - startedAt), "success");
            log.info("event=legal_technical_certificate_generation result=success");
        } catch (RuntimeException e) {
            String reason = e.getMessage() != null && e.getMessage().contains("assinar") ? "digital_signature" : "generation";
            kronosMetrics.legalFailure("technical_certificate", reason);
            kronosMetrics.recordLegalDuration("technical_certificate", Duration.ofNanos(System.nanoTime() - startedAt), "failure");
            log.error("event=legal_technical_certificate_generation result=failure reason={} exception_type={}",
                    reason,
                    e.getClass().getSimpleName());
            throw e;
        }
    }

    @GetMapping("/afd")
    @PreAuthorize("hasAnyRole('MANAGER', 'CTO')")
    @Operation(summary = "Baixar Arquivo Fonte de Dados (AFD)", description = "Arquivo TXT contendo todos os registros brutos de ponto.")
    public void downloadAfd(HttpServletResponse response) throws IOException {
        UUID companyId = getCompanyIdFromLoggedUser();

        String filename = String.format("AFD_%s.txt", companyId);
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        afdUseCase.writeAfdToStream(companyId, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/aej")
    @PreAuthorize("hasAnyRole('MANAGER', 'CTO')")
    @Operation(summary = "Baixar Arquivo Eletrônico de Jornada (AEJ)", description = "Arquivo ASSINADO DIGITALMENTE (.p7s) contendo apuração de ponto.")
    public void downloadAej(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response
    ) throws IOException {

        UUID companyId = getCompanyIdFromLoggedUser();

        String filename = String.format("AEJ_%s_%s.p7s", startDate, endDate);
        response.setContentType("application/pkcs7-signature");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        aejUseCase.generateAej(companyId, startDate, endDate, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping("/espelho-ponto")
    @PreAuthorize("hasAnyRole('MANAGER', 'CTO', 'PARTNER')")
    @Operation(summary = "Baixar Espelho de Ponto (PDF)", description = "Relatório mensal detalhado para conferência do funcionário.")
    public void downloadMirror(
            @RequestParam(required = false) UUID targetEmployeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response
    ) throws IOException {

        UUID employeeIdToGenerate = domainAuthorizationService
                .authorizeEmployeeAccess(targetEmployeeId)
                .employeeId();

        byte[] pdfBytes = pointMirrorPdfUseCase.generateMirror(employeeIdToGenerate, startDate, endDate);

        String filename = String.format("Espelho_%s_%s.pdf", startDate, endDate);
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        response.getOutputStream().write(pdfBytes);
        response.flushBuffer();
    }

    private UUID getCompanyIdFromLoggedUser() {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        Employee employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado."));
        return employee.companyId();
    }
}
