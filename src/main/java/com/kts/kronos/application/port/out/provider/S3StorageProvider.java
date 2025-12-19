package com.kts.kronos.application.port.out.provider;

public interface S3StorageProvider {
    String uploadFile(String fileName, byte[] content);
    byte[] downloadFile(String fileKey);
    // void deleteFile(String fileKey); // Opcional
}