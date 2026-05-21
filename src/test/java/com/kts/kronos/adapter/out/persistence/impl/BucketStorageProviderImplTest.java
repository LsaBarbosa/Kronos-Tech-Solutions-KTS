package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

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
        String objectName = "doc123/UUID-file.pdf";
        String result = provider.uploadFile(objectName, payload, "application/pdf");

        Path expected = tempDir.resolve(Paths.get(result).normalize()).normalize();
        assertTrue(expected.startsWith(tempDir));
        assertTrue(Files.exists(expected));
        assertArrayEquals(payload, provider.downloadFile(result));
        assertEquals(objectName, result);
    }

    @Test
    @DisplayName("upload: rejeita paths inseguros com escape characters")
    void shouldRejectUnsafePaths() {
        byte[] payload = "conteudo".getBytes();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> provider.uploadFile("../../escape.pdf", payload, "application/pdf"));
        assertEquals("Caminho de storage inválido.", exception.getMessage());
    }

    @Test
    @DisplayName("download: falha quando arquivo não existe")
    void shouldFailWhenFileDoesNotExist() {
        assertThrows(ResourceNotFoundException.class, () -> provider.downloadFile("missing.pdf"));
    }

    @Test
    @DisplayName("delete: remove arquivo existente dentro da raiz")
    void shouldDeleteExistingFile() throws Exception {
        String objectName = provider.uploadFile("delete.pdf", "x".getBytes(), "application/pdf");

        provider.deleteFile(objectName);

        assertFalse(Files.exists(tempDir.resolve(objectName)));
    }

    @Test
    @DisplayName("upload: traduz erro de IO")
    void shouldTranslateUploadIOException() throws Exception {
        Path fileRoot = tempDir.resolve("not-a-directory");
        Files.writeString(fileRoot, "root");
        ReflectionTestUtils.setField(provider, "rootPath", fileRoot.toString());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> provider.uploadFile("file.pdf", "x".getBytes(), "application/pdf"));

        assertEquals("Falha ao salvar o arquivo no disco.", exception.getMessage());
    }

    @Test
    @DisplayName("upload: traduz rootPath inválido para erro seguro")
    void shouldTranslateInvalidRootPathOnUpload() {
        ReflectionTestUtils.setField(provider, "rootPath", "\0");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> provider.uploadFile("file.pdf", "x".getBytes(), "application/pdf"));

        assertEquals("Caminho de storage inválido.", exception.getMessage());
    }

    @Test
    @DisplayName("download: bloqueia leitura fora da raiz")
    void shouldBlockReadOutsideRoot() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> provider.downloadFile("../../etc/passwd"));
        assertEquals("Caminho de storage inválido.", exception.getMessage());
        assertFalse(exception.getMessage().contains(tempDir.toString()));
    }

    @Test
    @DisplayName("download: rejeita path final fora da raiz mesmo depois de normalizar")
    void shouldRejectFinalResolvedPathOutsideRoot() {
        ReflectionTestUtils.setField(provider, "rootPath", "root");
        Path rawRoot = mock(Path.class);
        Path root = mock(Path.class);
        Path rawRelative = mock(Path.class);
        Path relative = mock(Path.class);
        Path finalPath = mock(Path.class);

        try (var paths = mockStatic(Paths.class)) {
            paths.when(() -> Paths.get("root")).thenReturn(rawRoot);
            paths.when(() -> Paths.get("safe.pdf")).thenReturn(rawRelative);
            when(rawRoot.toAbsolutePath()).thenReturn(root);
            when(root.normalize()).thenReturn(root);
            when(rawRelative.normalize()).thenReturn(relative);
            when(relative.isAbsolute()).thenReturn(false);
            when(relative.startsWith("..")).thenReturn(false);
            when(root.resolve(relative)).thenReturn(finalPath);
            when(finalPath.normalize()).thenReturn(finalPath);
            when(finalPath.startsWith(root)).thenReturn(false);

            RuntimeException exception = assertThrows(RuntimeException.class, () -> provider.downloadFile("safe.pdf"));

            assertEquals("Caminho de storage inválido.", exception.getMessage());
        }
    }

    @Test
    @DisplayName("download: traduz erro de leitura do disco")
    void shouldTranslateDownloadIOException() throws Exception {
        Path directoryObject = tempDir.resolve("directory-object");
        Files.createDirectory(directoryObject);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> provider.downloadFile("directory-object"));

        assertEquals("Falha ao ler o arquivo do disco.", exception.getMessage());
    }

    @Test
    @DisplayName("delete: bloqueia deleção fora da raiz")
    void shouldBlockDeleteOutsideRoot() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> provider.deleteFile("../outside.txt"));
        assertEquals("Caminho de storage inválido.", exception.getMessage());
        assertFalse(exception.getMessage().contains(tempDir.toString()));
    }

    @Test
    @DisplayName("delete: traduz erro de IO")
    void shouldTranslateDeleteIOException() throws Exception {
        Path directoryObject = tempDir.resolve("non-empty-directory");
        Files.createDirectory(directoryObject);
        Files.writeString(directoryObject.resolve("child.txt"), "x");

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> provider.deleteFile("non-empty-directory"));

        assertEquals("Falha ao excluir o arquivo do disco.", exception.getMessage());
    }

    @Test
    @DisplayName("storage: rejeita nomes nulos ou em branco")
    void shouldRejectNullOrBlankObjectNames() {
        RuntimeException blankDownload = assertThrows(RuntimeException.class, () -> provider.downloadFile(" "));
        RuntimeException nullDelete = assertThrows(RuntimeException.class, () -> provider.deleteFile(null));

        assertEquals("Caminho de storage inválido.", blankDownload.getMessage());
        assertEquals("Caminho de storage inválido.", nullDelete.getMessage());
    }
}
