package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.afd.AfdImportConfirmResponse;
import com.kts.kronos.adapter.in.web.dto.afd.AfdImportPreviewResponse;

import java.io.IOException;
import java.io.InputStream;

public interface AfdImportUseCase {
    AfdImportPreviewResponse preview(InputStream inputStream) throws IOException;
    AfdImportConfirmResponse confirm(InputStream inputStream) throws IOException;
}
