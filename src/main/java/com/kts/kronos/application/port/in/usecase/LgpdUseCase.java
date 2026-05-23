package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDryRunResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.CreateLgpdRequestRequest;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdEmployeeExportResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdRequestAdminListResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdRequestDetailsResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.UpdateLgpdRequestStatusRequest;
import com.kts.kronos.domain.model.AnonymizationConsolidatedResult;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.LgpdRequestHistory;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface LgpdUseCase {
    LgpdRequest createRequest(CreateLgpdRequestRequest request, String ipAddress, String userAgent);

    List<LgpdRequest> listRequests(UUID employeeId, LgpdRequestType type, LgpdRequestStatus status);

    LgpdRequest getRequest(UUID requestId);

    LgpdRequest updateRequestStatus(UUID requestId, UpdateLgpdRequestStatusRequest request);

    List<LgpdRequestHistory> getRequestHistory(UUID requestId);

    LgpdEmployeeExportResponse exportEmployeeData(
            UUID employeeId,
            boolean includePreciseGeolocation,
            String ipAddress,
            String userAgent,
            String exportReason
    );

    void anonymizeEmployee(UUID employeeId, String ipAddress, String userAgent);

    AnonymizationDryRunResponse dryRunAnonymizeEmployee(UUID employeeId);

    Page<LgpdRequestAdminListResponse> listAdminRequests(
            LgpdRequestType type,
            LgpdRequestStatus status,
            UUID companyId,
            Pageable pageable
    );

    LgpdRequestDetailsResponse getRequestDetails(UUID requestId);

    LgpdRequest assignRequest(UUID requestId, UUID assignedToUserId);

    LgpdRequest addNote(UUID requestId, String publicNote, String internalNote);

    LgpdRequest completeRequest(UUID requestId, String publicResolutionNotes, String internalNotes);

    LgpdRequest rejectRequest(UUID requestId, String closedReason, String publicNote, String internalNote);

    LgpdRequest transitionStatus(UUID requestId, LgpdRequestStatus newStatus, String publicNotes, String internalNotes, String closedReason);

    LgpdRequest requestDataSubjectComplement(UUID requestId, String complementMessage);

    LgpdRequest cancelRequest(UUID requestId, String cancellationReason);

    List<LgpdRequestStatus> getAvailableTransitions(LgpdRequestStatus currentStatus);

    AnonymizationConsolidatedResult getAnonymizationResult(UUID requestId);
}
