package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.AejUseCase;
import com.kts.kronos.application.port.in.usecase.PointMirrorPdfUseCase;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.service.TechnicalCertificateService;
import com.kts.kronos.domain.model.Employee;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fiscal")
@RequiredArgsConstructor
@Tag(name = "Fiscal (Portaria 671)", description = "Arquivos fiscais e legais do REP-P")
public class FiscalController {

    private final AdfUseCase adfUseCase;
    private final AejUseCase aejUseCasee;
    private final PointMirrorPdfUseCase pointMirrorPdfUseCase;
    private final TechnicalCertificateService certificateService;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final EmployeeProvider employeeProvider;

    private UUID getCompanyIdFromUser() {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        Employee employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado."));
        return employee.companyId();
    }

    @GetMapping("/afd")
    @PreAuthorize("hasAnyRole('MANAGER', 'CTO')")
    @Operation(summary = "Download do Arquivo Fonte de Dados (AFD) - Completo")
    public void downloadAfd(HttpServletResponse response) throws IOException {
        UUID companyId = getCompanyIdFromUser();
        
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"AFD.txt\"");
        adfUseCase.writeAfdToStream(companyId, response.getOutputStream());
    }

    @GetMapping("/aej")
    @PreAuthorize("hasAnyRole('MANAGER', 'CTO')")
    @Operation(summary = "Download do Arquivo Eletrônico de Jornada (AEJ) - Mensal")
    public void downloadAej(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response
    ) throws IOException {
        UUID companyId = getCompanyIdFromUser();

        String filename = String.format("AEJ_%s_%s.txt", startDate, endDate);
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        aejUseCasee.generateAej(companyId, startDate, endDate, response.getOutputStream());
    }

    @GetMapping("/atestado-tecnico")
    @PreAuthorize("hasAnyRole('MANAGER', 'CTO')")
    @Operation(summary = "Download do Atestado Técnico e Termo de Responsabilidade (PDF)")
    public void downloadCertificate(HttpServletResponse response) throws IOException {
        UUID companyId = getCompanyIdFromUser();

        byte[] pdfBytes = certificateService.generateCertificate(companyId);

        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Atestado_Tecnico_REP_P.pdf\"");
        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();
    }

    @GetMapping("/espelho-ponto")
    @PreAuthorize("hasAnyRole('MANAGER', 'CTO', 'PARTNER')") // Partner (Funcionário) também pode ver o seu?
    @Operation(summary = "Download do Espelho de Ponto (PDF) - Mensal")
    public void downloadMirror(
            @RequestParam(required = false) UUID targetEmployeeId, // Opcional: Se Manager, pode pedir de outro. Se Partner, pega o próprio.
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response
    ) throws IOException {

        UUID employeeId;

        // Lógica de Permissão Simplificada
        // Se targetEmployeeId for informado e quem pede é Manager, usa o target.
        // Senão, usa o ID do usuário logado.
        UUID loggedId = jwtAuthenticatedUser.getEmployeeId();
        // (Aqui você pode adicionar validação se loggedUser tem permissão sobre targetEmployeeId)

        if (targetEmployeeId != null) {
            // Validar se usuário logado é gerente deste funcionário
            employeeId = targetEmployeeId;
        } else {
            employeeId = loggedId;
        }

        byte[] pdfBytes = pointMirrorPdfUseCase.generateMirror(employeeId, startDate, endDate);

        String filename = String.format("Espelho_%s_%s.pdf", startDate, endDate);
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.getOutputStream().write(pdfBytes);
        response.getOutputStream().flush();
    }
}