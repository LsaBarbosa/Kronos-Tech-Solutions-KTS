package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.timesheetsignature.*;
import com.kts.kronos.application.port.in.usecase.TimesheetSignatureUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.hamcrest.Matchers;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@WebMvcTest(TimesheetSignatureController.class)
@AutoConfigureMockMvc(addFilters = false)
class TimesheetSignatureControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TimesheetSignatureUseCase useCase;
    @MockitoBean private ClientIpResolver clientIpResolver;

    @Test
    void deveRetornarStatusDaAssinaturaDoMes() throws Exception {
        var response = new PreviousMonthSignatureStatusResponse(
                2026, 6, LocalDate.of(2026,6,1), LocalDate.of(2026,6,30),
                "PENDING", true, false, null, null, null, null, "v1.0", null, null, List.of()
        );
        when(useCase.getMonthStatus(any(), any())).thenReturn(response);

        mockMvc.perform(get("/records/timesheet-signatures/status"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornarPreviewDoEspelho() throws Exception {
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        when(useCase.previewMonthMirror(any(), any(), any(), any())).thenReturn(new byte[]{1,2,3});

        mockMvc.perform(get("/records/timesheet-signatures/preview"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }

    @Test
    void deveRetornarPreviewComAnoEMesEspecificados() throws Exception {
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        when(useCase.previewMonthMirror(eq(2026), eq(5), any(), any())).thenReturn(new byte[]{1,2,3});

        mockMvc.perform(get("/records/timesheet-signatures/preview")
                        .param("year", "2026")
                        .param("month", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void deveAssinarEspelhoDoMes() throws Exception {
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        var response = new SignPreviousMonthTimesheetResponse(
                UUID.randomUUID(), 2026, 5, java.time.Instant.now(), "DIGITAL", "CLICK", "hash", "hash2", UUID.randomUUID(), "v1.0"
        );
        when(useCase.signMonth(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/records/timesheet-signatures/sign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referenceYear\":2026,\"referenceMonth\":5,\"confirmed\":true,\"declarationVersion\":\"v1.0\",\"declarationHashSha256\":\"hash1\",\"recordsSnapshotHashSha256\":\"def\",\"faceImageBase64\":\"base64img\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void deveDownloadDocumentoAssinado() throws Exception {
        var signatureId = UUID.randomUUID();
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        var download = new TimesheetSignatureUseCase.SignedDocumentDownload(
                new byte[]{1,2,3}, "espelho.pdf", "application/pdf"
        );
        when(useCase.downloadSignatureDocument(eq(signatureId), any(), any())).thenReturn(download);

        mockMvc.perform(get("/records/timesheet-signatures/" + signatureId + "/document"))
                .andExpect(status().isOk());
    }

    @Test
    void deveDownloadDocumentoComContentTypeInvalido() throws Exception {
        var signatureId = UUID.randomUUID();
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        // Invalid contentType → catch → APPLICATION_OCTET_STREAM
        var download = new TimesheetSignatureUseCase.SignedDocumentDownload(
                new byte[]{1,2,3}, "espelho.bin", "tipo/invalido/nao-existe"
        );
        when(useCase.downloadSignatureDocument(eq(signatureId), any(), any())).thenReturn(download);

        mockMvc.perform(get("/records/timesheet-signatures/" + signatureId + "/document"))
                .andExpect(status().isOk());
    }

    @Test
    void deveListarAssinaturasAdmin() throws Exception {
        var response = new AdminTimesheetSignaturePageResponse(List.of(), 0, 10, 0, 0);
        when(useCase.findAdmin(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/records/timesheet-signatures/admin"))
                .andExpect(status().isOk());
    }
    @Test
    void deveRetornarPreviewComAnoMasSemMes() throws Exception {
        // BR L62 B=false: year != null (A=true) but month == null (B=false) → "espelho_preview.pdf"
        when(clientIpResolver.resolve(any())).thenReturn("127.0.0.1");
        when(useCase.previewMonthMirror(eq(2026), isNull(), any(), any())).thenReturn(new byte[]{1,2,3});

        mockMvc.perform(get("/records/timesheet-signatures/preview")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(header().string(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("espelho_preview.pdf")));
    }

}
