package com.kts.kronos;


import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RekognitionSetup {

    private final FaceRecognitionProvider faceRecognitionProvider; // Injetado abaixo

    /**
     * Executa após a construção do bean para garantir que a coleção exista.
     */
    @PostConstruct
    public void initializeRekognitionCollection() {
        log.info("Iniciando a verificação da coleção Rekognition...");
        faceRecognitionProvider.ensureCollectionExists();
        log.info("Coleção Rekognition verificada/criada com sucesso.");
    }
}