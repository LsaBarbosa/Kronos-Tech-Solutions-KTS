package com.kts.kronos.application.port.out.provider;

import java.io.InputStream;
import java.util.UUID;

public interface FaceStorageProvider {
    String uploadFaceImage(UUID employeeId, InputStream imageStream, String contentType);
    void deleteFaceImage(String objectKey);
}