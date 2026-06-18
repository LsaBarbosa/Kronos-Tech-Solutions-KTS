package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RekognitionProviderImpl  implements FaceRecognitionProvider {

    private final RekognitionClient rekognitionClient;
    private final FaceStorageProvider faceStorageProvider;
    private final PrivacyLogReferenceService privacyLogReferenceService;
    private final KronosMetrics kronosMetrics;
    private final KronosTracing kronosTracing;

    @Value("${aws.rekognition.collection-id}")
    private String collectionId;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private static final float FACE_MATCH_THRESHOLD = 90.0f; // Limite de 90% para o Free Tier

    @Override
    public void ensureCollectionExists() {
        long startedAt = System.nanoTime();
        try {
            kronosTracing.observe("kronos.external.rekognition", () -> rekognitionClient.createCollection(
                    CreateCollectionRequest.builder()
                            .collectionId(collectionId)
                            .build()
            ), "provider", "rekognition", "operation", "create_collection");
            kronosMetrics.recordExternalProviderRequest("rekognition", "create_collection", "success", "none");
            kronosMetrics.recordExternalProviderRequestDuration("rekognition", "create_collection",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "success");
        } catch (ResourceAlreadyExistsException e) {
            kronosMetrics.recordExternalProviderRequest("rekognition", "create_collection", "success", "already_exists");
            kronosMetrics.recordExternalProviderRequestDuration("rekognition", "create_collection",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "success");
        } catch (SdkException e) {
            kronosMetrics.recordExternalProviderRequest("rekognition", "create_collection", "failure", "sdk_exception");
            kronosMetrics.recordExternalProviderRequestDuration("rekognition", "create_collection",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
            log.error("event=rekognition_collection_init_error collectionRef={} exceptionType={}",
                    privacyLogReferenceService.storageRef(collectionId), e.getClass().getSimpleName(), e);
            throw new RuntimeException("Falha na inicialização do serviço Rekognition.", e);
        }
    }

    @Override
    public String indexFace(String imageS3Key, UUID externalImageId) {
        long startedAt = System.nanoTime();
        try {
            Image image = Image.builder()
                    .s3Object(S3Object.builder()
                            .bucket(bucketName)
                            .name(imageS3Key)
                            .build())
                    .build();

            IndexFacesRequest indexFacesRequest = IndexFacesRequest.builder()
                    .collectionId(collectionId)
                    .image(image)
                    .externalImageId(externalImageId.toString())
                    .detectionAttributes(Attribute.DEFAULT)
                    .build();

            IndexFacesResponse response = kronosTracing.observe("kronos.external.rekognition",
                    () -> rekognitionClient.indexFaces(indexFacesRequest),
                    "provider", "rekognition", "operation", "index_face");

            if (response.faceRecords().isEmpty()) {
                kronosMetrics.recordExternalProviderRequest("rekognition", "index_face", "failure", "no_face_detected");
                kronosMetrics.recordExternalProviderRequestDuration("rekognition", "index_face",
                        java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
                log.warn("event=rekognition_no_face_detected storageRef={}",
                        privacyLogReferenceService.storageRef(imageS3Key));
                return null;
            }

            kronosMetrics.recordExternalProviderRequest("rekognition", "index_face", "success", "none");
            kronosMetrics.recordExternalProviderRequestDuration("rekognition", "index_face",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "success");
            return response.faceRecords().get(0).face().faceId();

        } catch (SdkException e) {
            kronosMetrics.recordExternalProviderRequest("rekognition", "index_face", "failure", "sdk_exception");
            kronosMetrics.recordExternalProviderRequestDuration("rekognition", "index_face",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
            log.error("event=rekognition_index_face_error externalImageRef={} exceptionType={}",
                    privacyLogReferenceService.externalImageRef(externalImageId), e.getClass().getSimpleName(), e);
            throw new RuntimeException("Falha ao registrar face no Rekognition.", e);
        }
    }

    @Override
    public UUID searchFaceByImage(InputStream imageStream) {
        long startedAt = System.nanoTime();
        try {
            byte[] imageBytes = imageStream.readAllBytes();
            SdkBytes sourceImageBytes = SdkBytes.fromByteArray(imageBytes);

            Image image = Image.builder().bytes(sourceImageBytes).build();

            SearchFacesByImageRequest searchRequest = SearchFacesByImageRequest.builder()
                    .collectionId(collectionId)
                    .image(image)
                    .faceMatchThreshold(FACE_MATCH_THRESHOLD)
                    .maxFaces(1)
                    .build();

            SearchFacesByImageResponse searchResponse = kronosTracing.observe("kronos.external.rekognition",
                    () -> rekognitionClient.searchFacesByImage(searchRequest),
                    "provider", "rekognition", "operation", "search_face");

            if (searchResponse.faceMatches().isEmpty()) {
                kronosMetrics.recordExternalProviderRequest("rekognition", "search_face", "failure", "no_match");
                kronosMetrics.recordExternalProviderRequestDuration("rekognition", "search_face",
                        java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
                return null;
            }

            FaceMatch match = searchResponse.faceMatches().get(0);
            String externalIdStr = match.face().externalImageId();
            kronosMetrics.recordExternalProviderRequest("rekognition", "search_face", "success", "none");
            kronosMetrics.recordExternalProviderRequestDuration("rekognition", "search_face",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "success");
            return UUID.fromString(externalIdStr);

        } catch (IOException e) {
            kronosMetrics.recordExternalProviderRequest("rekognition", "search_face", "failure", "io");
            kronosMetrics.recordExternalProviderRequestDuration("rekognition", "search_face",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
            log.error("event=rekognition_search_face_error reason=io exceptionType={}",
                    e.getClass().getSimpleName(), e);
            throw new RuntimeException("Falha ao ler a imagem para reconhecimento.", e);
        } catch (IllegalArgumentException e) {
            kronosMetrics.recordExternalProviderRequest("rekognition", "search_face", "failure", "invalid_external_image_id");
            kronosMetrics.recordExternalProviderRequestDuration("rekognition", "search_face",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
            log.error("event=rekognition_search_face_error reason=invalid_external_image_id exceptionType={}",
                    e.getClass().getSimpleName(), e);
            throw new RuntimeException("Falha no serviço de reconhecimento facial.", e);
        } catch (SdkException e) {
            kronosMetrics.recordExternalProviderRequest("rekognition", "search_face", "failure", "sdk_exception");
            kronosMetrics.recordExternalProviderRequestDuration("rekognition", "search_face",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
            log.error("event=rekognition_search_face_error reason=sdk_exception exceptionType={}",
                    e.getClass().getSimpleName(), e);
            throw new RuntimeException("Falha no serviço de reconhecimento facial.", e);
        }
    }

    @Override
    public void deleteFace(String faceId) {
        long startedAt = System.nanoTime();
        try {
            DeleteFacesRequest deleteFacesRequest = DeleteFacesRequest.builder()
                    .collectionId(collectionId)
                    .faceIds(faceId)
                    .build();
            kronosTracing.observe("kronos.external.rekognition",
                    () -> rekognitionClient.deleteFaces(deleteFacesRequest),
                    "provider", "rekognition", "operation", "delete_face");
            kronosMetrics.recordExternalProviderRequest("rekognition", "delete_face", "success", "none");
            kronosMetrics.recordExternalProviderRequestDuration("rekognition", "delete_face",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "success");
        } catch (SdkException e) {
            kronosMetrics.recordExternalProviderRequest("rekognition", "delete_face", "failure", "sdk_exception");
            kronosMetrics.recordExternalProviderRequestDuration("rekognition", "delete_face",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
            log.error("event=rekognition_delete_face_error faceRef={} exceptionType={}",
                    privacyLogReferenceService.faceRef(faceId), e.getClass().getSimpleName(), e);
        }
    }

    @Override
    public void deleteFacesByExternalImageId(UUID externalImageId) {
        long startedAt = System.nanoTime();
        try {
            List<String> faceIds = new ArrayList<>();
            String nextToken = null;

            do {
                var listRequest = ListFacesRequest.builder()
                        .collectionId(collectionId)
                        .maxResults(1000)
                        .nextToken(nextToken)
                        .build();

                var response = rekognitionClient.listFaces(listRequest);

                response.faces().stream()
                        .filter(face -> externalImageId.toString().equals(face.externalImageId()))
                        .map(Face::faceId)
                        .forEach(faceIds::add);

                nextToken = response.nextToken();
            } while (nextToken != null);

            if (faceIds.isEmpty()) {
                kronosMetrics.recordExternalProviderRequest("rekognition", "delete_faces_by_external_image_id", "success", "none");
                kronosMetrics.recordExternalProviderRequestDuration("rekognition", "delete_faces_by_external_image_id",
                        java.time.Duration.ofNanos(System.nanoTime() - startedAt), "success");
                return;
            }

            var deleteRequest = DeleteFacesRequest.builder()
                    .collectionId(collectionId)
                    .faceIds(faceIds)
                    .build();
            kronosTracing.observe("kronos.external.rekognition",
                    () -> rekognitionClient.deleteFaces(deleteRequest),
                    "provider", "rekognition", "operation", "delete_faces_by_external_image_id");

            kronosMetrics.recordExternalProviderRequest("rekognition", "delete_faces_by_external_image_id", "success", "none");
            kronosMetrics.recordExternalProviderRequestDuration("rekognition", "delete_faces_by_external_image_id",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "success");
            log.info("event=rekognition_delete_templates_success externalImageRef={}",
                    privacyLogReferenceService.externalImageRef(externalImageId));
        } catch (SdkException e) {
            kronosMetrics.recordExternalProviderRequest("rekognition", "delete_faces_by_external_image_id", "failure", "sdk_exception");
            kronosMetrics.recordExternalProviderRequestDuration("rekognition", "delete_faces_by_external_image_id",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
            log.error("event=rekognition_delete_templates_error externalImageRef={} exceptionType={}",
                    privacyLogReferenceService.externalImageRef(externalImageId), e.getClass().getSimpleName(), e);
        }
    }
}
