package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.servicecontract.CreateServiceContractResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.PendingServiceContractListResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.ServiceContractAdminItemResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.ServiceContractAdminPageResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.ServiceContractSignatureAdminPageResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.SignServiceContractRequest;
import com.kts.kronos.adapter.in.web.dto.servicecontract.SignServiceContractResponse;
import com.kts.kronos.application.port.in.usecase.ServiceContractUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.domain.model.enuns.ContractSignatureStatus;
import com.kts.kronos.domain.model.enuns.ServiceContractStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.SERVICE_CONTRACTS;
import static com.kts.kronos.constants.ApiPaths.SERVICE_CONTRACT_ADMIN;
import static com.kts.kronos.constants.ApiPaths.SERVICE_CONTRACT_ADMIN_DETAIL;
import static com.kts.kronos.constants.ApiPaths.SERVICE_CONTRACT_ADMIN_SIGNATURES;
import static com.kts.kronos.constants.ApiPaths.SERVICE_CONTRACT_ME_PENDING;
import static com.kts.kronos.constants.ApiPaths.SERVICE_CONTRACT_PREVIEW;
import static com.kts.kronos.constants.ApiPaths.SERVICE_CONTRACT_SIGN;
import static com.kts.kronos.constants.ApiPaths.SERVICE_CONTRACT_SIGNATURE_DOCUMENT;

@RestController
@RequestMapping(SERVICE_CONTRACTS)
@RequiredArgsConstructor
public class ServiceContractController {

    private final ServiceContractUseCase useCase;
    private final ClientIpResolver clientIpResolver;

    @PreAuthorize("hasAnyRole('MANAGER', 'CTO')")
    @PostMapping(value = SERVICE_CONTRACT_ADMIN, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreateServiceContractResponse> create(
            @RequestPart("title") String title,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart("employeeIds") String employeeIdsCsv,
            @RequestPart("file") MultipartFile file,
            HttpServletRequest httpServletRequest
    ) {
        List<UUID> employeeIds = Arrays.stream(employeeIdsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(UUID::fromString)
                .toList();
        String ip = clientIpResolver.resolve(httpServletRequest);
        String ua = httpServletRequest.getHeader(HttpHeaders.USER_AGENT);
        return ResponseEntity.ok(useCase.create(title, description, employeeIds, file, ip, ua));
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'CTO')")
    @GetMapping(SERVICE_CONTRACT_ADMIN)
    public ResponseEntity<ServiceContractAdminPageResponse> findAdmin(
            @RequestParam(required = false) ServiceContractStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(useCase.findAdmin(status, page, size));
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'CTO')")
    @GetMapping(SERVICE_CONTRACT_ADMIN_DETAIL)
    public ResponseEntity<ServiceContractAdminItemResponse> findAdminDetail(@PathVariable UUID contractId) {
        return ResponseEntity.ok(useCase.findAdminDetail(contractId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(SERVICE_CONTRACT_ME_PENDING)
    public ResponseEntity<PendingServiceContractListResponse> findMyPending() {
        return ResponseEntity.ok(useCase.findPendingForCurrentEmployee());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(SERVICE_CONTRACT_PREVIEW)
    public ResponseEntity<byte[]> preview(@PathVariable UUID contractId, HttpServletRequest httpServletRequest) {
        String ip = clientIpResolver.resolve(httpServletRequest);
        String ua = httpServletRequest.getHeader(HttpHeaders.USER_AGENT);
        byte[] pdf = useCase.preview(contractId, ip, ua);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"contrato_" + contractId + ".pdf\"")
                .body(pdf);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping(SERVICE_CONTRACT_SIGN)
    public ResponseEntity<SignServiceContractResponse> sign(
            @PathVariable UUID contractId,
            @Valid @RequestBody SignServiceContractRequest request,
            HttpServletRequest httpServletRequest
    ) {
        String ip = clientIpResolver.resolve(httpServletRequest);
        String ua = httpServletRequest.getHeader(HttpHeaders.USER_AGENT);
        return ResponseEntity.ok(useCase.sign(contractId, request, ip, ua));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping(SERVICE_CONTRACT_SIGNATURE_DOCUMENT)
    public ResponseEntity<byte[]> downloadSignedDocument(@PathVariable UUID signatureId, HttpServletRequest httpServletRequest) {
        String ip = clientIpResolver.resolve(httpServletRequest);
        String ua = httpServletRequest.getHeader(HttpHeaders.USER_AGENT);
        ServiceContractUseCase.SignedDocumentDownload d = useCase.downloadSignatureDocument(signatureId, ip, ua);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + d.fileName() + "\"")
                .body(d.data());
    }

    @PreAuthorize("hasAnyRole('MANAGER', 'CTO')")
    @GetMapping(SERVICE_CONTRACT_ADMIN_SIGNATURES)
    public ResponseEntity<ServiceContractSignatureAdminPageResponse> findAdminSignatures(
            @RequestParam(required = false) UUID contractId,
            @RequestParam(required = false) ContractSignatureStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(useCase.findAdminSignatures(contractId, status, page, size));
    }
}
