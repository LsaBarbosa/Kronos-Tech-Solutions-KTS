package com.kts.kronos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsClientConfig {

    @Value("${aws.region}")
    private String awsRegion;

    // 1. Configura o S3 Client (usado para salvar as imagens de rosto)
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(awsRegion))
                // Usa as variáveis de ambiente do Render para autenticação
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }

    // 2. Configura o Rekognition Client (usado para indexar e buscar faces)
    @Bean
    public RekognitionClient rekognitionClient() {
        return RekognitionClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .build();
    }
}
