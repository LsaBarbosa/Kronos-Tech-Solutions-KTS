package com.kts.kronos.adapter.out.persistence.impl;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@RequiredArgsConstructor
public class BucketStorageProviderImpl implements BucketStorageProvider {
    private final Storage storage;

    @Value("${spring.cloud.gcp.storage.bucket-name}")
    private String bucketName;

    @Override
    public String uploadFile(String objectName, byte[] fileData, String contentType) {
        log.info("Iniciando upload para GCS: bucket={}, object={}", bucketName, objectName);
        try {
            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType(contentType).build();
            storage.create(blobInfo, fileData);
            log.info("Upload para GCS concluído: object={}", objectName);
            return objectName; // No GCS, o caminho é o próprio nome do objeto
        } catch (Exception e) {
            log.error("Erro no upload do arquivo para GCS: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao fazer upload para o Bucket GCS.", e);
        }
    }

    @Override
    public byte[] downloadFile(String objectName) {
        log.info("Iniciando download do GCS: bucket={}, object={}", bucketName, objectName);
        try {
            byte[] content = storage.readAllBytes(bucketName, objectName);
            log.info("Download do GCS concluído: object={}", objectName);
            return content;
        } catch (Exception e) {
            log.error("Arquivo não encontrado no GCS: {}", objectName, e);
            throw new ResourceNotFoundException("Arquivo não encontrado no GCS: " + objectName);
        }
    }

    @Override
    public void deleteFile(String objectName) {
        log.info("Iniciando exclusão do GCS: bucket={}, object={}", bucketName, objectName);
        try {
            storage.delete(bucketName, objectName);
            log.info("Exclusão do GCS concluída: object={}", objectName);
        } catch (Exception e) {
            log.error("Erro na exclusão do arquivo do GCS: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao excluir o arquivo do Bucket GCS.", e);
        }
    }
}