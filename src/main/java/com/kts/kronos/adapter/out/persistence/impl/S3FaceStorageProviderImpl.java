package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static com.kts.kronos.constants.ExceptionMessages.S3_FACE_PREPARE_UPLOAD_ERROR;
import static com.kts.kronos.constants.ExceptionMessages.S3_FACE_SAVE_ERROR;
import static com.kts.kronos.constants.Logs.LOG_S3_FACE_DELETE_ERROR;
import static com.kts.kronos.constants.Logs.LOG_S3_FACE_DELETE_SUCCESS;
import static com.kts.kronos.constants.Logs.LOG_S3_FACE_STREAM_ERROR;
import static com.kts.kronos.constants.Logs.LOG_S3_FACE_UPLOAD_SUCCESS;
import static com.kts.kronos.constants.Logs.LOG_S3_UPLOAD_ERROR;
import static com.kts.kronos.constants.StoragePaths.PATH_FACES;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3FaceStorageProviderImpl implements FaceStorageProvider {

    private final S3Client s3Client;
    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Override
    public String uploadFaceImage(UUID employeeId, InputStream imageStream, String contentType) {
        String objectKey = PATH_FACES + employeeId + "/" + UUID.randomUUID() + ".jpg";

        try {
            byte[] fileBytes = imageStream.readAllBytes();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength((long) fileBytes.length)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));

            log.info(LOG_S3_FACE_UPLOAD_SUCCESS, objectKey);
            return objectKey;

        } catch (IOException e) {
            log.error(LOG_S3_FACE_STREAM_ERROR, e.getMessage(), e);
            throw new RuntimeException(S3_FACE_PREPARE_UPLOAD_ERROR, e);
        } catch (Exception e) {
            log.error(LOG_S3_UPLOAD_ERROR, e.getMessage(), e);
            throw new RuntimeException(S3_FACE_SAVE_ERROR, e);
        }
    }

    @Override
    public void deleteFaceImage(String objectKey) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info(LOG_S3_FACE_DELETE_SUCCESS, objectKey);
        } catch (Exception e) {
            log.error(LOG_S3_FACE_DELETE_ERROR, objectKey, e.getMessage(), e);
        }
    }
}