package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.port.out.provider.S3StorageProvider; // Sua interface
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.kts.kronos.constants.ExceptionMessages.S3_COMMUNICATION_ERROR;
import static com.kts.kronos.constants.ExceptionMessages.S3_NOT_FOUND_OR_ERROR;
import static com.kts.kronos.constants.ExceptionMessages.S3_READ_ERROR;
import static com.kts.kronos.constants.Logs.LOG_S3_LEGAL_DOWNLOAD_ERROR;
import static com.kts.kronos.constants.Logs.LOG_S3_LEGAL_DOWNLOAD_IO_ERROR;
import static com.kts.kronos.constants.Logs.LOG_S3_LEGAL_UPLOAD_FATAL;
import static com.kts.kronos.constants.Logs.LOG_S3_LEGAL_UPLOAD_START;
import static com.kts.kronos.constants.Logs.LOG_S3_LEGAL_UPLOAD_SUCCESS;

@Slf4j
@Service
public class S3StorageProviderImpl implements S3StorageProvider {

    @Value("${aws.s3.bucket-name-doc}")
    private String bucketName;
    private final S3Client s3Client;

    public S3StorageProviderImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String uploadFile(String keyName, byte[] content) {
        try {
            log.info(LOG_S3_LEGAL_UPLOAD_START, keyName);

            PutObjectRequest.Builder putObBuilder = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(keyName)
                    // --- A CORREÇÃO ESTÁ AQUI ---
                    // A AWS exige isso quando usamos Object Lock.
                    // O SDK calcula o hash automaticamente ao enviar.
                    .checksumAlgorithm(ChecksumAlgorithm.SHA256);
            // ----------------------------

            // Configuração de Object Lock (Imutabilidade)
            putObBuilder.objectLockMode(ObjectLockMode.GOVERNANCE)
                    .objectLockRetainUntilDate(Instant.now().plus(1825, ChronoUnit.DAYS));

            s3Client.putObject(putObBuilder.build(), RequestBody.fromBytes(content));

            log.info(LOG_S3_LEGAL_UPLOAD_SUCCESS, keyName);
            return keyName;

        } catch (Exception e) {
            log.error(LOG_S3_LEGAL_UPLOAD_FATAL, e);
            throw new RuntimeException(S3_COMMUNICATION_ERROR, e);
        }
    }

    @Override
    public byte[] downloadFile(String fileKey) {

        GetObjectRequest getOb = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .build();
        try (ResponseInputStream<GetObjectResponse> stream = s3Client.getObject(getOb)) {
            return stream.readAllBytes();
        } catch (IOException e) {
            log.error(LOG_S3_LEGAL_DOWNLOAD_IO_ERROR, fileKey, e);
            throw new RuntimeException(S3_READ_ERROR, e);
        } catch (Exception e) {
            log.error(LOG_S3_LEGAL_DOWNLOAD_ERROR, fileKey, e);
            throw new RuntimeException(S3_NOT_FOUND_OR_ERROR, e);
        }
    }
}
