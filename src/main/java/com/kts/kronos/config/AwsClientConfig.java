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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials; // Adicione esta importação
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider; // Adicione esta importação
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider; // Adicione esta importação
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsClientConfig {

    @Value("${aws.region}")
    private String awsRegion;
    @Value("${aws.access-key-id:}")
    private String accessKeyId;
    @Value("${aws.secret-access-key:}")
    private String secretAccessKey;

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        if (accessKeyId != null && !accessKeyId.isBlank() && secretAccessKey != null && !secretAccessKey.isBlank()) {
            var credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey);
            return StaticCredentialsProvider.create(credentials);
        }

        return DefaultCredentialsProvider.create();
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider awsCredentialsProvider) {
        return S3Client.builder()
                .region(Region.of(awsRegion))
                // 2. UTILIZA O NOVO BEAN
                .credentialsProvider(awsCredentialsProvider)
                .build();
    }

    // 2. Configura o Rekognition Client (usado para indexar e buscar faces)
    @Bean
    public RekognitionClient rekognitionClient(AwsCredentialsProvider awsCredentialsProvider) {
        return RekognitionClient.builder()
                .region(Region.of(awsRegion))
                // 3. UTILIZA O NOVO BEAN
                .credentialsProvider(awsCredentialsProvider)
                .build();
    }
}