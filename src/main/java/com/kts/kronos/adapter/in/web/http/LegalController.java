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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

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

    @GetMapping("/technical-certificate")
    @PreAuthorize("hasAnyRole('MANAGER', 'CTO')")
    @Operation(summary = "Baixar Atestado Técnico (Portaria 671)", description = "Gera o atestado de conformidade técnica assinado digitalmente pelo desenvolvedor.")
    public void downloadTechnicalCertificate(HttpServletResponse response) throws IOException {

        // 1. Identifica a empresa
        UUID companyId = getCompanyIdFromLoggedUser();
        var company = companyProvider.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));

        // 2. Gera PDF e Assina
        byte[] pdfBytes = certificateService.generateCertificate(company);
        byte[] signedBytes = signatureService.signData(pdfBytes);

        // 3. Download .p7s
        String filename = "Atestado_Tecnico_Kronos_" + LocalDate.now().getYear() + ".p7s";
        response.setContentType("application/pkcs7-signature");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        response.getOutputStream().write(signedBytes);
        response.flushBuffer();
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
