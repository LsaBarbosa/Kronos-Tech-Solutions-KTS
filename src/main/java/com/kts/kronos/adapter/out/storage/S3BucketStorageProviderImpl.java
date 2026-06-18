package com.kts.kronos.adapter.out.storage;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.io.IOException;
import java.io.UncheckedIOException;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kronos.storage.provider", havingValue = "s3")
public class S3BucketStorageProviderImpl implements BucketStorageProvider {

    private final S3DocumentBucketProperties bucketProperties;
    private final S3Client s3Client;
    private final KronosMetrics kronosMetrics;
    private final KronosTracing kronosTracing;

    @PostConstruct
    void init() {
        log.info("event=s3_storage_init result=success");
    }

    @Override
    public String uploadFile(DocumentType documentType, String objectName, byte[] fileData, String contentType) {
        var bucket = bucketProperties.bucketFor(documentType);
        long startedAt = System.nanoTime();

        try {
            kronosTracing.observe("kronos.external.s3", () -> {
                var request = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectName)
                        .contentType(contentType)
                        .serverSideEncryption(ServerSideEncryption.AES256)
                        .build();

                s3Client.putObject(request, RequestBody.fromBytes(fileData));
            }, "provider", "s3", "operation", "upload");

            kronosMetrics.recordExternalProviderRequest("s3", "upload", "success", "none");
            kronosMetrics.recordExternalProviderRequestDuration("s3", "upload",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "success");

            log.info(
                    "event=document_s3_upload result=success document_type={} bucket={} key={}",
                    documentType,
                    bucket,
                    objectName
            );

            return objectName;
        } catch (SdkException e) {
            kronosMetrics.recordExternalProviderRequest("s3", "upload", "failure", "sdk_exception");
            kronosMetrics.recordExternalProviderRequestDuration("s3", "upload",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
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
        long startedAt = System.nanoTime();

        try {
            byte[] bytes = kronosTracing.observe("kronos.external.s3", () -> {
                try {
                    var request = GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectName)
                            .build();

                    return s3Client.getObject(request).readAllBytes();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }, "provider", "s3", "operation", "download");

            kronosMetrics.recordExternalProviderRequest("s3", "download", "success", "none");
            kronosMetrics.recordExternalProviderRequestDuration("s3", "download",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "success");

            log.info(
                    "event=document_s3_download result=success document_type={} bucket={} key={} file_size_bytes={}",
                    documentType,
                    bucket,
                    objectName,
                    bytes.length
            );

            return bytes;
        } catch (NoSuchKeyException e) {
            kronosMetrics.recordExternalProviderRequest("s3", "download", "failure", "object_not_found");
            kronosMetrics.recordExternalProviderRequestDuration("s3", "download",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
            log.warn(
                    "event=document_s3_download result=failure reason=object_not_found document_type={} bucket={} key={}",
                    documentType,
                    bucket,
                    objectName
            );
            throw new ResourceNotFoundException("Arquivo não encontrado no S3.");
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                kronosMetrics.recordExternalProviderRequest("s3", "download", "failure", "object_not_found");
                kronosMetrics.recordExternalProviderRequestDuration("s3", "download",
                        java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
                log.warn(
                        "event=document_s3_download result=failure reason=object_not_found document_type={} bucket={} key={} status={}",
                        documentType,
                        bucket,
                        objectName,
                        e.statusCode()
                );
                throw new ResourceNotFoundException("Arquivo não encontrado no S3.");
            }

            kronosMetrics.recordExternalProviderRequest("s3", "download", "failure", "s3_error");
            kronosMetrics.recordExternalProviderRequestDuration("s3", "download",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
            log.error(
                    "event=document_s3_download result=failure reason=s3_error document_type={} bucket={} key={} status={}",
                    documentType,
                    bucket,
                    objectName,
                    e.statusCode(),
                    e
            );

            throw new RuntimeException("Erro ao baixar arquivo do S3.", e);
        } catch (UncheckedIOException e) {
            kronosMetrics.recordExternalProviderRequest("s3", "download", "failure", "io");
            kronosMetrics.recordExternalProviderRequestDuration("s3", "download",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
            log.error(
                    "event=document_s3_download result=failure reason=io document_type={} bucket={} key={}",
                    documentType,
                    bucket,
                    objectName,
                    e
            );
            throw new RuntimeException("Erro ao ler arquivo do S3.", e.getCause());
        }
    }

    @Override
    public void deleteFile(DocumentType documentType, String objectName) {
        var bucket = bucketProperties.bucketFor(documentType);
        long startedAt = System.nanoTime();

        try {
            kronosTracing.observe("kronos.external.s3", () -> {
                var request = DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectName)
                        .build();

                s3Client.deleteObject(request);
            }, "provider", "s3", "operation", "delete");

            kronosMetrics.recordExternalProviderRequest("s3", "delete", "success", "none");
            kronosMetrics.recordExternalProviderRequestDuration("s3", "delete",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "success");

            log.info(
                    "event=document_s3_delete result=success document_type={} bucket={} key={}",
                    documentType,
                    bucket,
                    objectName
            );
        } catch (SdkException e) {
            kronosMetrics.recordExternalProviderRequest("s3", "delete", "failure", "sdk_exception");
            kronosMetrics.recordExternalProviderRequestDuration("s3", "delete",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
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
