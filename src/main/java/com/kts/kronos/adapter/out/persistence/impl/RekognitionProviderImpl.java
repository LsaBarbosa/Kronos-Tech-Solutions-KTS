package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.exceptions.ServiceUnavailableException;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static com.kts.kronos.constants.ExceptionMessages.FACIAL_AUTH_UNAVAILABLE;
import static com.kts.kronos.constants.ExceptionMessages.FACIAL_RECOGNITION_SERVICE_ERROR;
import static com.kts.kronos.constants.ExceptionMessages.REKOGNITION_IMAGE_READ_ERROR;
import static com.kts.kronos.constants.ExceptionMessages.REKOGNITION_INIT_ERROR;
import static com.kts.kronos.constants.ExceptionMessages.REKOGNITION_REGISTER_FACE_ERROR;
import static com.kts.kronos.constants.Logs.LOG_REKOGNITION_COLLECTION_FATAL;
import static com.kts.kronos.constants.Logs.LOG_REKOGNITION_DELETE_ERROR;
import static com.kts.kronos.constants.Logs.LOG_REKOGNITION_INDEX_ERROR;
import static com.kts.kronos.constants.Logs.LOG_REKOGNITION_NO_FACE_DETECTED;
import static com.kts.kronos.constants.Logs.LOG_REKOGNITION_SEARCH_ERROR;
import static com.kts.kronos.constants.Logs.LOG_REKOGNITION_STREAM_READ_ERROR;

@Slf4j
@Component
@RequiredArgsConstructor
public class RekognitionProviderImpl  implements FaceRecognitionProvider {

    private final RekognitionClient rekognitionClient;
    private final FaceStorageProvider faceStorageProvider;

    @Value("${aws.rekognition.collection-id}")
    private String collectionId;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private static final float FACE_MATCH_THRESHOLD = 90.0f; // Limite de 90% para o Free Tier

    @Override
    public void ensureCollectionExists() {
        try {
            rekognitionClient.createCollection(CreateCollectionRequest.builder()
                    .collectionId(collectionId)
                    .build());
        } catch (ResourceAlreadyExistsException e) {
            // Se já existe, é o comportamento esperado no startup.
        } catch (Exception e) {
            log.error(LOG_REKOGNITION_COLLECTION_FATAL, collectionId, e.getMessage(), e);
            throw new RuntimeException(REKOGNITION_INIT_ERROR, e);
        }
    }

    @Override
    public String indexFace(String imageS3Key, UUID externalImageId) {
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

            IndexFacesResponse response = rekognitionClient.indexFaces(indexFacesRequest);

            if (response.faceRecords().isEmpty()) {
                log.warn(LOG_REKOGNITION_NO_FACE_DETECTED, imageS3Key);
                return null;
            }

            return response.faceRecords().get(0).face().faceId();

        } catch (Exception e) {
            log.error(LOG_REKOGNITION_INDEX_ERROR, externalImageId, e.getMessage(), e);
            throw new RuntimeException(REKOGNITION_REGISTER_FACE_ERROR, e);
        }
    }

    @Override
    public UUID searchFaceByImage(InputStream imageStream) {
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

            SearchFacesByImageResponse searchResponse = rekognitionClient.searchFacesByImage(searchRequest);

            if (searchResponse.faceMatches().isEmpty()) {
                return null;
            }

            FaceMatch match = searchResponse.faceMatches().get(0);
            String externalIdStr = match.face().externalImageId();
            return UUID.fromString(externalIdStr);

        } catch (IOException e) {
            log.error(LOG_REKOGNITION_STREAM_READ_ERROR, e.getMessage(), e);
            throw new RuntimeException(REKOGNITION_IMAGE_READ_ERROR, e);
        } catch (RekognitionException | SdkClientException e) {
            log.error("errorCode=FACIAL_PROVIDER_UNAVAILABLE message={}", e.getMessage(), e);
            throw new ServiceUnavailableException(FACIAL_AUTH_UNAVAILABLE);
        } catch (Exception e) {
            log.error(LOG_REKOGNITION_SEARCH_ERROR, e.getMessage(), e);
            throw new RuntimeException(FACIAL_RECOGNITION_SERVICE_ERROR, e);
        }
    }

    @Override
    public void deleteFace(String faceId) {
        try {
            DeleteFacesRequest deleteFacesRequest = DeleteFacesRequest.builder()
                    .collectionId(collectionId)
                    .faceIds(faceId)
                    .build();
            rekognitionClient.deleteFaces(deleteFacesRequest);
        } catch (Exception e) {
            log.error(LOG_REKOGNITION_DELETE_ERROR, faceId, e.getMessage(), e);
        }
    }
}