package com.kts.kronos.adapter.out.persistence.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3FaceStorageProviderImplTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private S3FaceStorageProviderImpl provider;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(provider, "bucketName", "bucket-test");
    }

    @Test
    @DisplayName("uploadFaceImage: deve fazer upload com sucesso")
    void shouldUploadFaceImageSuccessfully() {
        UUID employeeId = UUID.randomUUID();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().eTag("ok").build());

        String key = provider.uploadFaceImage(employeeId,
                new java.io.ByteArrayInputStream("image".getBytes()),
                "image/jpeg");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest request = captor.getValue();
        assertEquals("bucket-test", request.bucket());
        assertEquals("image/jpeg", request.contentType());
        assertTrue(request.key().startsWith("faces/" + employeeId + "/"));
        assertTrue(request.key().endsWith(".jpg"));
        assertEquals(request.key(), key);
    }

    @Test
    @DisplayName("uploadFaceImage: deve falhar ao ler stream")
    void shouldFailWhenReadingStream() {
        UUID employeeId = UUID.randomUUID();

        InputStream brokenStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("read failure");
            }
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> provider.uploadFaceImage(employeeId, brokenStream, "image/jpeg"));

        assertEquals("Falha ao preparar a imagem para upload no S3.", ex.getMessage());
    }

    @Test
    @DisplayName("uploadFaceImage: deve falhar em erro genérico de upload")
    void shouldFailWhenUploadHasSdkError() {
        UUID employeeId = UUID.randomUUID();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(S3Exception.builder().message("boom").build());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> provider.uploadFaceImage(employeeId,
                        new java.io.ByteArrayInputStream("image".getBytes()),
                        "image/jpeg"));

        assertEquals("Falha ao salvar a imagem no S3.", ex.getMessage());
    }

    @Test
    @DisplayName("deleteFaceImage: deve deletar com sucesso")
    void shouldDeleteFaceImageSuccessfully() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());

        assertDoesNotThrow(() -> provider.deleteFaceImage("faces/test.jpg"));

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());

        DeleteObjectRequest request = captor.getValue();
        assertEquals("bucket-test", request.bucket());
        assertEquals("faces/test.jpg", request.key());
    }

    @Test
    @DisplayName("deleteFaceImage: deve absorver exceção do SDK")
    void shouldAbsorbDeleteException() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(S3Exception.builder().message("boom").build());

        assertDoesNotThrow(() -> provider.deleteFaceImage("faces/test.jpg"));
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }
}