package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.port.out.provider.S3StorageProvider; // Sua interface
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
            log.info("Enviando arquivo para S3 (Legal): {}", keyName);

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

            log.info("✅ Upload S3 com Object Lock concluído: {}", keyName);
            return keyName;

        } catch (Exception e) {
            log.error("❌ Falha crítica ao enviar para o S3", e);
            throw new RuntimeException("Erro de comunicação com Storage S3", e);
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
            log.error("Erro de IO ao baixar arquivo do S3. key={}", fileKey, e);
            throw new RuntimeException("Falha de leitura do arquivo no S3", e);
        } catch (Exception e) {
            log.error("Erro ao baixar arquivo do S3. key={}", fileKey, e);
            throw new RuntimeException("Arquivo não encontrado ou erro S3", e);
        }
    }
}
