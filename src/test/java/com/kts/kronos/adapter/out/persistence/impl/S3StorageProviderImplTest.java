package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class S3StorageProviderImplTest {

    @Test
    @DisplayName("init: deve inicializar client")
    void shouldInitClient() {
        S3StorageProviderImpl provider = new S3StorageProviderImpl();

        ReflectionTestUtils.setField(provider, "bucketName", "bucket-doc");
        ReflectionTestUtils.setField(provider, "region", "sa-east-1");
        ReflectionTestUtils.setField(provider, "accessKey", "access");
        ReflectionTestUtils.setField(provider, "secretKey", "secret");

        S3ClientBuilder builder = mock(S3ClientBuilder.class);
        S3Client client = mock(S3Client.class);

        when(builder.region(any(Region.class))).thenReturn(builder);
        when(builder.credentialsProvider(any(AwsCredentialsProvider.class))).thenReturn(builder);
        when(builder.build()).thenReturn(client);

        try (MockedStatic<S3Client> mocked = mockStatic(S3Client.class)) {
            mocked.when(S3Client::builder).thenReturn(builder);

            provider.init();
        }

        assertSame(client, ReflectionTestUtils.getField(provider, "s3Client"));
    }

    @Test
    @DisplayName("uploadFile: deve fazer upload com sucesso")
    void shouldUploadFileSuccessfully() {
        S3StorageProviderImpl provider = new S3StorageProviderImpl();
        S3Client s3Client = mock(S3Client.class);

        ReflectionTestUtils.setField(provider, "bucketName", "bucket-doc");
        ReflectionTestUtils.setField(provider, "s3Client", s3Client);

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String key = provider.uploadFile("legal/file.pdf", "pdf-content".getBytes());

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest request = captor.getValue();
        assertEquals("bucket-doc", request.bucket());
        assertEquals("legal/file.pdf", request.key());
        assertEquals(ChecksumAlgorithm.SHA256, request.checksumAlgorithm());
        assertEquals(ObjectLockMode.GOVERNANCE, request.objectLockMode());
        assertNotNull(request.objectLockRetainUntilDate());
        assertEquals("legal/file.pdf", key);
    }

    @Test
    @DisplayName("uploadFile: deve falhar no upload")
    void shouldFailOnUpload() {
        S3StorageProviderImpl provider = new S3StorageProviderImpl();
        S3Client s3Client = mock(S3Client.class);

        ReflectionTestUtils.setField(provider, "bucketName", "bucket-doc");
        ReflectionTestUtils.setField(provider, "s3Client", s3Client);

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("boom").build());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> provider.uploadFile("legal/file.pdf", "pdf-content".getBytes()));

        assertEquals("Erro de comunicação com Storage S3", ex.getMessage());
    }

    @Test
    @DisplayName("downloadFile: deve baixar arquivo com sucesso")
    void shouldDownloadFileSuccessfully() throws IOException {
        S3StorageProviderImpl provider = new S3StorageProviderImpl();
        S3Client s3Client = mock(S3Client.class);

        ReflectionTestUtils.setField(provider, "bucketName", "bucket-doc");
        ReflectionTestUtils.setField(provider, "s3Client", s3Client);

        byte[] expected = "pdf-content".getBytes();

        @SuppressWarnings("unchecked")
        ResponseInputStream<GetObjectResponse> responseStream = mock(ResponseInputStream.class);

        when(responseStream.readAllBytes()).thenReturn(expected);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

        byte[] result = provider.downloadFile("legal/file.pdf");

        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(captor.capture());

        GetObjectRequest request = captor.getValue();
        assertEquals("bucket-doc", request.bucket());
        assertEquals("legal/file.pdf", request.key());
        assertArrayEquals(expected, result);
    }

    @Test
    @DisplayName("downloadFile: deve falhar quando arquivo não existir")
    void shouldFailWhenDownloadCannotFindFile() {
        S3StorageProviderImpl provider = new S3StorageProviderImpl();
        S3Client s3Client = mock(S3Client.class);

        ReflectionTestUtils.setField(provider, "bucketName", "bucket-doc");
        ReflectionTestUtils.setField(provider, "s3Client", s3Client);

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("missing").build());

        assertThrows(ResourceNotFoundException.class,
                () -> provider.downloadFile("legal/file.pdf"));
    }
}