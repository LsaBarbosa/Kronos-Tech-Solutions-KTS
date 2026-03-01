package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.AejUseCase;
import com.kts.kronos.application.port.in.usecase.PointMirrorPdfUseCase;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.service.TechnicalCertificatePdfService;
import com.kts.kronos.infrastructure.DigitalSignatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

import static com.kts.kronos.constants.ApiPaths.AEJ;
import static com.kts.kronos.constants.ApiPaths.AFD;
import static com.kts.kronos.constants.ApiPaths.LEGAL;
import static com.kts.kronos.constants.ApiPaths.MIRROR_POINT;
import static com.kts.kronos.constants.ApiPaths.TECHNICAL_CERTIFICATE;
import static com.kts.kronos.constants.ExceptionMessages.COMPANY_NOT_FOUND;
import static com.kts.kronos.constants.ExceptionMessages.EMPLOYEE_NOT_FOUND;
import static com.kts.kronos.constants.Messages.*;
import static com.kts.kronos.constants.PathValues.FILE_NAME_AEJ_PATTERN;
import static com.kts.kronos.constants.PathValues.FILE_NAME_AFD_PATTERN;
import static com.kts.kronos.constants.PathValues.FILE_NAME_MIRROR_PATTERN;
import static com.kts.kronos.constants.PathValues.FILE_NAME_TECHNICAL_CERTIFICATE_PATTERN;
import static com.kts.kronos.constants.PathValues.MEDIA_TYPE_PKCS7;
import static com.kts.kronos.constants.PathValues.ROLE_MANAGER;
import static com.kts.kronos.constants.Swagger.*;


@RestController
@RequestMapping(LEGAL)
@RequiredArgsConstructor
@Tag(name = SWAGGER_LEGAL_TAG, description = SWAGGER_LEGAL_DESC)public class LegalController {

    private final AdfUseCase afdUseCase;
    private final AejUseCase aejUseCase;
    private final PointMirrorPdfUseCase pointMirrorPdfUseCase;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final EmployeeProvider employeeProvider;
    private final CompanyProvider companyProvider;
    private final TechnicalCertificatePdfService certificateService;
    private final DigitalSignatureService signatureService;

    @GetMapping(TECHNICAL_CERTIFICATE)
    @PreAuthorize(ADMINISTRATOR)
    @Operation(summary = TECH_CERT_SUMMARY, description = TECH_CERT_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = FILE_GENERATED_SUCCESS,
                    content = @Content(mediaType = "application/pkcs7-signature")),
            @ApiResponse(responseCode = "404", description = COMPANY_OR_EMPLOYEE_NOT_FOUND),
            @ApiResponse(responseCode = "500", description = INTERNAL_SIGNATURE_ERROR),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED_MANAGER_CTO)
    })
    public void downloadTechnicalCertificate(HttpServletResponse response) throws IOException {

        var companyId = getCompanyIdFromLoggedUser();
        var company = companyProvider.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

        // Gera PDF e Assina
        byte[] pdfBytes = certificateService.generateCertificate(company);
        byte[] signedBytes = signatureService.signData(pdfBytes);

        //Download .p7s
        var filename = String.format(FILE_NAME_TECHNICAL_CERTIFICATE_PATTERN, LocalDate.now().getYear());
        response.setContentType(MEDIA_TYPE_PKCS7);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        response.getOutputStream().write(signedBytes);
        response.flushBuffer();
    }

    @GetMapping(AFD)
    @PreAuthorize(ADMINISTRATOR)
    @Operation(summary = AFD_SUMMARY, description = AFD_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = AFD_SUCCESS,
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "404", description = COMPANY_NOT_FOUND),
            @ApiResponse(responseCode = "500", description = STREAM_ERROR),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
    public void downloadAfd(HttpServletResponse response) throws IOException {
        var companyId = getCompanyIdFromLoggedUser();

        var filename = String.format(FILE_NAME_AFD_PATTERN, companyId);
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        afdUseCase.writeAfdToStream(companyId, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping(AEJ)
    @PreAuthorize(ADMINISTRATOR)
    @Operation(summary = AEJ_SUMMARY, description = AEJ_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = AEJ_SUCCESS,
                    content = @Content(mediaType = "application/pkcs7-signature")),
            @ApiResponse(responseCode = "404", description = COMPANY_NOT_FOUND),
            @ApiResponse(responseCode = "500", description = CRITICAL_SIGNATURE_ERROR),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
    public void downloadAej(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response
    ) throws IOException {

        var companyId = getCompanyIdFromLoggedUser();

        var filename = String.format(FILE_NAME_AEJ_PATTERN, startDate, endDate);
        response.setContentType(MEDIA_TYPE_PKCS7);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        aejUseCase.generateAej(companyId, startDate, endDate, response.getOutputStream());
        response.flushBuffer();
    }

    @GetMapping(MIRROR_POINT)
    @PreAuthorize(ANY_EMPLOYEE)
    @Operation(summary = MIRROR_SUMMARY, description = MIRROR_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = PDF_SUCCESS,
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE)),
            @ApiResponse(responseCode = "404", description = COMPANY_OR_EMPLOYEE_NOT_FOUND),
            @ApiResponse(responseCode = "500", description = PDF_GENERATION_ERROR),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
    public void downloadMirror(
            @RequestParam(required = false) UUID targetEmployeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletResponse response
    ) throws IOException {

        var loggedId = jwtAuthenticatedUser.getEmployeeId();
        UUID employeeIdToGenerate;

        if (targetEmployeeId != null && jwtAuthenticatedUser.getRoleFromToken().equals(ROLE_MANAGER)) {
            employeeIdToGenerate = targetEmployeeId;
        } else {
            employeeIdToGenerate = loggedId;
        }

        byte[] pdfBytes = pointMirrorPdfUseCase.generateMirror(employeeIdToGenerate, startDate, endDate);

        var filename = String.format(FILE_NAME_MIRROR_PATTERN, startDate, endDate);
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        response.getOutputStream().write(pdfBytes);
        response.flushBuffer();
    }

    private UUID getCompanyIdFromLoggedUser() {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        return employee.companyId();
    }
}