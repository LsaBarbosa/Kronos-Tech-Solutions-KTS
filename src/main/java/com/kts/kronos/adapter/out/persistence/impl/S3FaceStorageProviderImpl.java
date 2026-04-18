package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3FaceStorageProviderImpl implements FaceStorageProvider {

    private final S3Client s3Client;
    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Override
    public String uploadFaceImage(UUID employeeId, InputStream imageStream, String contentType) {
        String objectKey = "faces/" + employeeId + "/" + UUID.randomUUID() + ".jpg";

        try {
            byte[] fileBytes = imageStream.readAllBytes();

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength((long) fileBytes.length)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));

            log.info("Upload de imagem facial concluído para S3. Key: {}", objectKey);
            return objectKey;

        } catch (IOException e) {
            log.error("Erro ao ler o stream da imagem para upload no S3: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao preparar a imagem para upload no S3.", e);
        } catch (SdkException e) {
            log.error("Erro no upload do arquivo para o S3: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao salvar a imagem no S3.", e);
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
            log.info("Exclusão de imagem facial do S3 concluída: {}", objectKey);
        } catch (SdkException e) {
            log.error("Erro na exclusão do arquivo {}: {}", objectKey, e.getMessage(), e);
        }
    }
}
