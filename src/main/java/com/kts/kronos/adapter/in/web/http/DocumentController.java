package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.document.DocumentResponse;
import com.kts.kronos.adapter.in.web.dto.document.DocumentResponseList;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.DocumentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import static com.kts.kronos.constants.ApiPaths.DOCUMENTS;
import static com.kts.kronos.constants.ApiPaths.DOCUMENT_ID;
import static com.kts.kronos.constants.Swagger.*;

@RestController
@RequestMapping(DOCUMENTS)
@RequiredArgsConstructor
@Tag(name = DOCUMENT_API, description = DOCUMENT_DESCRIPTION_API)
public class DocumentController {
    private final DocumentUseCase useCase;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = CREATE, description = CREATE_DESCRIPTION)
    public void upload(
            @PathVariable UUID employeeId,
            @RequestParam("type") DocumentType type,
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        useCase.uploadDocument(type,employeeId, file);
    }

    @GetMapping
    @Operation(summary = GET_ALL, description = ALL_OBJECTS_DESCRIPTION)
    public ResponseEntity<DocumentResponseList> list(
            @PathVariable UUID employeeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam("type") DocumentType type
    ) {
        var docs = useCase.listDocuments(type, employeeId, date);
        return ResponseEntity.ok(new DocumentResponseList(docs.stream().map(
                DocumentResponse::fromDomain).toList()));
    }

    @GetMapping(DOCUMENT_ID)
    @Operation(summary = GET_BY_PARAMETER, description = DOCUMENT_DESCRIPTION)
    public ResponseEntity<byte[]> download(
            @PathVariable UUID employeeId,
            @PathVariable UUID documentId) throws IOException {
        Document doc = useCase.downloadDocument(employeeId, documentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.fileName() + "\"")
                .body(doc.data());
    }


}
