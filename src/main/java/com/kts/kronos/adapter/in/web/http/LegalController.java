package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.AejUseCase;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.service.TechnicalCertificatePdfService;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.infrastructure.DigitalSignatureService;
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
@RequestMapping("/api/v1/legal")
@RequiredArgsConstructor
@Tag(name = "Fiscal - Arquivos Legais", description = "Geração de arquivos para fiscalização (Portaria 671)")
public class LegalController {

    private final AdfUseCase afdUseCase; // Use a interface (UseCase), não a implementação direta
    private final AejUseCase aejUseCase;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final EmployeeProvider employeeProvider;
    private final CompanyProvider companyProvider;
    private final TechnicalCertificatePdfService certificateService;
    private final DigitalSignatureService signatureService;

    @GetMapping("/technical-certificate")
    @PreAuthorize("hasAnyRole('MANAGER', 'CTO')")
    @Operation(summary = "Baixar Atestado Técnico (Portaria 671)", description = "Gera o atestado de conformidade técnica assinado digitalmente pelo desenvolvedor.")
    public void downloadTechnicalCertificate(HttpServletResponse response) throws IOException {

        // 1. Identifica a empresa do cliente (Padaria)
        UUID companyId = getCompanyIdFromLoggedUser();
        // Precisamos do objeto Company completo para pegar o Nome e CNPJ
        // Assumindo que você tem um método findById no companyUseCase ou provider
        var company = companyProvider.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));

        // 2. Gera o PDF em memória (Visual)
        byte[] pdfBytes = certificateService.generateCertificate(company);

        // 3. Assina o PDF digitalmente (Obrigatório por lei ser assinado pelo Fabricante/Kronos)
        // Isso vai gerar um arquivo .p7s (PDF envelopado na assinatura) ou você pode optar por assinar o PDF direto (PAdES) se tiver biblioteca específica.
        // Vamos usar o padrão .p7s que já implementamos, que é universalmente aceito na ICP-Brasil.
        byte[] signedBytes = signatureService.signData(pdfBytes);

        // 4. Configura o Download
        String filename = "Atestado_Tecnico_Kronos_" + LocalDate.now().getYear() + ".p7s";

        response.setContentType("application/pkcs7-signature");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        // 5. Envia
        response.getOutputStream().write(signedBytes);
        response.flushBuffer();
    }

    @GetMapping("/afd")
    @PreAuthorize("hasAnyRole('MANAGER', 'CTO')")
    @Operation(summary = "Baixar Arquivo Fonte de Dados (AFD)", description = "Arquivo TXT contendo todos os registros brutos de ponto.")
    public void downloadAfd(HttpServletResponse response) throws IOException {
        
        // 1. Busca a empresa do usuário logado (CORREÇÃO DO MOCK)
        UUID companyId = getCompanyIdFromLoggedUser();

        // 2. Configura resposta para TXT (AFD é texto puro)
        String filename = String.format("AFD_%s.txt", companyId);
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        // 3. Escreve no stream
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

        // 1. Configura resposta para P7S (CORREÇÃO DO CONTENT-TYPE)
        // O AejService agora retorna um binário assinado, não texto.
        String filename = String.format("AEJ_%s_%s.p7s", startDate, endDate);
        
        response.setContentType("application/pkcs7-signature"); // MIME Type correto para arquivo assinado
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        // 2. Gera e assina no stream
        aejUseCase.generateAej(companyId, startDate, endDate, response.getOutputStream());
        response.flushBuffer();
    }

    // Método auxiliar para evitar duplicação de código
    private UUID getCompanyIdFromLoggedUser() {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        Employee employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado."));
        return employee.companyId();
    }
}