package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.exceptions.ServiceUnavailableException;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import com.kts.kronos.domain.model.Address;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageAndExternalProviderImplsTest {

    @Mock JavaMailSender mailSender;
    @Mock S3Client s3Client;
    @Mock RekognitionClient rekognitionClient;
    @Mock FaceStorageProvider faceStorageProvider;

    @InjectMocks EmailSenderProviderImpl emailSenderProvider;
    @InjectMocks S3FaceStorageProviderImpl s3FaceStorageProvider;
    @InjectMocks RekognitionProviderImpl rekognitionProvider;

    @Test
    void bucketStorage_shouldCoverSuccessAndFailures(@TempDir Path tempDir) throws Exception {
        BucketStorageProviderImpl provider = new BucketStorageProviderImpl();
        ReflectionTestUtils.setField(provider, "rootPath", tempDir.toString());

        String object = provider.uploadFile("a.txt", "abc".getBytes(), "text/plain");
        assertThat(provider.downloadFile(object)).isEqualTo("abc".getBytes());
        provider.deleteFile(object);
        assertThatThrownBy(() -> provider.downloadFile("missing"))
                .isInstanceOf(ResourceNotFoundException.class);

        Path fileAsRoot = tempDir.resolve("root-file");
        Files.writeString(fileAsRoot, "x");
        ReflectionTestUtils.setField(provider, "rootPath", fileAsRoot.toString());
        assertThatThrownBy(() -> provider.uploadFile("b.txt", new byte[]{1}, "text/plain"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void emailSender_shouldCoverHappyAndFailures() {
        MimeMessage message = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(message);
        ReflectionTestUtils.setField(emailSenderProvider, "emailRemetente", "from@test.com");

        emailSenderProvider.sendResetEmail("to@test.com", "token", "user", "https://front/reset");
        verify(mailSender).send(message);

        assertThatThrownBy(() -> emailSenderProvider.sendResetEmail("", "t", "u", "url"))
                .isInstanceOf(IllegalArgumentException.class);

        ReflectionTestUtils.setField(emailSenderProvider, "emailRemetente", " ");
        assertThatThrownBy(() -> emailSenderProvider.sendResetEmail("to@test.com", "t", "u", "url"))
                .isInstanceOf(IllegalStateException.class);

        ReflectionTestUtils.setField(emailSenderProvider, "emailRemetente", "from@test.com");
        doThrow(new MailSendException("send")).when(mailSender).send(any(MimeMessage.class));
        assertThatThrownBy(() -> emailSenderProvider.sendResetEmail("to@test.com", "t", "u", "url"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void s3Storage_shouldCoverUploadAndDownloadErrors() throws Exception {
        S3StorageProviderImpl provider = new S3StorageProviderImpl(s3Client);
        ReflectionTestUtils.setField(provider, "bucketName", "bucket");

        assertThat(provider.uploadFile("k", new byte[]{1})).isEqualTo("k");
        doThrow(new RuntimeException("x")).when(s3Client).putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class), any(RequestBody.class));
        assertThatThrownBy(() -> provider.uploadFile("k2", new byte[]{1})).isInstanceOf(RuntimeException.class);

        ResponseInputStream<GetObjectResponse> okStream = new ResponseInputStream<>(GetObjectResponse.builder().build(),
                new ByteArrayInputStream("abc".getBytes()));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(okStream);
        assertThat(provider.downloadFile("f")).isEqualTo("abc".getBytes());

        ResponseInputStream<GetObjectResponse> badIo = new ResponseInputStream<>(GetObjectResponse.builder().build(),
                new InputStream() {
                    @Override public int read() throws IOException { throw new IOException("io"); }
                });
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(badIo);
        assertThatThrownBy(() -> provider.downloadFile("f2")).isInstanceOf(RuntimeException.class);

        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(new RuntimeException("generic"));
        assertThatThrownBy(() -> provider.downloadFile("f3")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void s3Face_shouldCoverMethods() {
        ReflectionTestUtils.setField(s3FaceStorageProvider, "bucketName", "bucket");
        String key = s3FaceStorageProvider.uploadFaceImage(UUID.randomUUID(), new ByteArrayInputStream(new byte[]{1}), "image/jpeg");
        assertThat(key).contains("faces/");

        assertThatThrownBy(() -> s3FaceStorageProvider.uploadFaceImage(UUID.randomUUID(), new InputStream() {
            @Override public int read() throws IOException { throw new IOException("io"); }
        }, "image/jpeg")).isInstanceOf(RuntimeException.class);

        doThrow(new RuntimeException("x")).when(s3Client).putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class), any(RequestBody.class));
        assertThatThrownBy(() -> s3FaceStorageProvider.uploadFaceImage(UUID.randomUUID(), new ByteArrayInputStream(new byte[]{1}), "image/jpeg"))
                .isInstanceOf(RuntimeException.class);

        s3FaceStorageProvider.deleteFaceImage("obj");
        doThrow(new RuntimeException("x")).when(s3Client).deleteObject(any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class));
        assertThatCode(() -> s3FaceStorageProvider.deleteFaceImage("obj2")).doesNotThrowAnyException();
    }

    @Test
    void rekognition_shouldCoverAllBranches() throws Exception {
        ReflectionTestUtils.setField(rekognitionProvider, "collectionId", "col");
        ReflectionTestUtils.setField(rekognitionProvider, "bucketName", "bucket");

        rekognitionProvider.ensureCollectionExists();
        doThrow(ResourceAlreadyExistsException.builder().message("exists").build()).when(rekognitionClient).createCollection(any(CreateCollectionRequest.class));
        rekognitionProvider.ensureCollectionExists();
        doThrow(new RuntimeException("boom")).when(rekognitionClient).createCollection(any(CreateCollectionRequest.class));
        assertThatThrownBy(() -> rekognitionProvider.ensureCollectionExists()).isInstanceOf(RuntimeException.class);

        IndexFacesResponse idxOk = IndexFacesResponse.builder().faceRecords(FaceRecord.builder().face(Face.builder().faceId("f1").build()).build()).build();
        when(rekognitionClient.indexFaces(any(IndexFacesRequest.class))).thenReturn(idxOk);
        assertThat(rekognitionProvider.indexFace("img", UUID.randomUUID())).isEqualTo("f1");
        when(rekognitionClient.indexFaces(any(IndexFacesRequest.class))).thenReturn(IndexFacesResponse.builder().faceRecords(List.of()).build());
        assertThat(rekognitionProvider.indexFace("img", UUID.randomUUID())).isNull();
        when(rekognitionClient.indexFaces(any(IndexFacesRequest.class))).thenThrow(new RuntimeException("x"));
        assertThatThrownBy(() -> rekognitionProvider.indexFace("img", UUID.randomUUID())).isInstanceOf(RuntimeException.class);

        UUID externalId = UUID.randomUUID();
        SearchFacesByImageResponse sfOk = SearchFacesByImageResponse.builder()
                .faceMatches(FaceMatch.builder().face(Face.builder().externalImageId(externalId.toString()).build()).build())
                .build();
        when(rekognitionClient.searchFacesByImage(any(SearchFacesByImageRequest.class))).thenReturn(sfOk);
        assertThat(rekognitionProvider.searchFaceByImage(new ByteArrayInputStream(new byte[]{1}))).isEqualTo(externalId);
        when(rekognitionClient.searchFacesByImage(any(SearchFacesByImageRequest.class))).thenReturn(SearchFacesByImageResponse.builder().faceMatches(List.of()).build());
        assertThat(rekognitionProvider.searchFaceByImage(new ByteArrayInputStream(new byte[]{1}))).isNull();
        assertThatThrownBy(() -> rekognitionProvider.searchFaceByImage(new InputStream() {
            @Override public int read() throws IOException { throw new IOException("io"); }
        })).isInstanceOf(RuntimeException.class);
        when(rekognitionClient.searchFacesByImage(any(SearchFacesByImageRequest.class))).thenThrow(SdkClientException.create("sdk"));
        assertThatThrownBy(() -> rekognitionProvider.searchFaceByImage(new ByteArrayInputStream(new byte[]{1}))).isInstanceOf(ServiceUnavailableException.class);
        when(rekognitionClient.searchFacesByImage(any(SearchFacesByImageRequest.class))).thenThrow(new RuntimeException("other"));
        assertThatThrownBy(() -> rekognitionProvider.searchFaceByImage(new ByteArrayInputStream(new byte[]{1}))).isInstanceOf(RuntimeException.class);

        rekognitionProvider.deleteFace("face");
        doThrow(new RuntimeException("x")).when(rekognitionClient).deleteFaces(any(DeleteFacesRequest.class));
        assertThatCode(() -> rekognitionProvider.deleteFace("face")).doesNotThrowAnyException();
    }

    @Test
    @SuppressWarnings({"unchecked","rawtypes"})
    void viaCep_shouldCoverSuccessAndErrors() {
        WebClient.Builder builder = mock(WebClient.Builder.class);
        WebClient wc = mock(WebClient.class);
        WebClient.RequestHeadersUriSpec uriSpec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.RequestHeadersSpec headersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.build()).thenReturn(wc);
        when(wc.get()).thenReturn(uriSpec);
        doReturn(headersSpec).when(uriSpec).uri(anyString(), any(Object[].class));
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        ViaCepClientImpl provider = new ViaCepClientImpl(builder);

        when(responseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.empty());
        assertThatThrownBy(() -> provider.lookup("01001000")).isInstanceOfAny(ResourceNotFoundException.class, InternalError.class);

        when(responseSpec.bodyToMono(any(Class.class))).thenThrow(WebClientResponseException.create(HttpStatus.NOT_FOUND.value(), "nf", null, null, null));
        assertThatThrownBy(() -> provider.lookup("01001000")).isInstanceOf(ResourceNotFoundException.class);

        when(responseSpec.bodyToMono(any(Class.class))).thenThrow(new RuntimeException("x"));
        assertThatThrownBy(() -> provider.lookup("01001000")).isInstanceOf(InternalError.class);
    }
}
