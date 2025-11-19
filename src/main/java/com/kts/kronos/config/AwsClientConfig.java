package com.kts.kronos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials; // Adicione esta importação
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider; // Adicione esta importação
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider; // Adicione esta importação
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.s3.S3Client;
@Configuration
public class AwsClientConfig {

    @Value("${aws.region}")
    private String awsRegion;

    // 1. INJETA AS CHAVES DO YAML
    @Value("${aws.access-key-id}")
    private String accessKeyId;
    @Value("${aws.secret-access-key}")
    private String secretAccessKey;
    // FIM DA INJEÇÃO

    // NOVO BEAN: Cria um provedor de credenciais estáticas que lê do YAML
    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
        return StaticCredentialsProvider.create(credentials);
    }


    // 1. Configura o S3 Client (usado para salvar as imagens de rosto)
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(awsRegion))
                // 2. UTILIZA O NOVO BEAN
                .credentialsProvider(awsCredentialsProvider())
                .build();
    }

    // 2. Configura o Rekognition Client (usado para indexar e buscar faces)
    @Bean
    public RekognitionClient rekognitionClient() {
        return RekognitionClient.builder()
                .region(Region.of(awsRegion))
                // 3. UTILIZA O NOVO BEAN
                .credentialsProvider(awsCredentialsProvider())
                .build();
    }
}