package com.kts.kronos.adapter.out.storage;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.domain.model.enuns.DocumentType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kronos.storage.provider", havingValue = "s3")
public class S3BucketStorageProviderImpl implements BucketStorageProvider {

    private final S3DocumentBucketProperties bucketProperties;

    @Value("${aws.region}")
    private String region;

    @Value("${aws.access-key-id}")
    private String accessKeyId;

    @Value("${aws.secret-access-key}")
    private String secretAccessKey;

    private S3Client s3Client;

    @PostConstruct
    void init() {
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKeyId, secretAccessKey)
                        )
                )
                .build();

        log.info("event=s3_storage_init result=success region={}", region);
    }

    @Override
    public String uploadFile(DocumentType documentType, String objectName, byte[] fileData, String contentType) {
        var bucket = bucketProperties.bucketFor(documentType);

        try {
            var request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectName)
                    .contentType(contentType)
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(fileData));

            log.info(
                    "event=document_s3_upload result=success document_type={} bucket={} key={}",
                    documentType,
                    bucket,
                    objectName
            );

            return objectName;
        } catch (SdkException e) {
            log.error(
                    "event=document_s3_upload result=failure document_type={} bucket={} key={} exception_type={}",
                    documentType,
                    bucket,
                    objectName,
                    e.getClass().getSimpleName(),
                    e
            );
            throw new RuntimeException("Erro ao enviar arquivo para o S3.", e);
        }
    }

    @Override
    public byte[] downloadFile(DocumentType documentType, String objectName) {
        var bucket = bucketProperties.bucketFor(documentType);

        try {
            var request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectName)
                    .build();

            var bytes = s3Client.getObject(request).readAllBytes();

            log.info(
                    "event=document_s3_download result=success document_type={} bucket={} key={} file_size_bytes={}",
                    documentType,
                    bucket,
                    objectName,
                    bytes.length
            );

            return bytes;
        } catch (NoSuchKeyException e) {
            log.warn(
                    "event=document_s3_download result=failure reason=object_not_found document_type={} bucket={} key={}",
                    documentType,
                    bucket,
                    objectName
            );
            throw new ResourceNotFoundException("Arquivo não encontrado no S3.");
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                log.warn(
                        "event=document_s3_download result=failure reason=object_not_found document_type={} bucket={} key={} status={}",
                        documentType,
                        bucket,
                        objectName,
                        e.statusCode()
                );
                throw new ResourceNotFoundException("Arquivo não encontrado no S3.");
            }

            log.error(
                    "event=document_s3_download result=failure reason=s3_error document_type={} bucket={} key={} status={}",
                    documentType,
                    bucket,
                    objectName,
                    e.statusCode(),
                    e
            );

            throw new RuntimeException("Erro ao baixar arquivo do S3.", e);
        } catch (IOException e) {
            log.error(
                    "event=document_s3_download result=failure reason=io document_type={} bucket={} key={}",
                    documentType,
                    bucket,
                    objectName,
                    e
            );
            throw new RuntimeException("Erro ao ler arquivo do S3.", e);
        }
    }

    @Override
    public void deleteFile(DocumentType documentType, String objectName) {
        var bucket = bucketProperties.bucketFor(documentType);

        try {
            var request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectName)
                    .build();

            s3Client.deleteObject(request);

            log.info(
                    "event=document_s3_delete result=success document_type={} bucket={} key={}",
                    documentType,
                    bucket,
                    objectName
            );
        } catch (SdkException e) {
            log.error(
                    "event=document_s3_delete result=failure document_type={} bucket={} key={} exception_type={}",
                    documentType,
                    bucket,
                    objectName,
                    e.getClass().getSimpleName(),
                    e
            );
            throw new RuntimeException("Erro ao excluir arquivo do S3.", e);
        }
    }
}
