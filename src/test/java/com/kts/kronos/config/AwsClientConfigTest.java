package com.kts.kronos.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AwsClientConfigTest {

    @Test
    @DisplayName("awsCredentialsProvider: deve usar credenciais configuradas")
    void shouldCreateStaticCredentialsProvider() {
        AwsClientConfig config = configuredAwsClientConfig();

        AwsCredentialsProvider provider = config.awsCredentialsProvider();
        var credentials = provider.resolveCredentials();

        assertEquals("test-access-key", credentials.accessKeyId());
        assertEquals("test-secret-key", credentials.secretAccessKey());
    }

    @Test
    @DisplayName("s3Client e rekognitionClient: devem criar clientes com configuracao local")
    void shouldCreateAwsClients() {
        AwsClientConfig config = configuredAwsClientConfig();

        try (S3Client s3Client = config.s3Client();
             RekognitionClient rekognitionClient = config.rekognitionClient()) {
            assertNotNull(s3Client);
            assertNotNull(rekognitionClient);
        }
    }

    private static AwsClientConfig configuredAwsClientConfig() {
        AwsClientConfig config = new AwsClientConfig();
        ReflectionTestUtils.setField(config, "awsRegion", "us-east-1");
        ReflectionTestUtils.setField(config, "accessKeyId", "test-access-key");
        ReflectionTestUtils.setField(config, "secretAccessKey", "test-secret-key");
        return config;
    }
}
