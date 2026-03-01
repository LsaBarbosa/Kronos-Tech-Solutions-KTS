package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.AfdEntryProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
