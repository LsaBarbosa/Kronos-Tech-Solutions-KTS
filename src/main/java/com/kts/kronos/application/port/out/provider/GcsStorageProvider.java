package com.kts.kronos.application.port.out.provider;

public interface GcsStorageProvider {
    String uploadFile(String objectName, byte[] fileData, String contentType);

    byte[] downloadFile(String objectName);

    void deleteFile(String objectName);
}
