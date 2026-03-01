package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.infrastructure.DigitalSignatureService;
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
class AejServiceTest {

    @Mock CompanyProvider companyProvider;
    @Mock EmployeeProvider employeeProvider;
    @Mock TimeRecordProvider recordRepository;
    @Mock DigitalSignatureService signatureService;

    @InjectMocks AejService service;

    @Test
    void generateAejThrowsWhenCompanyNotFound() {
        UUID companyId = UUID.randomUUID();
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.generateAej(companyId, java.time.LocalDate.now(), java.time.LocalDate.now(), new ByteArrayOutputStream()));
    }
}
