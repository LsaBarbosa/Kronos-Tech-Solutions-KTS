package com.kts.kronos.adapter.in.web.http.webmvc;

import com.kts.kronos.adapter.in.web.dto.document.DocumentWithData;
import com.kts.kronos.adapter.in.web.exceptions.RestExceptionHandler;
import com.kts.kronos.adapter.in.web.http.DocumentController;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.enuns.DocumentType;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RestExceptionHandler.class)
class DocumentControllerWebMvcTest {

    @Resource
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentUseCase documentUseCase;

    @Test
    void shouldUploadDocumentSuccessfully() throws Exception {
        UUID employeeId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holerite.pdf",
                "application/pdf",
                "pdf-content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .param("type", "PAYSLIP")
                        .param("employeeId", employeeId.toString()))
                .andExpect(status().isCreated());

        verify(documentUseCase).uploadDocument(eq(DocumentType.PAYSLIP), eq(employeeId), any(MultipartFile.class));
    }

    @Test
    void shouldReturnBadRequestWhenUploadRequestIsMalformed() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "holerite.pdf",
                "application/pdf",
                "pdf-content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .param("type", "INVALID_TYPE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldTranslateExceptionWhenUploadingDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                "application/octet-stream",
                "binary".getBytes(StandardCharsets.UTF_8)
        );
        doThrow(new BadRequestException("Tipo de arquivo inválido"))
                .when(documentUseCase).uploadDocument(eq(DocumentType.PAYSLIP), eq(null), any(MultipartFile.class));

        mockMvc.perform(multipart("/documents")
                        .file(file)
                        .param("type", "PAYSLIP"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Tipo de arquivo inválido"));
    }

    @Test
    void shouldListDocumentsByType() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        Document document = new Document(
                documentId,
                employeeId,
                DocumentType.PAYSLIP,
                "holerite.pdf",
                "application/pdf",
                "/docs/holerite.pdf",
                LocalDateTime.of(2026, 4, 17, 10, 0),
                null,
                false,
                false
        );

        when(documentUseCase.listDocuments(DocumentType.PAYSLIP, employeeId, null))
                .thenReturn(List.of(document));

        mockMvc.perform(get("/documents")
                        .param("type", "PAYSLIP")
                        .param("employeeId", employeeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents[0].id").value(documentId.toString()))
                .andExpect(jsonPath("$.documents[0].fileName").value("holerite.pdf"))
                .andExpect(jsonPath("$.documents[0].type").value("PAYSLIP"));
    }

    @Test
    void shouldListDocumentsByTypeAndDate() throws Exception {
        UUID employeeId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 4, 17);

        when(documentUseCase.listDocuments(DocumentType.PAYSLIP, employeeId, date))
                .thenReturn(List.of());

        mockMvc.perform(get("/documents")
                        .param("type", "PAYSLIP")
                        .param("employeeId", employeeId.toString())
                        .param("date", "2026-04-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents").isArray());

        verify(documentUseCase).listDocuments(DocumentType.PAYSLIP, employeeId, date);
    }

    @Test
    void shouldDownloadDocumentWithEmployeeIdAndReturnHeaders() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        DocumentWithData document = new DocumentWithData(
                documentId,
                employeeId,
                DocumentType.PAYSLIP,
                "holerite.pdf",
                "application/pdf",
                "PDF".getBytes(StandardCharsets.UTF_8),
                LocalDateTime.of(2026, 4, 17, 10, 0)
        );

        when(documentUseCase.downloadDocument(employeeId, documentId)).thenReturn(document);

        mockMvc.perform(get("/documents/{documentId}", documentId)
                        .param("employeeId", employeeId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"holerite.pdf\""))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes("PDF".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void shouldDownloadDocumentWithoutEmployeeId() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        DocumentWithData document = new DocumentWithData(
                documentId,
                employeeId,
                DocumentType.PAYSLIP,
                "holerite.pdf",
                "application/pdf",
                "PDF".getBytes(StandardCharsets.UTF_8),
                LocalDateTime.of(2026, 4, 17, 10, 0)
        );

        when(documentUseCase.downloadDocument(null, documentId)).thenReturn(document);

        mockMvc.perform(get("/documents/{documentId}", documentId))
                .andExpect(status().isOk());

        verify(documentUseCase).downloadDocument(null, documentId);
    }

    @Test
    void shouldTranslateExceptionWhenDownloadingDocument() throws Exception {
        UUID documentId = UUID.randomUUID();
        when(documentUseCase.downloadDocument(null, documentId))
                .thenThrow(new ResourceNotFoundException("Documento não encontrado"));

        mockMvc.perform(get("/documents/{documentId}", documentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Documento não encontrado"));
    }

    @Test
    void shouldDeleteDocumentWithEmployeeId() throws Exception {
        UUID employeeId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        mockMvc.perform(delete("/documents/{documentId}", documentId)
                        .param("employeeId", employeeId.toString()))
                .andExpect(status().isNoContent());

        verify(documentUseCase).deleteDocument(employeeId, documentId);
    }

    @Test
    void shouldDeleteDocumentWithoutEmployeeId() throws Exception {
        UUID documentId = UUID.randomUUID();

        mockMvc.perform(delete("/documents/{documentId}", documentId))
                .andExpect(status().isNoContent());

        verify(documentUseCase).deleteDocument(null, documentId);
    }

    @Test
    void shouldTranslateExceptionWhenDeletingDocument() throws Exception {
        UUID documentId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Documento não encontrado"))
                .when(documentUseCase).deleteDocument(null, documentId);

        mockMvc.perform(delete("/documents/{documentId}", documentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Documento não encontrado"));
    }
    @Test
    void shouldDownloadDocumentWithInvalidContentType_fallsBackToOctetStream() throws Exception {
        UUID documentId = UUID.randomUUID();

        DocumentWithData document = new DocumentWithData(
                documentId,
                null,
                DocumentType.PAYSLIP,
                "file.bin",
                "invalid!!content-type",
                "data".getBytes(StandardCharsets.UTF_8),
                LocalDateTime.of(2026, 4, 17, 10, 0)
        );

        when(documentUseCase.downloadDocument(null, documentId)).thenReturn(document);

        mockMvc.perform(get("/documents/{documentId}", documentId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM));

        verify(documentUseCase).downloadDocument(null, documentId);
    }


}
