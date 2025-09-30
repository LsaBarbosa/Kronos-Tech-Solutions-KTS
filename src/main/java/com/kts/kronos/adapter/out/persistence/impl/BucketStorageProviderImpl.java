package com.kts.kronos.adapter.out.persistence.impl;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
// Imports do AWS SDK V2
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
@Slf4j
@Component
public class BucketStorageProviderImpl implements BucketStorageProvider {
    private final S3Client s3Client;
    private final String bucketName;

    // Injetamos o bucketName e a região para inicializar o S3Client
    public BucketStorageProviderImpl(
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${cloud.aws.region.static}") String region) { // Propriedade 'cloud.aws.region.static'

        this.bucketName = bucketName;
        // Inicializa o cliente S3, respeitando a região configurada no application.yml
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    @Override
    public String uploadFile(String objectName, byte[] fileData, String contentType) {
        log.info("Iniciando upload para AWS S3: bucket={}, object={}", bucketName, objectName);
        try {
            // Cria a requisição PutObject (equivalente a BlobInfo)
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .contentType(contentType)
                    .contentLength((long) fileData.length)
                    .build();

            // Sobe o arquivo
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileData));

            log.info("Upload para AWS S3 concluído: object={}", objectName);
            return objectName;
        } catch (S3Exception e) { // Captura exceções específicas do S3
            log.error("Erro no upload do arquivo para AWS S3: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao fazer upload para o Bucket S3.", e);
        }
    }

    @Override
    public byte[] downloadFile(String objectName) {
        log.info("Iniciando download do AWS S3: bucket={}, object={}", bucketName, objectName);
        try {
            // Cria a requisição GetObject
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .build();

            // Baixa o conteúdo como bytes
            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getObjectRequest);
            byte[] content = s3Object.readAllBytes();

            log.info("Download do AWS S3 concluído: object={}", objectName);
            return content;
        } catch (NoSuchKeyException e) { // Exceção para objeto não encontrado (equivalente a 404)
            log.error("Arquivo não encontrado no AWS S3: {}", objectName, e);
            throw new ResourceNotFoundException("Arquivo não encontrado no AWS S3: " + objectName);
        } catch (S3Exception | IOException e) {
            log.error("Erro no download do arquivo do AWS S3: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao fazer download do Bucket S3.", e);
        }
    }

    @Override
    public void deleteFile(String objectName) {
        log.info("Iniciando exclusão do AWS S3: bucket={}, object={}", bucketName, objectName);
        try {
            // Cria a requisição DeleteObject
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .build();

            // Exclui o arquivo
            s3Client.deleteObject(deleteObjectRequest);

            log.info("Exclusão do AWS S3 concluída: object={}", objectName);
        } catch (S3Exception e) {
            log.error("Erro na exclusão do arquivo do AWS S3: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao excluir o arquivo do Bucket S3.", e);
        }
    }
}