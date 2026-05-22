package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.lgpd.CreateLgpdRequestRequest;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdEmployeeExportResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdRequestHistoryResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdRequestResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.UpdateLgpdRequestStatusRequest;
import com.kts.kronos.application.port.in.usecase.LgpdUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.LGPD;
import static com.kts.kronos.constants.ApiPaths.LGPD_EMPLOYEE_ANONYMIZE;
import static com.kts.kronos.constants.ApiPaths.LGPD_EMPLOYEE_EXPORT;
import static com.kts.kronos.constants.ApiPaths.LGPD_REQUESTS;
import static com.kts.kronos.constants.ApiPaths.LGPD_REQUEST_HISTORY;
import static com.kts.kronos.constants.ApiPaths.LGPD_REQUEST_ID;
import static com.kts.kronos.constants.ApiPaths.LGPD_REQUEST_STATUS;
import static com.kts.kronos.constants.Messages.ADMINISTRATOR;
import static com.kts.kronos.constants.Messages.ANY_EMPLOYEE;

@RestController
@RequestMapping(LGPD)
@RequiredArgsConstructor
public class LgpdController {
    private final LgpdUseCase lgpdUseCase;
    private final ClientIpResolver clientIpResolver;

    @PreAuthorize(ANY_EMPLOYEE)
    @PostMapping(LGPD_REQUESTS)
    public ResponseEntity<LgpdRequestResponse> createRequest(
            @Valid @RequestBody CreateLgpdRequestRequest request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest httpServletRequest
    ) {
        var created = lgpdUseCase.createRequest(
                request,
                clientIpResolver.resolve(httpServletRequest),
                userAgent
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(LgpdRequestResponse.fromDomain(created));
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping(LGPD_REQUESTS)
    public ResponseEntity<List<LgpdRequestResponse>> listRequests(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) LgpdRequestType type,
            @RequestParam(required = false) LgpdRequestStatus status
    ) {
        var requests = lgpdUseCase.listRequests(employeeId, type, status)
                .stream()
                .map(LgpdRequestResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(requests);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping(LGPD_REQUEST_ID)
    public ResponseEntity<LgpdRequestResponse> getRequest(@PathVariable UUID requestId) {
        return ResponseEntity.ok(LgpdRequestResponse.fromDomain(lgpdUseCase.getRequest(requestId)));
    }

    @PreAuthorize(ADMINISTRATOR)
    @PatchMapping(LGPD_REQUEST_STATUS)
    public ResponseEntity<LgpdRequestResponse> updateRequestStatus(
            @PathVariable UUID requestId,
            @Valid @RequestBody UpdateLgpdRequestStatusRequest request
    ) {
        return ResponseEntity.ok(LgpdRequestResponse.fromDomain(lgpdUseCase.updateRequestStatus(requestId, request)));
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping(LGPD_REQUEST_HISTORY)
    public ResponseEntity<List<LgpdRequestHistoryResponse>> getRequestHistory(@PathVariable UUID requestId) {
        var history = lgpdUseCase.getRequestHistory(requestId)
                .stream()
                .map(LgpdRequestHistoryResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(history);
    }

    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping(LGPD_EMPLOYEE_EXPORT)
    public ResponseEntity<LgpdEmployeeExportResponse> exportEmployeeData(
            @PathVariable UUID employeeId,
            @RequestParam(defaultValue = "false") boolean includePreciseGeolocation,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(lgpdUseCase.exportEmployeeData(
                employeeId,
                includePreciseGeolocation,
                clientIpResolver.resolve(httpServletRequest),
                userAgent
        ));
    }

    @PreAuthorize(ADMINISTRATOR)
    @PostMapping(LGPD_EMPLOYEE_ANONYMIZE)
    public ResponseEntity<Void> anonymizeEmployee(
            @PathVariable UUID employeeId,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletRequest httpServletRequest
    ) {
        lgpdUseCase.anonymizeEmployee(
                employeeId,
                clientIpResolver.resolve(httpServletRequest),
                userAgent
        );
        return ResponseEntity.noContent().build();
    }
}
