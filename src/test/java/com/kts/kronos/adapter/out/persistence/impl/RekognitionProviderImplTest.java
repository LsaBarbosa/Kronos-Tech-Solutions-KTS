package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RekognitionProviderImplTest {

    @Mock
    private RekognitionClient rekognitionClient;

    @Mock
    private FaceStorageProvider faceStorageProvider;

    @InjectMocks
    private RekognitionProviderImpl provider;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(provider, "collectionId", "collection-test");
        ReflectionTestUtils.setField(provider, "bucketName", "bucket-test");
    }

    @Test
    @DisplayName("ensureCollectionExists: deve criar coleção com sucesso")
    void shouldEnsureCollectionExistsSuccessfully() {
        when(rekognitionClient.createCollection(any(CreateCollectionRequest.class)))
                .thenReturn(CreateCollectionResponse.builder().statusCode(200).build());

        assertDoesNotThrow(() -> provider.ensureCollectionExists());

        ArgumentCaptor<CreateCollectionRequest> captor = ArgumentCaptor.forClass(CreateCollectionRequest.class);
        verify(rekognitionClient).createCollection(captor.capture());
        assertEquals("collection-test", captor.getValue().collectionId());
    }

    @Test
    @DisplayName("ensureCollectionExists: deve absorver ResourceAlreadyExistsException")
    void shouldIgnoreResourceAlreadyExistsException() {
        when(rekognitionClient.createCollection(any(CreateCollectionRequest.class)))
                .thenThrow(ResourceAlreadyExistsException.builder().message("exists").build());

        assertDoesNotThrow(() -> provider.ensureCollectionExists());
        verify(rekognitionClient).createCollection(any(CreateCollectionRequest.class));
    }

    @Test
    @DisplayName("ensureCollectionExists: deve falhar com erro genérico do SDK")
    void shouldFailWhenEnsureCollectionExistsHasGenericSdkFailure() {
        when(rekognitionClient.createCollection(any(CreateCollectionRequest.class)))
                .thenThrow(RekognitionException.builder().message("boom").build());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> provider.ensureCollectionExists());
        assertEquals("Falha na inicialização do serviço Rekognition.", ex.getMessage());
    }

    @Test
    @DisplayName("indexFace: deve retornar faceId quando houver face detectada")
    void shouldIndexFaceWhenFaceIsDetected() {
        UUID externalImageId = UUID.randomUUID();

        IndexFacesResponse response = IndexFacesResponse.builder()
                .faceRecords(List.of(
                        FaceRecord.builder()
                                .face(Face.builder().faceId("face-123").build())
                                .build()
                ))
                .build();

        when(rekognitionClient.indexFaces(any(IndexFacesRequest.class))).thenReturn(response);

        String result = provider.indexFace("faces/file.jpg", externalImageId);

        ArgumentCaptor<IndexFacesRequest> captor = ArgumentCaptor.forClass(IndexFacesRequest.class);
        verify(rekognitionClient).indexFaces(captor.capture());

        IndexFacesRequest request = captor.getValue();
        assertEquals("collection-test", request.collectionId());
        assertEquals(externalImageId.toString(), request.externalImageId());
        assertEquals("bucket-test", request.image().s3Object().bucket());
        assertEquals("faces/file.jpg", request.image().s3Object().name());
        assertEquals("face-123", result);
    }

    @Test
    @DisplayName("indexFace: deve retornar null quando nenhuma face for detectada")
    void shouldReturnNullWhenNoFaceIsDetected() {
        when(rekognitionClient.indexFaces(any(IndexFacesRequest.class)))
                .thenReturn(IndexFacesResponse.builder().faceRecords(List.of()).build());

        String result = provider.indexFace("faces/file.jpg", UUID.randomUUID());

        assertNull(result);
        verify(rekognitionClient).indexFaces(any(IndexFacesRequest.class));
    }

    @Test
    @DisplayName("indexFace: deve lançar exceção em falha do SDK")
    void shouldFailWhenIndexFaceHasSdkFailure() {
        when(rekognitionClient.indexFaces(any(IndexFacesRequest.class)))
                .thenThrow(RekognitionException.builder().message("boom").build());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> provider.indexFace("faces/file.jpg", UUID.randomUUID()));

        assertEquals("Falha ao registrar face no Rekognition.", ex.getMessage());
    }

    @Test
    @DisplayName("searchFaceByImage: deve retornar employeeId quando houver match")
    void shouldReturnEmployeeIdWhenSearchFindsMatch() {
        UUID employeeId = UUID.randomUUID();

        SearchFacesByImageResponse response = SearchFacesByImageResponse.builder()
                .faceMatches(List.of(
                        FaceMatch.builder()
                                .face(Face.builder().externalImageId(employeeId.toString()).build())
                                .build()
                ))
                .build();

        when(rekognitionClient.searchFacesByImage(any(SearchFacesByImageRequest.class))).thenReturn(response);

        UUID result = provider.searchFaceByImage(InputStream.nullInputStream());

        ArgumentCaptor<SearchFacesByImageRequest> captor =
                ArgumentCaptor.forClass(SearchFacesByImageRequest.class);
        verify(rekognitionClient).searchFacesByImage(captor.capture());

        SearchFacesByImageRequest request = captor.getValue();
        assertEquals("collection-test", request.collectionId());
        assertEquals(90.0f, request.faceMatchThreshold());
        assertEquals(1, request.maxFaces());
        assertEquals(employeeId, result);
    }

    @Test
    @DisplayName("searchFaceByImage: deve retornar null quando não houver match")
    void shouldReturnNullWhenSearchFindsNoMatch() {
        when(rekognitionClient.searchFacesByImage(any(SearchFacesByImageRequest.class)))
                .thenReturn(SearchFacesByImageResponse.builder().faceMatches(List.of()).build());

        UUID result = provider.searchFaceByImage(InputStream.nullInputStream());

        assertNull(result);
        verify(rekognitionClient).searchFacesByImage(any(SearchFacesByImageRequest.class));
    }

    @Test
    @DisplayName("searchFaceByImage: deve lançar exceção quando houver IOException")
    void shouldFailWhenSearchImageStreamCannotBeRead() {
        InputStream brokenStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("read failure");
            }
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> provider.searchFaceByImage(brokenStream));

        assertEquals("Falha ao ler a imagem para reconhecimento.", ex.getMessage());
    }

    @Test
    @DisplayName("searchFaceByImage: deve lançar exceção em falha genérica do SDK")
    void shouldFailWhenSearchHasGenericSdkFailure() {
        when(rekognitionClient.searchFacesByImage(any(SearchFacesByImageRequest.class)))
                .thenThrow(RekognitionException.builder().message("boom").build());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> provider.searchFaceByImage(InputStream.nullInputStream()));

        assertEquals("Falha no serviço de reconhecimento facial.", ex.getMessage());
    }

    @Test
    @DisplayName("searchFaceByImage: deve falhar quando ExternalImageId não é UUID")
    void shouldFailWhenSearchReturnsInvalidExternalImageId() {
        SearchFacesByImageResponse response = SearchFacesByImageResponse.builder()
                .faceMatches(List.of(
                        FaceMatch.builder()
                                .face(Face.builder().externalImageId("not-a-uuid").build())
                                .build()
                ))
                .build();

        when(rekognitionClient.searchFacesByImage(any(SearchFacesByImageRequest.class))).thenReturn(response);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> provider.searchFaceByImage(InputStream.nullInputStream()));

        assertEquals("Falha no serviço de reconhecimento facial.", ex.getMessage());
    }

    @Test
    @DisplayName("deleteFace: deve remover face com sucesso")
    void shouldDeleteFaceSuccessfully() {
        when(rekognitionClient.deleteFaces(any(DeleteFacesRequest.class)))
                .thenReturn(DeleteFacesResponse.builder().build());

        assertDoesNotThrow(() -> provider.deleteFace("face-123"));

        ArgumentCaptor<DeleteFacesRequest> captor = ArgumentCaptor.forClass(DeleteFacesRequest.class);
        verify(rekognitionClient).deleteFaces(captor.capture());

        DeleteFacesRequest request = captor.getValue();
        assertEquals("collection-test", request.collectionId());
        assertEquals(List.of("face-123"), request.faceIds());
    }

    @Test
    @DisplayName("deleteFace: deve absorver exceção do SDK")
    void shouldAbsorbDeleteFaceException() {
        when(rekognitionClient.deleteFaces(any(DeleteFacesRequest.class)))
                .thenThrow(RekognitionException.builder().message("boom").build());

        assertDoesNotThrow(() -> provider.deleteFace("face-123"));
        verify(rekognitionClient).deleteFaces(any(DeleteFacesRequest.class));
    }

    @Test
    @DisplayName("deleteFacesByExternalImageId: retorna sem deletar quando não há faces")
    void shouldReturnWhenNoFacesMatchExternalImageId() {
        UUID employeeId = UUID.randomUUID();
        when(rekognitionClient.listFaces(any(ListFacesRequest.class)))
                .thenReturn(ListFacesResponse.builder()
                        .faces(Face.builder().faceId("other").externalImageId(UUID.randomUUID().toString()).build())
                        .build());

        assertDoesNotThrow(() -> provider.deleteFacesByExternalImageId(employeeId));

        verify(rekognitionClient, never()).deleteFaces(any(DeleteFacesRequest.class));
    }

    @Test
    @DisplayName("deleteFacesByExternalImageId: pagina e remove faces do colaborador")
    void shouldDeleteFacesByExternalImageIdAcrossPages() {
        UUID employeeId = UUID.randomUUID();
        ListFacesResponse firstPage = ListFacesResponse.builder()
                .faces(
                        Face.builder().faceId("face-1").externalImageId(employeeId.toString()).build(),
                        Face.builder().faceId("other").externalImageId(UUID.randomUUID().toString()).build()
                )
                .nextToken("next")
                .build();
        ListFacesResponse secondPage = ListFacesResponse.builder()
                .faces(Face.builder().faceId("face-2").externalImageId(employeeId.toString()).build())
                .build();

        when(rekognitionClient.listFaces(any(ListFacesRequest.class))).thenReturn(firstPage, secondPage);
        when(rekognitionClient.deleteFaces(any(DeleteFacesRequest.class))).thenReturn(DeleteFacesResponse.builder().build());

        provider.deleteFacesByExternalImageId(employeeId);

        ArgumentCaptor<DeleteFacesRequest> captor = ArgumentCaptor.forClass(DeleteFacesRequest.class);
        verify(rekognitionClient).deleteFaces(captor.capture());
        assertEquals(List.of("face-1", "face-2"), captor.getValue().faceIds());
    }

    @Test
    @DisplayName("deleteFacesByExternalImageId: absorve falha do SDK")
    void shouldAbsorbDeleteFacesByExternalImageIdSdkFailure() {
        when(rekognitionClient.listFaces(any(ListFacesRequest.class)))
                .thenThrow(RekognitionException.builder().message("boom").build());

        assertDoesNotThrow(() -> provider.deleteFacesByExternalImageId(UUID.randomUUID()));
    }
}
