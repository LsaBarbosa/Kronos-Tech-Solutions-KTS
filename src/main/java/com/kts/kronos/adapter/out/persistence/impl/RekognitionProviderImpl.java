package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.*;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RekognitionProviderImpl  implements FaceRecognitionProvider {

    private final RekognitionClient rekognitionClient;
    private final FaceStorageProvider faceStorageProvider;

    @Value("${aws.rekognition.collection-id}")
    private String collectionId;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    private static final float FACE_MATCH_THRESHOLD = 90.0f; // Limite de 90% para o Free Tier

    @Override
    public void ensureCollectionExists() {
        try {
            rekognitionClient.createCollection(CreateCollectionRequest.builder()
                    .collectionId(collectionId)
                    .build());
        } catch (ResourceAlreadyExistsException e) {
            // Se já existe, é o comportamento esperado no startup.
        } catch (Exception e) {
            log.error(" Erro fatal ao tentar criar ou verificar coleção '{}': {}", collectionId, e.getMessage(), e);
            throw new RuntimeException("Falha na inicialização do serviço Rekognition.", e);
        }
    }

    @Override
    public String indexFace(String imageS3Key, UUID externalImageId) {
        try {
            Image image = Image.builder()
                    .s3Object(S3Object.builder()
                            .bucket(bucketName)
                            .name(imageS3Key)
                            .build())
                    .build();

            IndexFacesRequest indexFacesRequest = IndexFacesRequest.builder()
                    .collectionId(collectionId)
                    .image(image)
                    .externalImageId(externalImageId.toString())
                    .detectionAttributes(Attribute.DEFAULT)
                    .build();

            IndexFacesResponse response = rekognitionClient.indexFaces(indexFacesRequest);

            if (response.faceRecords().isEmpty()) {
                log.warn("Nenhuma face detectada na imagem S3 Key: {}", imageS3Key);
                return null;
            }

            return response.faceRecords().get(0).face().faceId();

        } catch (Exception e) {
            log.error("Erro ao indexar face do funcionário {}: {}", externalImageId, e.getMessage(), e);
            throw new RuntimeException("Falha ao registrar face no Rekognition.", e);
        }
    }

    @Override
    public UUID searchFaceByImage(InputStream imageStream) {
        try {
            byte[] imageBytes = imageStream.readAllBytes();
            SdkBytes sourceImageBytes = SdkBytes.fromByteArray(imageBytes);

            Image image = Image.builder().bytes(sourceImageBytes).build();

            SearchFacesByImageRequest searchRequest = SearchFacesByImageRequest.builder()
                    .collectionId(collectionId)
                    .image(image)
                    .faceMatchThreshold(FACE_MATCH_THRESHOLD)
                    .maxFaces(1)
                    .build();

            SearchFacesByImageResponse searchResponse = rekognitionClient.searchFacesByImage(searchRequest);

            if (searchResponse.faceMatches().isEmpty()) {
                return null;
            }

            FaceMatch match = searchResponse.faceMatches().get(0);
            String externalIdStr = match.face().externalImageId();
            return UUID.fromString(externalIdStr);

        } catch (IOException e) {
            log.error("Erro ao ler o stream da imagem para busca: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao ler a imagem para reconhecimento.", e);
        } catch (Exception e) {
            log.error("Erro ao buscar face na coleção Rekognition: {}", e.getMessage(), e);
            throw new RuntimeException("Falha no serviço de reconhecimento facial.", e);
        }
    }

    @Override
    public void deleteFace(String faceId) {
        try {
            DeleteFacesRequest deleteFacesRequest = DeleteFacesRequest.builder()
                    .collectionId(collectionId)
                    .faceIds(faceId)
                    .build();
            rekognitionClient.deleteFaces(deleteFacesRequest);
        } catch (Exception e) {
            log.error("Erro ao deletar face {}: {}", faceId, e.getMessage(), e);
        }
    }
}