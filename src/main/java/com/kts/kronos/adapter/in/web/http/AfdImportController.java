package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.afd.AfdImportConfirmResponse;
import com.kts.kronos.adapter.in.web.dto.afd.AfdImportPreviewResponse;
import com.kts.kronos.application.port.in.usecase.AfdImportUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static com.kts.kronos.constants.Messages.ADMINISTRATOR;

@RestController
@RequestMapping("/afd")
@RequiredArgsConstructor
public class AfdImportController {

    private final AfdImportUseCase afdImportUseCase;

    @PreAuthorize(ADMINISTRATOR)
    @PostMapping(value = "/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AfdImportPreviewResponse> preview(
            @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(afdImportUseCase.preview(file.getInputStream()));
    }

    @PreAuthorize(ADMINISTRATOR)
    @PostMapping(value = "/import/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AfdImportConfirmResponse> confirm(
            @RequestPart("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(afdImportUseCase.confirm(file.getInputStream()));
    }
}
