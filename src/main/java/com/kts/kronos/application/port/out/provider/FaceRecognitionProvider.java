package com.kts.kronos.application.port.out.provider;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface FaceRecognitionProvider {
    void ensureCollectionExists();
    String indexFace(String imageS3Key, UUID externalImageId);
    UUID searchFaceByImage(InputStream imageStream);
    List<FaceMatchCandidate> searchFacesByImage(InputStream imageStream);
    void deleteFace(String faceId);
    void deleteFacesByExternalImageId(UUID externalImageId);

    record FaceMatchCandidate(UUID employeeId, float similarity) {}
}
