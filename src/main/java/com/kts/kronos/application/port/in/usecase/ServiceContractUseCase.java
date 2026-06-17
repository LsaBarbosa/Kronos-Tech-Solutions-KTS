package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.servicecontract.CreateServiceContractResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.PendingServiceContractListResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.ServiceContractAdminItemResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.ServiceContractAdminPageResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.ServiceContractSignatureAdminPageResponse;
import com.kts.kronos.adapter.in.web.dto.servicecontract.SignServiceContractRequest;
import com.kts.kronos.adapter.in.web.dto.servicecontract.SignServiceContractResponse;
import com.kts.kronos.domain.model.enuns.ContractSignatureStatus;
import com.kts.kronos.domain.model.enuns.ServiceContractStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ServiceContractUseCase {

    CreateServiceContractResponse create(
            String title,
            String description,
            List<UUID> employeeIds,
            MultipartFile pdfFile,
            String ipAddress,
            String userAgent
    );

    ServiceContractAdminPageResponse findAdmin(ServiceContractStatus status, int page, int size);

    ServiceContractAdminItemResponse findAdminDetail(UUID contractId);

    PendingServiceContractListResponse findPendingForCurrentEmployee();

    byte[] preview(UUID contractId);

    SignServiceContractResponse sign(UUID contractId, SignServiceContractRequest request, String ipAddress, String userAgent);

    SignedDocumentDownload downloadSignatureDocument(UUID signatureId);

    ServiceContractSignatureAdminPageResponse findAdminSignatures(
            UUID contractId,
            ContractSignatureStatus status,
            int page,
            int size
    );

    record SignedDocumentDownload(byte[] data, String fileName, String contentType) {}
}
