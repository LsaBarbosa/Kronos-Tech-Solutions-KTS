package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.DigitalSignatureException;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.infrastructure.DigitalSignatureService;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AejServiceCoverageTest {

    @Mock private CompanyProvider companyProvider;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private TimeRecordProvider recordRepository;
    @Mock private DigitalSignatureService signatureService;
    @Mock private KronosMetrics kronosMetrics;
    @Mock private KronosTracing kronosTracing;

    private AejService service(KronosMetrics metrics, KronosTracing tracing) {
        AejService svc = new AejService(companyProvider, employeeProvider, recordRepository,
                signatureService, metrics, tracing);
        ReflectionTestUtils.setField(svc, "inpiNumber", "123456789");
        ReflectionTestUtils.setField(svc, "softwareVersion", "1.0");
        ReflectionTestUtils.setField(svc, "developerName", "KRONOS");
        return svc;
    }

    private static final LocalDate START = LocalDate.of(2026, 1, 1);
    private static final LocalDate END   = LocalDate.of(2026, 1, 31);

    // ── metrics() — kronosMetrics != null TRUE branch ─────────────────────────

    @Test
    @DisplayName("metrics() TRUE branch: kronosMetrics não-nulo → usa instância injetada")
    void generateAej_withNonNullMetrics_coversTrueBranch() {
        UUID cid = UUID.randomUUID();
        when(companyProvider.findById(cid)).thenReturn(Optional.of(company(cid)));
        when(employeeProvider.findByCompanyId(cid)).thenReturn(List.of());
        when(recordRepository.findByEmployeeIdsAndRange(any(), any(), any())).thenReturn(List.of());
        when(signatureService.signData(any())).thenAnswer(inv -> inv.getArgument(0));

        service(kronosMetrics, null).generateAej(cid, START, END, new ByteArrayOutputStream());

        verify(kronosMetrics).legalSuccess("aej");
    }

    // ── tracing() — kronosTracing != null TRUE branch ─────────────────────────

    @Test
    @DisplayName("tracing() TRUE branch: kronosTracing não-nulo → usa instância injetada")
    void generateAej_withNonNullTracing_coversTrueBranch() {
        UUID cid = UUID.randomUUID();
        when(companyProvider.findById(cid)).thenReturn(Optional.of(company(cid)));

        service(null, kronosTracing).generateAej(cid, START, END, new ByteArrayOutputStream());

        verify(kronosTracing).observe(any(), any(Runnable.class));
    }

    // ── generateAej outer RuntimeException catch ──────────────────────────────

    @Test
    @DisplayName("generateAej: RuntimeException dentro da lambda (antes de signData) → outer RuntimeException catch")
    void generateAej_lambdaThrowsRuntimeException_outerCatchFiresAndRethrows() {
        UUID cid = UUID.randomUUID();
        when(companyProvider.findById(cid)).thenReturn(Optional.of(company(cid)));
        when(employeeProvider.findByCompanyId(cid)).thenThrow(new IllegalStateException("DB error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service(null, null).generateAej(cid, START, END, new ByteArrayOutputStream()));
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }

    // ── lambda inner RuntimeException catch ──────────────────────────────────

    @Test
    @DisplayName("generateAej: signData lança RuntimeException não-DSE → inner catch embrulha em DigitalSignatureException")
    void generateAej_signDataThrowsRuntimeException_wrapsInDSE() {
        UUID cid = UUID.randomUUID();
        when(companyProvider.findById(cid)).thenReturn(Optional.of(company(cid)));
        when(employeeProvider.findByCompanyId(cid)).thenReturn(List.of());
        when(recordRepository.findByEmployeeIdsAndRange(any(), any(), any())).thenReturn(List.of());
        when(signatureService.signData(any())).thenThrow(new IllegalStateException("HSM unavailable"));

        DigitalSignatureException ex = assertThrows(DigitalSignatureException.class,
                () -> service(null, null).generateAej(cid, START, END, new ByteArrayOutputStream()));
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }

    // ── lambda IOException catch ──────────────────────────────────────────────

    @Test
    @DisplayName("generateAej: OutputStream lança IOException → IOException catch embrulha em RuntimeException")
    void generateAej_outputStreamThrowsIOException_wrapsInRuntimeException() throws IOException {
        UUID cid = UUID.randomUUID();
        OutputStream badStream = mock(OutputStream.class);
        doThrow(new IOException("Stream fechado")).when(badStream).write(any(byte[].class));
        doThrow(new IOException("Stream fechado")).when(badStream).write(any(byte[].class), anyInt(), anyInt());
        when(companyProvider.findById(cid)).thenReturn(Optional.of(company(cid)));
        when(employeeProvider.findByCompanyId(cid)).thenReturn(List.of());
        when(recordRepository.findByEmployeeIdsAndRange(any(), any(), any())).thenReturn(List.of());
        when(signatureService.signData(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(RuntimeException.class,
                () -> service(null, null).generateAej(cid, START, END, badStream));
    }

    // ── generateType05Lines — startWork nulo → sem linha de entrada ──────────

    @Test
    @DisplayName("generateType05Lines: startWork nulo → if(startWork!=null) FALSE, apenas linha de saída gerada")
    void generateType05Lines_startWorkNull_skipsEntryLine() {
        UUID cid = UUID.randomUUID();
        UUID eid = UUID.randomUUID();
        LocalDateTime endTime = LocalDateTime.of(2026, 1, 5, 17, 0);

        when(companyProvider.findById(cid)).thenReturn(Optional.of(company(cid)));
        when(employeeProvider.findByCompanyId(cid)).thenReturn(List.of(employee(eid, cid)));
        when(recordRepository.findByEmployeeIdsAndRange(any(), any(), any())).thenReturn(
                List.of(record(eid, StatusRecord.CREATED, false, null, endTime, null, endTime)));
        when(signatureService.signData(any())).thenAnswer(inv -> inv.getArgument(0));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service(null, null).generateAej(cid, START, END, out);
        String content = out.toString(StandardCharsets.ISO_8859_1);

        assertFalse(content.contains("|001|E|"), "Não deve ter linha de entrada");
        assertTrue(content.contains("|001|S|"), "Deve ter linha de saída");
    }

    // ── generateType05Lines — endWork nulo → sem linha de saída ──────────────

    @Test
    @DisplayName("generateType05Lines: endWork nulo → if(endWork!=null) FALSE, apenas linha de entrada gerada")
    void generateType05Lines_endWorkNull_skipsExitLine() {
        UUID cid = UUID.randomUUID();
        UUID eid = UUID.randomUUID();
        LocalDateTime startTime = LocalDateTime.of(2026, 1, 5, 8, 0);

        when(companyProvider.findById(cid)).thenReturn(Optional.of(company(cid)));
        when(employeeProvider.findByCompanyId(cid)).thenReturn(List.of(employee(eid, cid)));
        when(recordRepository.findByEmployeeIdsAndRange(any(), any(), any())).thenReturn(
                List.of(record(eid, StatusRecord.CREATED, false, startTime, null, startTime, null)));
        when(signatureService.signData(any())).thenAnswer(inv -> inv.getArgument(0));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service(null, null).generateAej(cid, START, END, out);
        String content = out.toString(StandardCharsets.ISO_8859_1);

        assertTrue(content.contains("|001|E|"), "Deve ter linha de entrada");
        assertFalse(content.contains("|001|S|"), "Não deve ter linha de saída");
    }

    // ── determineSource — edited=false mas horários diferentes → "I" ─────────

    @Test
    @DisplayName("determineSource: não editado mas horário difere do original → retorna 'I'")
    void generateType05Lines_notEditedButTimeDiffers_sourceIsI() {
        UUID cid = UUID.randomUUID();
        UUID eid = UUID.randomUUID();
        LocalDateTime startTime   = LocalDateTime.of(2026, 1, 5, 8, 0);
        LocalDateTime origStart   = LocalDateTime.of(2026, 1, 5, 7, 45); // diferente!
        LocalDateTime endTime     = LocalDateTime.of(2026, 1, 5, 17, 0);

        when(companyProvider.findById(cid)).thenReturn(Optional.of(company(cid)));
        when(employeeProvider.findByCompanyId(cid)).thenReturn(List.of(employee(eid, cid)));
        when(recordRepository.findByEmployeeIdsAndRange(any(), any(), any())).thenReturn(
                List.of(record(eid, StatusRecord.CREATED, false, startTime, endTime, origStart, endTime)));
        when(signatureService.signData(any())).thenAnswer(inv -> inv.getArgument(0));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service(null, null).generateAej(cid, START, END, out);
        String content = out.toString(StandardCharsets.ISO_8859_1);

        assertTrue(content.contains("|E||I|"), "Source deve ser 'I' quando original difere do atual");
    }

    // ── generateType07 — startWork nulo, endWork não-nulo → usa endWork ───────

    @Test
    @DisplayName("generateType07: startWork nulo e endWork não-nulo → usa endWork para data do evento")
    void generateType07_startWorkNull_endWorkNonNull_usesEndWorkDate() {
        UUID cid = UUID.randomUUID();
        UUID eid = UUID.randomUUID();
        LocalDateTime endTime = LocalDateTime.of(2026, 1, 15, 17, 0);

        when(companyProvider.findById(cid)).thenReturn(Optional.of(company(cid)));
        when(employeeProvider.findByCompanyId(cid)).thenReturn(List.of(employee(eid, cid)));
        when(recordRepository.findByEmployeeIdsAndRange(any(), any(), any())).thenReturn(
                List.of(record(eid, StatusRecord.ABSENCE, false, null, endTime, null, null)));
        when(signatureService.signData(any())).thenAnswer(inv -> inv.getArgument(0));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service(null, null).generateAej(cid, START, END, out);
        String content = out.toString(StandardCharsets.ISO_8859_1);

        assertTrue(content.contains("07|"), "Deve ter linha tipo 07 de ausência");
        assertTrue(content.contains("2026-01-15"), "Deve usar data do endWork como data do evento");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Company company(UUID companyId) {
        return new Company(
                companyId, "KTS", "12345678000199", "kts@kts.com", true,
                new Address("Rua A", "10", "01001000", "São Paulo", "SP"),
                null, 0L, 0L
        );
    }

    private static Employee employee(UUID employeeId, UUID companyId) {
        return new Employee(
                employeeId, "Teste", "12345678901", "12345678901",
                "Dev", "emp@kts.com", 1000.0, null, true,
                new Address("Rua B", "1", "01001000", "SP", "SP"),
                companyId, null, false, null, null, null,
                null, null, null, null, null, null, null
        );
    }

    private static TimeRecord record(
            UUID employeeId, StatusRecord status, boolean edited,
            LocalDateTime startWork, LocalDateTime endWork,
            LocalDateTime origStart, LocalDateTime origEnd
    ) {
        return new TimeRecord(
                null, startWork, endWork, status, edited, true,
                employeeId, null, null, null, null, null, null,
                origStart, origEnd
        );
    }
}
