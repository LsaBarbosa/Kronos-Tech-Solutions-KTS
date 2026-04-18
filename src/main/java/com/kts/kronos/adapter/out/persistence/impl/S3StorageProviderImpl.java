package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.S3StorageProvider; // Sua interface
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.ObjectLockMode;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
public class S3StorageProviderImpl implements S3StorageProvider {

    @Value("${aws.s3.bucket-name-doc}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.access-key-id}")
    private String accessKey;

    @Value("${aws.secret-access-key}")
    private String secretKey;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
        log.info("🚀 Storage Provider S3 ATIVO. Bucket: {}", bucketName);
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
                    .objectLockRetainUntilDate(Instant.now().plus(1825, ChronoUnit.DAYS)); // 5 Anos

            s3Client.putObject(putObBuilder.build(), RequestBody.fromBytes(content));

            log.info("✅ Upload S3 com Object Lock concluído: {}", keyName);
            return keyName;

        } catch (SdkException e) {
            log.error("❌ Falha crítica ao enviar para o S3", e);
            throw new RuntimeException("Erro de comunicação com Storage S3", e);
        }
    }

    @Override
    public byte[] downloadFile(String fileKey) {
        try {
            GetObjectRequest getOb = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            return s3Client.getObject(getOb).readAllBytes();
        } catch (software.amazon.awssdk.services.s3.model.NoSuchKeyException e) {
            throw new ResourceNotFoundException("Arquivo não encontrado no S3.");
        } catch (software.amazon.awssdk.services.s3.model.S3Exception e) {
            if (e.statusCode() == 404) {
                throw new ResourceNotFoundException("Arquivo não encontrado no S3.");
            }
            log.error("Erro ao baixar arquivo do S3. key={}, statusCode={}", fileKey, e.statusCode(), e);
            throw new RuntimeException("Erro ao baixar arquivo do S3.", e);
        } catch (IOException e) {
            log.error("Erro de IO ao baixar arquivo do S3. key={}", fileKey, e);
            throw new RuntimeException("Erro ao ler arquivo do S3.", e);
        } catch (SdkException e) {
            log.error("Erro ao baixar arquivo do S3. key={}", fileKey, e);
            throw new RuntimeException("Arquivo não encontrado ou erro S3", e);
        }
    }
}
