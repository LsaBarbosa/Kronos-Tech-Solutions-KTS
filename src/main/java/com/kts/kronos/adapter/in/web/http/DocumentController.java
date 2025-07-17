package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.document.DocumentResponse;
import com.kts.kronos.adapter.in.web.dto.document.DocumentResponseList;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.domain.model.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.DOCUMENTS;
import static com.kts.kronos.constants.ApiPaths.DOCUMENT_ID;


@RestController
@RequestMapping(DOCUMENTS)
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentUseCase useCase;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(
            @PathVariable UUID employeeId,
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        var doc = useCase.uploadDocument(employeeId, file);
        var dto = new DocumentResponse(
                doc.documentId(),
                doc.fileName(),
                doc.contentType(),
                doc.uploadeAt()
        );
        return ResponseEntity
                .created(URI.create("/api/employees/" + employeeId + "/documents/" + doc.documentId()))
                .body(dto);
    }

    @GetMapping
    public ResponseEntity<DocumentResponseList> list(
            @PathVariable UUID employeeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        var docs = useCase.listDocuments(employeeId, date);
        return ResponseEntity.ok(new DocumentResponseList(docs.stream().map(
                DocumentResponse::fromDomain).toList()));
    }

    @GetMapping(DOCUMENT_ID)
    public ResponseEntity<byte[]> download(
            @PathVariable UUID employeeId,
            @PathVariable UUID documentId
    ) throws IOException {
        Document doc = useCase.downloadDocument(employeeId, documentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.fileName() + "\"")
                .body(doc.data());
    }
}
