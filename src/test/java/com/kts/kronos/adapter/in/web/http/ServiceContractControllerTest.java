package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.servicecontract.*;
import com.kts.kronos.application.port.in.usecase.ServiceContractUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServiceContractController.class)
@AutoConfigureMockMvc(addFilters = false)
class ServiceContractControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ServiceContractUseCase useCase;
    @MockitoBean private ClientIpResolver clientIpResolver;

    @Test
    void deveRetornarContratosCriadosViaPOST() throws Exception {
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        var response = new CreateServiceContractResponse(
                UUID.randomUUID(), "Contrato Teste", "doc.pdf",
                "abc123", Instant.now(), List.of(UUID.randomUUID())
        );
        when(useCase.create(any(), any(), any(), any(), any(), any())).thenReturn(response);

        String empId = UUID.randomUUID().toString();
        mockMvc.perform(multipart("/service-contracts/admin")
                        .file(new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1,2,3}))
                        .file(new MockMultipartFile("title", "", "text/plain", "Contrato Teste".getBytes()))
                        .file(new MockMultipartFile("employeeIds", "", "text/plain", empId.getBytes()))
                )
                .andExpect(status().isOk());
    }

    @Test
    void deveListarContratosAdmin() throws Exception {
        var response = new ServiceContractAdminPageResponse(List.of(), 0, 10, 0, 0);
        when(useCase.findAdmin(any(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/service-contracts/admin"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornarDetalheDeContratoAdmin() throws Exception {
        var contractId = UUID.randomUUID();
        var response = new ServiceContractAdminItemResponse(
                contractId, "Contrato", null, null, null, 0, 0, 0, 0
        );
        when(useCase.findAdminDetail(contractId)).thenReturn(response);

        mockMvc.perform(get("/service-contracts/admin/" + contractId))
                .andExpect(status().isOk());
    }

    @Test
    void deveListarContratosDoEmployee() throws Exception {
        var response = new PendingServiceContractListResponse(List.of());
        when(useCase.findPendingForCurrentEmployee()).thenReturn(response);

        mockMvc.perform(get("/service-contracts/me/pending"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornarPreviewDoPdfDoContrato() throws Exception {
        var contractId = UUID.randomUUID();
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        when(useCase.preview(eq(contractId), any(), any())).thenReturn(new byte[]{1,2,3});

        mockMvc.perform(get("/service-contracts/" + contractId + "/preview"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void deveAssinarContrato() throws Exception {
        var contractId = UUID.randomUUID();
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        var response = new SignServiceContractResponse(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now(), "DIGITAL", "CLICK", "sha256hash", "pdfhash", UUID.randomUUID(), "v1.0");
        when(useCase.sign(eq(contractId), any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/service-contracts/" + contractId + "/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmed\":true,\"declarationVersion\":\"v1.0\",\"declarationHashSha256\":\"hash1\",\"contractDocumentHashSha256\":\"hash2\",\"faceImageBase64\":\"base64img\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornarDocumentoAssinado() throws Exception {
        var signatureId = UUID.randomUUID();
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        var download = new ServiceContractUseCase.SignedDocumentDownload(
                new byte[]{1,2,3}, "contrato.pdf", "application/pdf"
        );
        when(useCase.downloadSignatureDocument(eq(signatureId), any(), any())).thenReturn(download);

        mockMvc.perform(get("/service-contracts/signatures/" + signatureId + "/document"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornarDocumentoAssinadoComContentTypeInvalido() throws Exception {
        var signatureId = UUID.randomUUID();
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        // contentType inválido → cai no catch → MediaType.APPLICATION_OCTET_STREAM
        var download = new ServiceContractUseCase.SignedDocumentDownload(
                new byte[]{1,2,3}, "contrato.bin", "tipo/invalido/muito/longo/que/nao/existe"
        );
        when(useCase.downloadSignatureDocument(eq(signatureId), any(), any())).thenReturn(download);

        mockMvc.perform(get("/service-contracts/signatures/" + signatureId + "/document"))
                .andExpect(status().isOk());
    }

    @Test
    void deveListarAssinaturasAdmin() throws Exception {
        var response = new ServiceContractSignatureAdminPageResponse(List.of(), 0, 10, 0, 0);
        when(useCase.findAdminSignatures(any(), any(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/service-contracts/admin/signatures"))
                .andExpect(status().isOk());
    }
    @Test
    void deveIgnorarEmployeeIdVazioNaListaCSV() throws Exception {
        // BR L63 FALSE: filter(s -> !s.isEmpty()) — empty string after split → filtered out
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        var response = new CreateServiceContractResponse(
                UUID.randomUUID(), "Contrato CSV", "doc.pdf",
                "abc123", java.time.Instant.now(), List.of(UUID.randomUUID())
        );
        when(useCase.create(any(), any(), any(), any(), any(), any())).thenReturn(response);

        String empId1 = UUID.randomUUID().toString();
        String empId2 = UUID.randomUUID().toString();
        // CSV with empty element in the middle: "uuid1,,uuid2" → split gives ["uuid1","","uuid2"]
        // filter(!isEmpty) removes "" → covers BR L63 FALSE branch
        String csvWithEmpty = empId1 + ",," + empId2;
        mockMvc.perform(multipart("/service-contracts/admin")
                        .file(new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1,2,3}))
                        .file(new MockMultipartFile("title", "", "text/plain", "Contrato CSV".getBytes()))
                        .file(new MockMultipartFile("employeeIds", "", "text/plain", csvWithEmpty.getBytes()))
                )
                .andExpect(status().isOk());
    }

}
