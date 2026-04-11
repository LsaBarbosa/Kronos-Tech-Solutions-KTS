package com.kts.kronos.adapter.out.persistence.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class BucketStorageProviderImplTest {

    @TempDir
    Path tempDir;

    private BucketStorageProviderImpl provider;

    @BeforeEach
    void setUp() {
        provider = new BucketStorageProviderImpl();
        ReflectionTestUtils.setField(provider, "rootPath", tempDir.toString());
    }

    @Test
    @DisplayName("upload/download: mantém arquivos dentro da raiz configurada")
    void shouldKeepWrittenFileInsideRoot() throws Exception {
        byte[] payload = "conteudo".getBytes();
        String objectName = provider.uploadFile("../../escape.pdf", payload, "application/pdf");

        Path expected = tempDir.resolve(Paths.get(objectName).normalize()).normalize();
        assertTrue(expected.startsWith(tempDir));
        assertTrue(Files.exists(expected));
        assertArrayEquals(payload, provider.downloadFile(objectName));
    }

    @Test
    @DisplayName("download: bloqueia leitura fora da raiz")
    void shouldBlockReadOutsideRoot() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> provider.downloadFile("../../etc/passwd"));
        assertEquals("Caminho de storage inválido.", exception.getMessage());
        assertFalse(exception.getMessage().contains(tempDir.toString()));
    }

    @Test
    @DisplayName("delete: bloqueia deleção fora da raiz")
    void shouldBlockDeleteOutsideRoot() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> provider.deleteFile("../outside.txt"));
        assertEquals("Caminho de storage inválido.", exception.getMessage());
        assertFalse(exception.getMessage().contains(tempDir.toString()));
    }
}
