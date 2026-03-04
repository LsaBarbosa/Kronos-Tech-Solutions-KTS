package com.kts.kronos.adapter.in.web.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.dto.document.DocumentWithData;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.enuns.DocumentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false) // Desativa filtros de segurança (JWT) para focar no teste funcional
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DocumentUseCase documentUseCase;

    private static final String BASE_URL = "/documents";
    private static final UUID DOC_ID = UUID.randomUUID();
    private static final UUID EMP_ID = UUID.randomUUID();

    // --- CENÁRIOS DE UPLOAD (POST) ---

    @Test
    @DisplayName("Deve realizar upload de documento com sucesso (200 OK)")
    void shouldUploadDocumentSuccessfully() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "contrato.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "conteudo-mock".getBytes()
        );

        // Simulamos que o método void execute sem erro
        doNothing().when(documentUseCase).uploadDocument(eq(DocumentType.DOCUMENTS), eq(EMP_ID), any());

        mockMvc.perform(multipart(BASE_URL)
                        .file(file)
                        .param("type", "DOCUMENTS")
                        .param("employeeId", EMP_ID.toString()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(documentUseCase).uploadDocument(eq(DocumentType.DOCUMENTS), eq(EMP_ID), any());
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found no upload quando funcionário não existir")
    void shouldReturn404WhenUploadEmployeeNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "contrato.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "conteudo-mock".getBytes()
        );

        doThrow(new ResourceNotFoundException("Funcionário não encontrado"))
                .when(documentUseCase).uploadDocument(any(), any(), any());

        mockMvc.perform(multipart(BASE_URL)
                        .file(file)
                        .param("type", "DOCUMENTS")
                        .param("employeeId", EMP_ID.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Funcionário não encontrado"));
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden no upload quando usuário não possuir permissão")
    void shouldReturn403WhenUploadWithoutPermission() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "contrato.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "conteudo-mock".getBytes()
        );

        doThrow(new ForbiddenException(DOCUMENT_NOT_BELONGS_EMPLOYEE))
                .when(documentUseCase).uploadDocument(any(), any(), any());

        mockMvc.perform(multipart(BASE_URL)
                        .file(file)
                        .param("type", "DOCUMENTS")
                        .param("employeeId", EMP_ID.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Sem permissão para utilizar esse recurso"));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request se o arquivo for inválido (Ex: Tipo não permitido)")
    void shouldReturn400WhenUploadInvalidFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "virus.exe", "application/octet-stream", "lixo".getBytes()
        );

        // Simula a validação de tipo MIME feita no Service
        doThrow(new BadRequestException(INVALID_DOCUMENT_TYPE))
                .when(documentUseCase).uploadDocument(any(), any(), any());

        mockMvc.perform(multipart(BASE_URL)
                        .file(file)
                        .param("type", "DOCUMENTS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(INVALID_DOCUMENT_TYPE));
    }

    // --- CENÁRIOS DE LISTAGEM (GET) ---

    @Test
    @DisplayName("Deve listar documentos com sucesso (200 OK)")
    void shouldListDocumentsSuccessfully() throws Exception {
        // Prepara objeto de domínio para retorno
        Document mockDoc = new Document(
                DOC_ID, EMP_ID, DocumentType.PAYSLIP, "holerite.pdf",
                "application/pdf", "s3/path", LocalDateTime.now(), null, false, false
        );

        when(documentUseCase.listDocuments(eq(DocumentType.PAYSLIP), eq(EMP_ID), any()))
                .thenReturn(List.of(mockDoc));

        mockMvc.perform(get(BASE_URL)
                        .param("type", "PAYSLIP")
                        .param("employeeId", EMP_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents[0].id").value(DOC_ID.toString()))
                .andExpect(jsonPath("$.documents[0].fileName").value("holerite.pdf"));
    }

    @Test
    @DisplayName("Deve retornar lista vazia se nenhum documento for encontrado (200 OK)")
    void shouldReturnEmptyListWhenNoDocumentsFound() throws Exception {
        when(documentUseCase.listDocuments(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get(BASE_URL)
                        .param("type", "TIME_OFF") // Exemplo de tipo
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documents").isEmpty());
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden ao listar documentos sem permissão")
    void shouldReturn403WhenListDocumentsWithoutPermission() throws Exception {
        when(documentUseCase.listDocuments(any(), any(), any()))
                .thenThrow(new ForbiddenException("Sem permissão para visualizar documentos"));

        mockMvc.perform(get(BASE_URL)
                        .param("type", "DOCUMENTS"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Sem permissão para utilizar esse recurso"));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request quando data for inválida na listagem")
    void shouldReturn400WhenListDocumentsWithInvalidDate() throws Exception {
        mockMvc.perform(get(BASE_URL)
                        .param("type", "DOCUMENTS")
                        .param("date", "2025-99-99"))
                .andExpect(status().isBadRequest());

        verify(documentUseCase, never()).listDocuments(any(), any(), any());
    }

    // --- CENÁRIOS DE DOWNLOAD (GET /{id}) ---

    @Test
    @DisplayName("Deve realizar download de documento com sucesso (200 OK)")
    void shouldDownloadDocumentSuccessfully() throws Exception {
        byte[] content = "conteudo-binario".getBytes();
        DocumentWithData docWithData = new DocumentWithData(
                DOC_ID, EMP_ID, DocumentType.DOCUMENTS, "arquivo.txt",
                MediaType.TEXT_PLAIN_VALUE, content, LocalDateTime.now()
        );

        when(documentUseCase.downloadDocument(any(), eq(DOC_ID))).thenReturn(docWithData);

        mockMvc.perform(get(BASE_URL + "/{documentId}", DOC_ID)
                        .param("employeeId", EMP_ID.toString())) // Opcional no controller, mas bom testar
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"arquivo.txt\""))
                .andExpect(content().bytes(content));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found se o documento não existir no download")
    void shouldReturn404WhenDocumentNotFoundForDownload() throws Exception {
        when(documentUseCase.downloadDocument(any(), eq(DOC_ID)))
                .thenThrow(new ResourceNotFoundException(DOCUMENT_NOT_FOUND));

        mockMvc.perform(get(BASE_URL + "/{documentId}", DOC_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value(DOCUMENT_NOT_FOUND));
    }

    @Test
    @DisplayName("Deve retornar 400 Bad Request em falha de leitura (IO Error)")
    void shouldReturn400WhenDownloadFailsOnIO() throws Exception {
        // Simula erro ao ler do Bucket (Ex: arquivo corrompido ou inacessível)
        when(documentUseCase.downloadDocument(any(), any()))
                .thenThrow(new BadRequestException(ERROR_GET_FILE + "Erro S3"));

        mockMvc.perform(get(BASE_URL + "/{documentId}", DOC_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(ERROR_GET_FILE + "Erro S3"));
    }

    // --- CENÁRIOS DE EXCLUSÃO (DELETE) ---

    @Test
    @DisplayName("Deve deletar documento com sucesso (200 OK)")
    void shouldDeleteDocumentSuccessfully() throws Exception {
        doNothing().when(documentUseCase).deleteDocument(any(), eq(DOC_ID));

        mockMvc.perform(delete(BASE_URL + "/{documentId}", DOC_ID))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(documentUseCase).deleteDocument(any(), eq(DOC_ID));
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden ao tentar deletar documento de outro usuário (Regra de Negócio)")
    void shouldReturn403WhenDeletingOthersDocument() throws Exception {
        // Simula a validação no Service onde o usuário tenta apagar algo que não é dele
        doThrow(new ForbiddenException(DOCUMENT_NOT_BELONGS_EMPLOYEE))
                .when(documentUseCase).deleteDocument(any(), any());

        mockMvc.perform(delete(BASE_URL + "/{documentId}", DOC_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Sem permissão para utilizar esse recurso"));
    }

    @Test
    @DisplayName("Deve retornar 404 Not Found ao tentar deletar documento inexistente")
    void shouldReturn404WhenDeletingNonExistentDocument() throws Exception {
        doThrow(new ResourceNotFoundException(DOCUMENT_NOT_FOUND))
                .when(documentUseCase).deleteDocument(any(), eq(DOC_ID));

        mockMvc.perform(delete(BASE_URL + "/{documentId}", DOC_ID))
                .andExpect(status().isNotFound());
    }
}
