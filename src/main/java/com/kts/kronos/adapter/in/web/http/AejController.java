package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.service.AejService;
import com.kts.kronos.application.service.CompanyService;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
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
@RequestMapping("/api/v1/fiscal/aej")
@RequiredArgsConstructor
@Tag(name = "Fiscal - AEJ", description = "Geração do Arquivo Eletrônico de Jornada (Anexo VI)")
public class AejController {

    private final AejService aejService;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final EmployeeProvider employeeProvider;

    @GetMapping("/download")
    @PreAuthorize("hasAnyRole('MANAGER', 'CTO')")
    @Operation(summary = "Baixar Arquivo AEJ (Período)")
    public void downloadAej(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response
    ) throws IOException {

        // Identifica empresa do usuário logado
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        Employee employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado."));
        
        UUID companyId = employee.companyId();

        // Configura resposta
        String filename = String.format("AEJ_%s_%s.txt", startDate, endDate);
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        // Gera o arquivo direto no stream
        aejService.generateAej(companyId, startDate, endDate, response.getOutputStream());
    }
}