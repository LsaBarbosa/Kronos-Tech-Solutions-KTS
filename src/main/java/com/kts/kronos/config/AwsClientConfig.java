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

    @Value("${aws.access-key-id:}")
    private String accessKeyId;
    @Value("${aws.secret-access-key:}")
    private String secretAccessKey;

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider() {
        if ((accessKeyId != null && !accessKeyId.isEmpty()) &&
            (secretAccessKey != null && !secretAccessKey.isEmpty())) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)
            );
        }
        return null;
    }


    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder().region(Region.of(awsRegion));
        var credentialsProvider = awsCredentialsProvider();
        if (credentialsProvider != null) {
            builder.credentialsProvider(credentialsProvider);
        }
        return builder.build();
    }

    @Bean
    public RekognitionClient rekognitionClient() {
        var builder = RekognitionClient.builder().region(Region.of(awsRegion));
        var credentialsProvider = awsCredentialsProvider();
        if (credentialsProvider != null) {
            builder.credentialsProvider(credentialsProvider);
        }
        return builder.build();
    }
}