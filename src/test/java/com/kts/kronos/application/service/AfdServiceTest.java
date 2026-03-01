package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.AfdEntryProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.domain.model.AfdEntry;
import com.kts.kronos.domain.model.Company;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AfdServiceTest {

    @Mock AfdEntryProvider afdProvider;
    @Mock CompanyProvider companyProvider;

    @InjectMocks AfdService service;

    @Test
    void writeAfdToStreamThrowsWhenCompanyNotFound() {
        UUID companyId = UUID.randomUUID();
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.writeAfdToStream(companyId, new ByteArrayOutputStream()));
    }

    @Test
    void writeAfdToStreamWritesContentWhenCompanyExists() {
        UUID companyId = UUID.randomUUID();
        var company = new Company(companyId, "KTS", "12.345.678/0001-95", "mail@kts.com", true, null, null, 0L, 0L);
        var entry = new AfdEntry(1L, 10L, "7", LocalDateTime.of(2025, 1, 1, 9, 0), "12345678900", "123", companyId, UUID.randomUUID(), null, "hash");

        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(afdProvider.streamByCompanyIdOrderByNsr(companyId)).thenReturn(Stream.of(entry));

        var out = new ByteArrayOutputStream();
        service.writeAfdToStream(companyId, out);

        var text = out.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("0000000011"));
        assertTrue(text.contains("999999999"));
    }
}
