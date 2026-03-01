package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock CompanyProvider companyProvider;
    @Mock AddressLookupProvider viaCep;
    @Mock EmployeeProvider employeeProvider;
    @Mock UserProvider userProvider;

    @InjectMocks CompanyService service;

    @Test
    void getCompanyNameByIdReturnsNameFromProvider() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company(companyId, "Kronos", "12345678000100", "mail@kts.com", true,
                new Address("A", "1", "12345678", "SP", "SP"), new Location(1.0, 2.0), 1, 0);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));

        assertEquals("Kronos", service.getCompanyNameById(companyId));
    }
}
