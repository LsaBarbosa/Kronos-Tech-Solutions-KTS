package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.exceptions.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.kts.kronos.constants.Messages.FILE_SCAN_FAILED;
import static com.kts.kronos.constants.Messages.MALICIOUS_FILE_DETECTED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileScanningProviderImplTest {

    @Test
    @DisplayName("scanOrThrow: ignora varredura quando antivírus está desabilitado")
    void shouldSkipScanWhenDisabled() {
        var provider = new FileScanningProviderImpl();
        ReflectionTestUtils.setField(provider, "enabled", false);

        assertDoesNotThrow(() -> provider.scanOrThrow("doc.pdf", "application/pdf", "pdf".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    @DisplayName("scanOrThrow: aceita arquivo limpo")
    void shouldAcceptCleanFile() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            startFakeClamAv(serverSocket, "stream: OK");

            var provider = configuredProvider(serverSocket.getLocalPort());

            assertDoesNotThrow(() -> provider.scanOrThrow(
                    "doc.pdf",
                    "application/pdf",
                    "pdf".getBytes(StandardCharsets.US_ASCII)
            ));
        }
    }

    @Test
    @DisplayName("scanOrThrow: deve rejeitar arquivo infectado")
    void shouldRejectInfectedFile() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            startFakeClamAv(serverSocket, "stream: Eicar-Test-Signature FOUND");

            var provider = configuredProvider(serverSocket.getLocalPort());
            BadRequestException exception = assertThrows(
                    BadRequestException.class,
                    () -> provider.scanOrThrow("doc.pdf", "application/pdf", "pdf".getBytes(StandardCharsets.US_ASCII))
            );

            assertEquals(MALICIOUS_FILE_DETECTED, exception.getMessage());
        }
    }

    @Test
    @DisplayName("scanOrThrow: deve falhar quando antivírus está indisponível")
    void shouldFailWhenAntivirusIsUnavailable() throws Exception {
        int unusedPort;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            unusedPort = serverSocket.getLocalPort();
        }

        var provider = configuredProvider(unusedPort);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> provider.scanOrThrow("doc.pdf", "application/pdf", "pdf".getBytes(StandardCharsets.US_ASCII))
        );

        assertEquals(FILE_SCAN_FAILED, exception.getMessage());
    }

    @Test
    @DisplayName("scanOrThrow: deve falhar quando antivírus retorna resposta inesperada")
    void shouldFailWhenAntivirusReturnsUnexpectedResponse() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            startFakeClamAv(serverSocket, "stream: ERROR");

            var provider = configuredProvider(serverSocket.getLocalPort());
            RuntimeException exception = assertThrows(
                    RuntimeException.class,
                    () -> provider.scanOrThrow("doc.pdf", "application/pdf", "pdf".getBytes(StandardCharsets.US_ASCII))
            );

            assertEquals(FILE_SCAN_FAILED, exception.getMessage());
        }
    }

    private FileScanningProviderImpl configuredProvider(int port) {
        var provider = new FileScanningProviderImpl();
        ReflectionTestUtils.setField(provider, "enabled", true);
        ReflectionTestUtils.setField(provider, "host", "127.0.0.1");
        ReflectionTestUtils.setField(provider, "port", port);
        ReflectionTestUtils.setField(provider, "timeoutMs", 1000);
        return provider;
    }

    private void startFakeClamAv(ServerSocket serverSocket, String response) throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Thread serverThread = new Thread(() -> {
            try (Socket socket = serverSocket.accept();
                 DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                 var output = new BufferedOutputStream(socket.getOutputStream())) {
                ready.countDown();
                readCommand(input);
                readStreamPayload(input);
                output.write(response.getBytes(StandardCharsets.US_ASCII));
                output.flush();
            } catch (Exception ignored) {
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        ready.await(1, TimeUnit.SECONDS);
    }

    private void readCommand(DataInputStream input) throws Exception {
        while (input.readByte() != 0) {
            // descarta "zINSTREAM"
        }
    }

    private void readStreamPayload(DataInputStream input) throws Exception {
        while (true) {
            int length = input.readInt();
            if (length == 0) {
                return;
            }
            input.readNBytes(length);
        }
    }
}
