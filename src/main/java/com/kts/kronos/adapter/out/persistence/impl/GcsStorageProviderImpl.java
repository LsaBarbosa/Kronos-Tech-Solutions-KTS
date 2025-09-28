package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.port.out.provider.GcsStorageProvider;
import com.google.cloud.storage.*;
import com.kts.kronos.application.port.out.provider.GcsStorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class GcsStorageProviderImpl implements GcsStorageProvider {
    private final Storage storage;
    private final String bucketName;

    public GcsStorageProviderImpl(@Value("${gcp.storage.bucket-name}") String bucketName) {
        // Inicializa o cliente Storage, que usa a autenticação padrão do ambiente.
        this.storage = StorageOptions.getDefaultInstance().getService();
        this.bucketName = bucketName;
    }

    @Override
    public String uploadFile(String objectName, byte[] fileData, String contentType) {
        log.info("Iniciando upload para GCS: bucket={}, object={}", bucketName, objectName);
        try {
            BlobId blobId = BlobId.of(bucketName, objectName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .build();

            // Sobe o arquivo
            storage.create(blobInfo, fileData);
            log.info("Upload para GCS concluído: object={}", objectName);
            return objectName;
        } catch (StorageException e) {
            log.error("Erro no upload do arquivo para GCS: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao fazer upload para o Google Cloud Storage.", e);
        }
    }

    @Override
    public byte[] downloadFile(String objectName) {
        log.info("Iniciando download do GCS: bucket={}, object={}", bucketName, objectName);
        try {
            BlobId blobId = BlobId.of(bucketName, objectName);
            Blob blob = storage.get(blobId);

            if (blob == null) {
                throw new StorageException(404, "Arquivo não encontrado no GCS: " + objectName);
            }

            // Baixa o conteúdo
            byte[] content = blob.getContent();

            log.info("Download do GCS concluído: object={}", objectName);
            return content;
        } catch (StorageException e) {
            log.error("Erro no download do arquivo do GCS: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao fazer download do Google Cloud Storage.", e);
        }
    }

    @Override
    public void deleteFile(String objectName) {
        log.info("Iniciando exclusão do GCS: bucket={}, object={}", bucketName, objectName);
        try {
            BlobId blobId = BlobId.of(bucketName, objectName);
            storage.delete(blobId);
            log.info("Exclusão do GCS concluída: object={}", objectName);
        } catch (StorageException e) {
            log.error("Erro na exclusão do arquivo do GCS: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao excluir o arquivo do Google Cloud Storage.", e);
        }
    }
}
