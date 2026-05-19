package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.port.out.provider.FileScanningProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static com.kts.kronos.constants.Messages.FILE_SCAN_FAILED;
import static com.kts.kronos.constants.Messages.MALICIOUS_FILE_DETECTED;

@Slf4j
@Service
public class FileScanningProviderImpl implements FileScanningProvider {

    private static final int CHUNK_SIZE = 8192;

    @Value("${kronos.security.upload.antivirus.enabled:false}")
    private boolean enabled;

    @Value("${kronos.security.upload.antivirus.host:localhost}")
    private String host;

    @Value("${kronos.security.upload.antivirus.port:3310}")
    private int port;

    @Value("${kronos.security.upload.antivirus.timeout-ms:3000}")
    private int timeoutMs;

    @Override
    public void scanOrThrow(String fileName, String contentType, byte[] content) {
        if (!enabled) {
            log.debug("event=document_upload_scan result=skipped reason=disabled");
            return;
        }

        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            socket.setSoTimeout(timeoutMs);

            try (var output = new BufferedOutputStream(socket.getOutputStream());
                 var input = new BufferedInputStream(socket.getInputStream())) {

                output.write("zINSTREAM\0".getBytes(StandardCharsets.US_ASCII));

                int offset = 0;
                while (offset < content.length) {
                    int length = Math.min(CHUNK_SIZE, content.length - offset);
                    output.write(ByteBuffer.allocate(Integer.BYTES).putInt(length).array());
                    output.write(content, offset, length);
                    offset += length;
                }

                output.write(new byte[]{0, 0, 0, 0});
                output.flush();

                String response = new String(input.readNBytes(512), StandardCharsets.US_ASCII).trim();

                if (response.contains("FOUND")) {
                    log.warn("event=document_upload_scan result=failure reason=malicious_file");
                    throw new BadRequestException(MALICIOUS_FILE_DETECTED);
                }

                if (!response.contains("OK")) {
                    log.error("event=document_upload_scan result=failure reason=unexpected_response");
                    throw new RuntimeException(FILE_SCAN_FAILED);
                }
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            log.error("event=document_upload_scan result=failure reason=io exception_type={}",
                    e.getClass().getSimpleName());
            throw new RuntimeException(FILE_SCAN_FAILED, e);
        }
    }
}
