package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.document.DocumentResponse;
import com.kts.kronos.adapter.in.web.dto.document.DocumentResponseList;
import com.kts.kronos.adapter.in.web.dto.document.DocumentWithData;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.domain.model.enuns.DocumentType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;
import static com.kts.kronos.constants.Swagger.*;
import static com.kts.kronos.constants.ApiPaths.*;
import static com.kts.kronos.constants.Messages.*;


@RestController
@RequestMapping(DOCUMENTS)
@RequiredArgsConstructor
@Tag(name = SWAGGER_DOC_TAG, description = SWAGGER_DOC_DESC)
public class DocumentController {

    private final DocumentUseCase useCase;

    @Operation(summary = UPLOAD_DOC_SUMMARY, description = UPLOAD_DOC_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = UPLOAD_DOC_SUCCESS),
            @ApiResponse(responseCode = "400", description = UPLOAD_DOC_400),
            @ApiResponse(responseCode = "404", description = UPLOAD_DOC_404),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
    @PreAuthorize(ANY_EMPLOYEE)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void upload(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam("type") DocumentType type,
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        useCase.uploadDocument(type,employeeId, file);
    }

    @Operation(summary = LIST_DOC_SUMMARY, description = LIST_DOC_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = LIST_SUCCESS),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping
    public ResponseEntity<DocumentResponseList> list(
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @RequestParam("type") DocumentType type
    ) {
        var docs = useCase.listDocuments(type, employeeId, date);
        return ResponseEntity.ok(new DocumentResponseList(docs.stream().map(
                DocumentResponse::fromDomain).toList()));
    }

    @Operation(summary = DOWN_DOC_SUMMARY, description = DOWN_DOC_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = DOWN_DOC_SUCCESS),
            @ApiResponse(responseCode = "400", description = DOWN_DOC_400),
            @ApiResponse(responseCode = "404", description = DOWN_DOC_404),
            @ApiResponse(responseCode = "403", description = ACCESS_DENIED)
    })
    @PreAuthorize(ANY_EMPLOYEE)
    @GetMapping(DOCUMENT_ID)
    public ResponseEntity<byte[]> download(
            @RequestParam(required = false) UUID employeeId,
            @PathVariable UUID documentId) throws IOException {

        DocumentWithData doc = useCase.downloadDocument(employeeId, documentId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.fileName() + "\"")
                .body(doc.data());
    }

    @Operation(summary = DEL_DOC_SUMMARY, description = DEL_DOC_DESC)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = DEL_DOC_SUCCESS),
            @ApiResponse(responseCode = "403", description = DEL_DOC_403),
            @ApiResponse(responseCode = "404", description = DEL_DOC_404)
    })
    @PreAuthorize(ANY_EMPLOYEE)
    @DeleteMapping(DOCUMENT_ID)
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteDocument( @RequestParam(required = false) UUID employeeId,  @PathVariable UUID documentId) {
        useCase.deleteDocument(employeeId, documentId);
    }

}
