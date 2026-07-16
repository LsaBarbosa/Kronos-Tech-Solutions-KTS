package com.kts.kronos.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.*;

class AwsClientConfigCoverageTest {

    // ── L25: accessKeyId=null → (accessKeyId != null && ...) = FALSE ─────────────

    @Test
    void awsCredentialsProvider_withNullAccessKeyId_returnsNull() {
        AwsClientConfig config = new AwsClientConfig();
        ReflectionTestUtils.setField(config, "awsRegion", "us-east-1");
        ReflectionTestUtils.setField(config, "accessKeyId", null);
        ReflectionTestUtils.setField(config, "secretAccessKey", "secret");

        AwsCredentialsProvider provider = config.awsCredentialsProvider();
        assertNull(provider);
    }

    // ── L25: accessKeyId="" → isEmpty=TRUE → condition FALSE ─────────────────────

    @Test
    void awsCredentialsProvider_withEmptyAccessKeyId_returnsNull() {
        AwsClientConfig config = new AwsClientConfig();
        ReflectionTestUtils.setField(config, "awsRegion", "us-east-1");
        ReflectionTestUtils.setField(config, "accessKeyId", "");
        ReflectionTestUtils.setField(config, "secretAccessKey", "secret");

        AwsCredentialsProvider provider = config.awsCredentialsProvider();
        assertNull(provider);
    }

    // ── L25: secretAccessKey=null → (secretAccessKey != null && ...) = FALSE ─────
    // Covers branch: accessKeyId valid but secretAccessKey is null

    @Test
    void awsCredentialsProvider_withNullSecretKey_returnsNull() {
        AwsClientConfig config = new AwsClientConfig();
        ReflectionTestUtils.setField(config, "awsRegion", "us-east-1");
        ReflectionTestUtils.setField(config, "accessKeyId", "valid-key");
        ReflectionTestUtils.setField(config, "secretAccessKey", null);

        AwsCredentialsProvider provider = config.awsCredentialsProvider();
        assertNull(provider);
    }

    // ── L26: secretAccessKey="" → isEmpty=TRUE → condition FALSE ─────────────────

    @Test
    void awsCredentialsProvider_withEmptySecretKey_returnsNull() {
        AwsClientConfig config = new AwsClientConfig();
        ReflectionTestUtils.setField(config, "awsRegion", "us-east-1");
        ReflectionTestUtils.setField(config, "accessKeyId", "access-key");
        ReflectionTestUtils.setField(config, "secretAccessKey", "");

        AwsCredentialsProvider provider = config.awsCredentialsProvider();
        assertNull(provider);
    }

    // ── L39: credentialsProvider=null → s3Client without explicit provider ───────

    @Test
    void s3Client_withNullCredentials_buildsWithoutExplicitProvider() {
        AwsClientConfig config = new AwsClientConfig();
        ReflectionTestUtils.setField(config, "awsRegion", "us-east-1");
        ReflectionTestUtils.setField(config, "accessKeyId", "");
        ReflectionTestUtils.setField(config, "secretAccessKey", "");

        try (S3Client client = config.s3Client()) {
            assertNotNull(client);
        }
    }

    // ── L49: credentialsProvider=null → rekognitionClient without explicit provider ─

    @Test
    void rekognitionClient_withNullCredentials_buildsWithoutExplicitProvider() {
        AwsClientConfig config = new AwsClientConfig();
        ReflectionTestUtils.setField(config, "awsRegion", "us-east-1");
        ReflectionTestUtils.setField(config, "accessKeyId", "");
        ReflectionTestUtils.setField(config, "secretAccessKey", "");

        try (RekognitionClient client = config.rekognitionClient()) {
            assertNotNull(client);
        }
    }
}
