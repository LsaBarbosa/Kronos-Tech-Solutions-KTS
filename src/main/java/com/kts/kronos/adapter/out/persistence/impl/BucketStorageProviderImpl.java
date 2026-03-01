package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static com.kts.kronos.constants.ExceptionMessages.LOCAL_FILE_DELETE_ERROR;
import static com.kts.kronos.constants.ExceptionMessages.LOCAL_FILE_NOT_FOUND;
import static com.kts.kronos.constants.ExceptionMessages.LOCAL_FILE_READ_ERROR;
import static com.kts.kronos.constants.ExceptionMessages.LOCAL_FILE_SAVE_ERROR;
import static com.kts.kronos.constants.Logs.LOG_LOCAL_DELETE_ERROR;
import static com.kts.kronos.constants.Logs.LOG_LOCAL_DELETE_SUCCESS;
import static com.kts.kronos.constants.Logs.LOG_LOCAL_DOWNLOAD_ERROR;
import static com.kts.kronos.constants.Logs.LOG_LOCAL_UPLOAD_ERROR;
import static com.kts.kronos.constants.Logs.LOG_LOCAL_UPLOAD_SUCCESS;

@Slf4j
@Component
public class BucketStorageProviderImpl implements BucketStorageProvider {
    @Value("${file.storage.root-path:/mnt/data/documents}")
    private String rootPath;

    @Override
    public String uploadFile(String originalFileName, byte[] fileData, String contentType) {
        try {
            // Cria um nome de objeto único
            String uniqueObjectName = UUID.randomUUID() + "-" + originalFileName;
            Path filePath = Paths.get(rootPath, uniqueObjectName);

            // Garante que o diretório exista
            Files.createDirectories(filePath.getParent());

            // Escreve o arquivo no disco persistente
            Files.write(filePath, fileData);

            log.info(LOG_LOCAL_UPLOAD_SUCCESS, filePath);
            return uniqueObjectName; // Retorna apenas o nome do objeto (para ser salvo no DB)
        } catch (IOException e) {
            log.error(LOG_LOCAL_UPLOAD_ERROR, e.getMessage(), e);
            throw new RuntimeException(LOCAL_FILE_SAVE_ERROR, e);
        }
    }

    @Override
    public byte[] downloadFile(String objectName) {
        Path filePath = Paths.get(rootPath, objectName);
        try {
            if (!Files.exists(filePath)) {
                throw new ResourceNotFoundException(LOCAL_FILE_NOT_FOUND + objectName);
            }
            // Lê e retorna os bytes do arquivo
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error(LOG_LOCAL_DOWNLOAD_ERROR, objectName, e.getMessage());
            throw new RuntimeException(LOCAL_FILE_READ_ERROR, e);
        }
    }

    @Override
    public void deleteFile(String objectName) {
        Path filePath = Paths.get(rootPath, objectName);
        try {
            Files.deleteIfExists(filePath);
            log.info(LOG_LOCAL_DELETE_SUCCESS, objectName);
        } catch (IOException e) {
            log.error(LOG_LOCAL_DELETE_ERROR, objectName, e.getMessage());
            throw new RuntimeException(LOCAL_FILE_DELETE_ERROR, e);
        }
    }
}