package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.domain.model.enuns.DocumentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(
    name = "kronos.storage.provider",
    havingValue = "local",
    matchIfMissing = false
)
public class BucketStorageProviderImpl implements BucketStorageProvider {
    private static final String INVALID_STORAGE_PATH = "Caminho de storage inválido.";

    @Value("${file.storage.root-path:/opt/kronos/documents}")
    private String rootPath;

    @Override
    public String uploadFile(DocumentType documentType, String originalFileName, byte[] fileData, String contentType) {
        try {
            Path filePath = resolveWithinRoot(originalFileName);

            // Garante que o diretório exista
            Files.createDirectories(filePath.getParent());

            // Escreve o arquivo no disco persistente
            Files.write(filePath, fileData);

            log.debug("Upload para disco local concluído.");
            return originalFileName;
        } catch (IOException e) {
            log.error("Erro no upload do arquivo para o disco local: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao salvar o arquivo no disco.", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(INVALID_STORAGE_PATH);
        }
    }

    @Override
    public byte[] downloadFile(DocumentType documentType, String objectName) {
        try {
            Path filePath = resolveWithinRoot(objectName);
            if (!Files.exists(filePath)) {
                throw new ResourceNotFoundException("Arquivo não encontrado no disco.");
            }
            // Lê e retorna os bytes do arquivo
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Erro no download/leitura do arquivo {}: {}", objectName, e.getMessage());
            throw new RuntimeException("Falha ao ler o arquivo do disco.", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(INVALID_STORAGE_PATH);
        }
    }

    @Override
    public void deleteFile(DocumentType documentType, String objectName) {
        try {
            Path filePath = resolveWithinRoot(objectName);
            Files.deleteIfExists(filePath);
            log.debug("Exclusão de arquivo local concluída.");
        } catch (IOException e) {
            log.error("Erro na exclusão do arquivo {}: {}", objectName, e.getMessage());
            throw new RuntimeException("Falha ao excluir o arquivo do disco.", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(INVALID_STORAGE_PATH);
        }
    }

    private Path resolveWithinRoot(String objectName) {
        if (objectName == null || objectName.isBlank()) {
            throw new IllegalArgumentException(INVALID_STORAGE_PATH);
        }

        Path root = Paths.get(rootPath).toAbsolutePath().normalize();
        String normalizedObjectName = objectName.trim().replace('\\', '/');
        while (normalizedObjectName.startsWith("/")) {
            normalizedObjectName = normalizedObjectName.substring(1);
        }

        Path relativePath = Paths.get(normalizedObjectName).normalize();
        if (relativePath.isAbsolute() || relativePath.startsWith("..")) {
            throw new IllegalArgumentException(INVALID_STORAGE_PATH);
        }

        Path finalPath = root.resolve(relativePath).normalize();
        if (!finalPath.startsWith(root)) {
            throw new IllegalArgumentException(INVALID_STORAGE_PATH);
        }

        return finalPath;
    }
}
