package com.kts.kronos.application;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.AfdEntryProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.service.AfdService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.AfdEntry;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static com.kts.kronos.constants.Messages.AFD_DATE_FMT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AfdServiceTest {

    @InjectMocks
    private AfdService service;

    @Mock
    private AfdEntryProvider afdProvider;
    @Mock
    private CompanyProvider companyProvider;

    @Test
    @DisplayName("logMarking: deve salvar registro AFD com hash encadeado")
    void shouldLogMarkingWithChainedHash() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDateTime date = LocalDateTime.of(2026, 4, 20, 8, 15);
        Company company = company(companyId);
        Employee employee = employee(employeeId, companyId, "123.456.789-01");
        String previousHash = "previous-hash";

        when(afdProvider.findLastHashByCompanyId(companyId)).thenReturn(Optional.of(previousHash));

        service.logMarking(company, employee, date, 123L);

        ArgumentCaptor<AfdEntry> captor = ArgumentCaptor.forClass(AfdEntry.class);
        verify(afdProvider).save(captor.capture());

        AfdEntry saved = captor.getValue();
        assertEquals(123L, saved.nsr());
        assertEquals("7", saved.recordType());
        assertEquals(date, saved.recordDate());
        assertEquals("123.456.789-01", saved.employeeCpf());
        assertEquals(companyId, saved.companyId());
        assertEquals(employeeId, saved.employeeId());
        assertEquals(previousHash, saved.previousHash());
        assertEquals(expectedHash("0000001237" + date.format(AFD_DATE_FMT) + "12345678901" + previousHash), saved.currentHash());
    }

    @Test
    @DisplayName("writeAfdToStream: deve escrever cabecalho, registros tipo 7 e trailer")
    void shouldWriteAfdFileToStream() {
        UUID companyId = UUID.randomUUID();
        Company company = company(companyId);
        LocalDateTime firstDate = LocalDateTime.of(2026, 4, 20, 8, 0);
        LocalDateTime secondDate = LocalDateTime.of(2026, 4, 20, 17, 0);
        AfdEntry first = new AfdEntry(2L, "7", firstDate, "123.456.789-01", "pis", companyId, UUID.randomUUID(), null, "hash-1");
        AfdEntry second = new AfdEntry(3L, "7", secondDate, null, "pis", companyId, UUID.randomUUID(), "hash-1", "hash-2");

        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(afdProvider.streamByCompanyIdOrderByNsr(companyId)).thenReturn(Stream.of(first, second));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        service.writeAfdToStream(companyId, output);

        String expected = "00000000111"
                + "12345678000199"
                + "KTS"
                + " ".repeat(147)
                + "\r\n"
                + "0000000027" + firstDate.format(AFD_DATE_FMT) + "012345678901" + "\r\n"
                + "0000000037" + secondDate.format(AFD_DATE_FMT) + "000000000000" + "\r\n"
                + "999999999000000002";

        assertEquals(expected, output.toString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("writeAfdToStream: deve falhar quando empresa nao existir")
    void shouldThrowWhenCompanyDoesNotExist() {
        UUID companyId = UUID.randomUUID();
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.writeAfdToStream(companyId, new ByteArrayOutputStream())
        );
    }

    private static Company company(UUID companyId) {
        return new Company(
                companyId,
                "KTS",
                "12345678000199",
                "contato@kts.com",
                true,
                new Address("Rua A", "10", "65000000", "Sao Luis", "MA"),
                null,
                0,
                0
        );
    }

    private static Employee employee(UUID employeeId, UUID companyId, String cpf) {
        return new Employee(
                employeeId,
                "Ana Paula",
                cpf,
                "12345678901",
                "Analista",
                "ana@kts.com",
                1000.0,
                "11999999999",
                true,
                new Address("Rua A", "10", "65000000", "Sao Luis", "MA"),
                companyId,
                null,
                false,
                null,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                null,
                null,
                null,
                null,
                null
        );
    }

    private static String expectedHash(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
