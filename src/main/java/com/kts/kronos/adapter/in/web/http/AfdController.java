package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.service.AfdService;
import com.kts.kronos.application.service.EmployeeService;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/afd")
@RequiredArgsConstructor
@Tag(name = "Fiscal", description = "Endpoints para arquivos fiscais (Portaria 671)")
public class AfdController {

    private final AfdService afdService;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final EmployeeService employeeService;

    @GetMapping("/download")
    @PreAuthorize("hasAnyRole('MANAGER', 'CTO', 'ADMIN')")
    @Operation(summary = "Download do Arquivo Fonte de Dados (AFD)")
    public void downloadAfd(HttpServletResponse response) throws IOException {

        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        // Busca funcionário para pegar a empresa
        // (Nota: você precisará expor um método 'findById' no EmployeeService ou Provider se ainda não tiver público)
        // Assumindo que você tem acesso aos dados do funcionário logado:
        // var employee = employeeService.findById(employeeId);
        // UUID companyId = employee.companyId();

        // Mock para exemplo, substitua pela busca real da empresa do usuário logado
        UUID companyId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"AFD.txt\"");

        // Escreve diretamente no stream da resposta HTTP
        afdService.writeAfdToStream(companyId, response.getOutputStream());
    }
}