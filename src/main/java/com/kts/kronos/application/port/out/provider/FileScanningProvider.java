package com.kts.kronos.application.port.out.provider;

public interface FileScanningProvider {
    void scanOrThrow(String fileName, String contentType, byte[] content);
}