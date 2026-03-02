package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.AfdEntryProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.domain.model.AfdEntry;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AfdServiceTest {

    @Mock AfdEntryProvider afdProvider;
    @Mock CompanyProvider companyProvider;

    @InjectMocks AfdService service;

    private UUID companyId;
    private Company company;
    private Employee employee;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        company = new Company(companyId, "KTS", "12.345.678/0001-90", "contato@kts.com", true,
                null, null, 0, 0);

        employee = new Employee(
                UUID.randomUUID(),
                "João da Silva",
                "123.456.789-01",
                "12345678901",
                "Dev",
                "joao@kts.com",
                1000d,
                "11999998888",
                true,
                null,
                companyId,
                LocalDateTime.now(),
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Set.of()
        );
    }

    @Test
    void logMarkingSavesEntryWhenNoPreviousHash() {
        LocalDateTime date = LocalDateTime.of(2026, 1, 10, 8, 30);
        when(afdProvider.findLastHashByCompanyId(companyId)).thenReturn(Optional.empty());

        service.logMarking(company, employee, date, 1L);

        ArgumentCaptor<AfdEntry> captor = ArgumentCaptor.forClass(AfdEntry.class);
        verify(afdProvider).save(captor.capture());
        AfdEntry saved = captor.getValue();

        assertEquals(1L, saved.nsr());
        assertEquals("7", saved.recordType());
        assertEquals(date, saved.recordDate());
        assertEquals(employee.cpf(), saved.employeeCpf());
        assertNull(saved.previousHash());
        assertNotNull(saved.currentHash());
        assertEquals(64, saved.currentHash().length());
    }

    @Test
    void logMarkingSavesEntryWhenPreviousHashExists() {
        LocalDateTime date = LocalDateTime.of(2026, 1, 10, 18, 45);
        when(afdProvider.findLastHashByCompanyId(companyId)).thenReturn(Optional.of("abc123"));

        service.logMarking(company, employee, date, 2L);

        ArgumentCaptor<AfdEntry> captor = ArgumentCaptor.forClass(AfdEntry.class);
        verify(afdProvider).save(captor.capture());
        AfdEntry saved = captor.getValue();

        assertEquals("abc123", saved.previousHash());
        assertNotNull(saved.currentHash());
        assertEquals(64, saved.currentHash().length());
    }

    @Test
    void writeAfdToStreamWritesHeaderRecordsAndTrailer() {
        UUID localCompanyId = UUID.randomUUID();
        Company localCompany = new Company(localCompanyId, "Empresa Teste", "12.345.678/0001-90", "x@x.com", true,
                null, null, 0, 0);
        AfdEntry first = new AfdEntry(1L, "7", LocalDateTime.of(2026, 1, 10, 8, 0), "123.456.789-01", "111", localCompanyId, UUID.randomUUID(), null, "h1");
        AfdEntry second = new AfdEntry(2L, "7", LocalDateTime.of(2026, 1, 10, 17, 0), "222.333.444-55", "222", localCompanyId, UUID.randomUUID(), "h1", "h2");

        when(companyProvider.findById(localCompanyId)).thenReturn(Optional.of(localCompany));
        when(afdProvider.streamByCompanyIdOrderByNsr(localCompanyId)).thenReturn(Stream.of(first, second));

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        service.writeAfdToStream(localCompanyId, outputStream);

        String content = outputStream.toString();

        assertTrue(content.startsWith("00000000111"));
        assertTrue(content.contains("0000000017"));
        assertTrue(content.contains("0000000027"));
        assertTrue(content.endsWith("999999999000000002"));
    }

    @Test
    void writeAfdToStreamThrowsWhenCompanyNotFound() {
        UUID missingCompanyId = UUID.randomUUID();
        when(companyProvider.findById(missingCompanyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.writeAfdToStream(missingCompanyId, new ByteArrayOutputStream()));
    }

    @Test
    void writeAfdToStreamWrapsProviderFailure() {
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(afdProvider.streamByCompanyIdOrderByNsr(companyId)).thenThrow(new RuntimeException("falha stream"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.writeAfdToStream(companyId, new ByteArrayOutputStream()));

        assertEquals("Falha crítica na geração do arquivo AFD", ex.getMessage());
    }

    @Test
    void privateFormattingMethodsCoverAllBranches() throws Exception {
        assertEquals("00123", invokePrivate("formatNumeric", "123", 5));
        assertEquals("123", invokePrivate("formatNumeric", "123456", 3));
        assertEquals("0000", invokePrivate("formatNumeric", null, 4));

        assertEquals("abc  ", invokePrivate("formatString", "abc", 5));
        assertEquals("abc", invokePrivate("formatString", "abcdef", 3));
        assertEquals("    ", invokePrivate("formatString", null, 4));

        assertEquals("12345678901", invokePrivate("formatCpf", "123.456.789-01"));
        assertEquals("00000000000", invokePrivate("formatCpf", (Object) null));
    }

    @Test
    void formatType7AndCalculateSha256Success() throws Exception {
        AfdEntry entry = new AfdEntry(10L, "7", LocalDateTime.of(2026, 1, 10, 12, 34), "987.654.321-00", "pis",
                companyId, employee.employeeId(), null, "hash");

        String type7 = (String) invokePrivate("formatType7", entry);
        assertTrue(type7.startsWith("0000000107"));
        assertTrue(type7.endsWith("098765432100"));

        String hash = (String) invokePrivate("calculateSha256", "conteudo");
        assertNotNull(hash);
        assertEquals(64, hash.length());
    }

    @Test
    void calculateSha256ThrowsWhenAlgorithmUnavailable() throws Exception {
        try (MockedStatic<MessageDigest> mocked = mockStatic(MessageDigest.class)) {
            mocked.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new NoSuchAlgorithmException("indisponivel"));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> invokePrivate("calculateSha256", "conteudo"));

            assertTrue(ex.getMessage().contains("Erro ao calcular Hash SHA-256"));
        }
    }

    private Object invokePrivate(String methodName, Object... args) throws Exception {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] == null ? String.class : args[i].getClass();
        }

        Method method;
        if ("formatNumeric".equals(methodName) || "formatString".equals(methodName)) {
            method = AfdService.class.getDeclaredMethod(methodName, String.class, int.class);
        } else if ("formatType7".equals(methodName)) {
            method = AfdService.class.getDeclaredMethod(methodName, AfdEntry.class);
        } else {
            method = AfdService.class.getDeclaredMethod(methodName, String.class);
        }
        method.setAccessible(true);

        try {
            return method.invoke(service, args);
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw e;
        }
    }
}
