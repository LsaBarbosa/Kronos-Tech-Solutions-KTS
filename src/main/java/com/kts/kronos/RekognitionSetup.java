package com.kts.kronos;


import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RekognitionSetup {

    private final FaceRecognitionProvider faceRecognitionProvider;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @PostConstruct
    public void initializeRekognitionCollection() {
        if ("test".equals(activeProfile)) {
            log.debug("Skipping Rekognition initialization in test profile");
            return;
        }

        try {
            log.info("Iniciando a verificação da coleção Rekognition...");
            faceRecognitionProvider.ensureCollectionExists();
            log.info("Coleção Rekognition verificada/criada com sucesso.");
        } catch (Exception e) {
            log.warn("Aviso não-crítico ao tentar inicializar Rekognition (pode ser um ambiente de teste): {}", e.getMessage());
        }
    }
}