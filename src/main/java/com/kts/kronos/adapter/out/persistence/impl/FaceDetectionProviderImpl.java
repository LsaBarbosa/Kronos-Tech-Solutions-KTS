package com.kts.kronos.adapter.out.persistence.impl;


import ai.djl.Application;
import ai.djl.inference.Predictor;
import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.ImageFactory;
import ai.djl.modality.cv.output.DetectedObjects;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.port.out.provider.FaceDetectionProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
@Component
public class FaceDetectionProviderImpl implements FaceDetectionProvider {

    private ZooModel<Image, DetectedObjects> model;
    private Predictor<Image, DetectedObjects> predictor;
    private boolean isModelLoaded = false;

    // Define um limite de confiança mínimo para a detecção ser considerada válida
    private static final double MIN_CONFIDENCE = 0.8;

    @PostConstruct
    public void init() {
        try {
            // AJUSTE CRUCIAL: Substituindo FACE_DETECTION por OBJECT_DETECTION
            // para ser compatível com a sua classe Application.java
            Criteria<Image, DetectedObjects> criteria = Criteria.builder()
                    .optApplication(Application.CV.OBJECT_DETECTION) // A detecção de face é um tipo de Object Detection
                    .setTypes(Image.class, DetectedObjects.class)
                    // Filtramos pelo modelo de backbone e engine que tipicamente suporta face detection (RetinaFace/PyTorch)
                    .optFilter("backbone", "resnet50")
                    .optEngine("PyTorch")
                    .build();

            model = criteria.loadModel();
            predictor = model.newPredictor();
            isModelLoaded = true;
            log.info("DJL Face Detection Model (PyTorch/RetinaFace) carregado com sucesso sob OBJECT_DETECTION.");

        } catch (Exception e) {
            log.error("ERRO ao inicializar o DJL Face Detection. A validação facial será desabilitada. Motivo: {}", e.getMessage());
            isModelLoaded = false;
        }
    }

    @PreDestroy
    public void close() {
        if (predictor != null) {
            predictor.close();
        }
        if (model != null) {
            model.close();
        }
    }

    @Override
    public boolean detectFace(byte[] imageBytes) throws IOException {
        if (!isModelLoaded) {
            log.warn("Validação facial desabilitada ou com erro de inicialização. Permitindo check-in (DJL).");
            return true;
        }

        Image image = ImageFactory.getInstance().fromInputStream(new ByteArrayInputStream(imageBytes));

        try {
            DetectedObjects detections = predictor.predict(image);

            long faceCount = detections.items().stream()
                    .filter(box -> box.getProbability() >= MIN_CONFIDENCE)
                    .count();

            if (faceCount == 1) {
                log.info("Detecção facial bem-sucedida: 1 face encontrada com confiança > {}", MIN_CONFIDENCE);
                return true;
            }

            log.warn("Detecção facial falhou: {} faces encontradas (Esperado: 1).", faceCount);
            throw new BadRequestException("Validação facial falhou. Certifique-se de que exatamente uma face esteja visível e clara na imagem.");

        } catch (TranslateException e) {
            log.error("Erro durante a inferência do DJL: {}", e.getMessage());
            throw new BadRequestException("Falha ao executar o modelo de detecção facial.");
        }
    }
}