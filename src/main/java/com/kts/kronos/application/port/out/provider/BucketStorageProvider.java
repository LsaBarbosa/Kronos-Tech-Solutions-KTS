package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.enuns.DocumentType;

public interface BucketStorageProvider {
    String uploadFile(
            DocumentType documentType,
            String objectName,
            byte[] fileData,
            String contentType
    );

    byte[] downloadFile(
            DocumentType documentType,
            String objectName
    );

    void deleteFile(
            DocumentType documentType,
            String objectName
    );
}
