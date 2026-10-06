package com.kts.kronos.application.service;

import com.kts.kronos.application.port.out.provider.AfdEntryProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import com.kts.kronos.observability.support.ObservabilityDefaults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AfdServiceCoverageTest {

    @InjectMocks private AfdService service;

    @Mock private AfdEntryProvider afdProvider;
    @Mock private CompanyProvider companyProvider;
    @Mock private KronosMetrics kronosMetrics;
    @Mock private KronosTracing kronosTracing;

    // ── L168 FALSE + L172 FALSE: kronosMetrics/kronosTracing == null → NOOP ─────
    // Inject null so the ternary falls back to ObservabilityDefaults

    @Test
    void writeAfdToStream_withNullObservability_usesNoopDefaults() throws Exception {
        ReflectionTestUtils.setField(service, "kronosMetrics", null);
        ReflectionTestUtils.setField(service, "kronosTracing", null);

        UUID companyId = UUID.randomUUID();
        Company company = new Company(
            companyId, "Empresa Teste", "12345678000195",
            "test@emp.com", true, null, null, 0, 0
        );

        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(afdProvider.streamByCompanyIdOrderByNsr(companyId)).thenReturn(Stream.empty());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> service.writeAfdToStream(companyId, out));
        assertTrue(out.toString().startsWith("0000000011"));
    }

    // BR L168/L172 TRUE: kronosMetrics/kronosTracing != null → return the injected mock.
    // Stubs tracing.observe() to actually run the Runnable so the stream is written.
    @Test
    void writeAfdToStream_withNonNullObservability_coversTrueBranch() {
        doAnswer(inv -> { ((Runnable) inv.getArgument(1)).run(); return null; })
                .when(kronosTracing).observe(anyString(), any(Runnable.class));

        UUID companyId = UUID.randomUUID();
        Company company = new Company(
            companyId, "Empresa Mocked Obs", "12345678000195",
            "mock@emp.com", true, null, null, 0, 0
        );
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(afdProvider.streamByCompanyIdOrderByNsr(companyId)).thenReturn(Stream.empty());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> service.writeAfdToStream(companyId, out));
        assertTrue(out.toString().startsWith("0000000011"));
        verify(kronosMetrics).legalSuccess("afd");
    }

    // BR L168/L172 FALSE: kronosMetrics/kronosTracing == null → ObservabilityDefaults fallback
    // ReflectionTestUtils.setField fails for final fields in Java 21 JVM.
    // Direct constructor call with null forces the null branch to be taken.
    @Test
    void writeAfdToStream_directConstructorNullObs_coversNullBranch() {
        AfdService serviceNullObs = new AfdService(afdProvider, companyProvider, null, null);
        ReflectionTestUtils.setField(serviceNullObs, "inpiNumber", "123456789");

        UUID companyId = UUID.randomUUID();
        Company company = new Company(
            companyId, "Empresa Null Obs", "12345678000195",
            "null@emp.com", true, null, null, 0, 0
        );

        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(afdProvider.streamByCompanyIdOrderByNsr(companyId)).thenReturn(Stream.empty());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertDoesNotThrow(() -> serviceNullObs.writeAfdToStream(companyId, out));
        assertTrue(out.toString().startsWith("0000000011"));
    }
}
