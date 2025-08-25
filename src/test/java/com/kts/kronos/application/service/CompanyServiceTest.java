package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyRequest;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.COMPANY_ALREADY_EXIST;
import static com.kts.kronos.constants.Messages.COMPANY_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
@DisplayName("CompanyService Unit Tests")
class CompanyServiceTest {
    @Mock
    private CompanyProvider companyProvider;

    @Mock
    private AddressLookupProvider viaCep;

    @InjectMocks
    private CompanyService companyService;

    private Company company;
    private CreateCompanyRequest createCompanyRequest;
    private UpdateCompanyRequest updateCompanyRequest;
    private Address address;
    private String cnpj;

    @BeforeEach
    void setUp() {
        cnpj = "12345678901234";
        address = new Address("Rua Teste", "123", "12345678", "Cidade Teste", "Estado Teste");
        company = new Company(UUID.randomUUID(), "Company Test", cnpj, "email@test.com", true, address);

        createCompanyRequest = new CreateCompanyRequest(
                "New Company",
                "14725836914725",
                "new_email@test.com",
                new com.kts.kronos.adapter.in.web.dto.address.AddressRequest("87654321", "456")
        );
        updateCompanyRequest = new UpdateCompanyRequest(
                "Updated Name",
                "updated_email@test.com",
                true,
                null
        );
    }

    @Nested
    @DisplayName("Create Company")
    class CreateCompanyTests {
        @Test
        @DisplayName("Should create a new company successfully")
        void createCompany_Success() {
            when(companyProvider.findByCnpj(anyString())).thenReturn(Optional.empty());
            when(viaCep.lookup(anyString())).thenReturn(address);
            doNothing().when(companyProvider).save(any(Company.class));

            assertDoesNotThrow(() -> companyService.createCompany(createCompanyRequest));

            verify(companyProvider, times(1)).findByCnpj(createCompanyRequest.cnpj());
            verify(viaCep, times(1)).lookup(createCompanyRequest.address().postalCode());
            verify(companyProvider, times(1)).save(any(Company.class));
        }

        @Test
        @DisplayName("Should throw BadRequestException if company already exists")
        void createCompany_CompanyAlreadyExists_ThrowsException() {
            when(companyProvider.findByCnpj(anyString())).thenReturn(Optional.of(company));

            BadRequestException thrown = assertThrows(BadRequestException.class, () -> companyService.createCompany(createCompanyRequest));

            assertEquals(COMPANY_ALREADY_EXIST, thrown.getMessage());
            verify(companyProvider, times(1)).findByCnpj(createCompanyRequest.cnpj());
            verify(viaCep, never()).lookup(anyString());
            verify(companyProvider, never()).save(any(Company.class));
        }
    }

    @Nested
    @DisplayName("Get Company")
    class GetCompanyTests {
        @Test
        @DisplayName("Should get a company by CNPJ successfully")
        void getCompany_Success() {
            when(companyProvider.findByCnpj(cnpj)).thenReturn(Optional.of(company));

            Company foundCompany = companyService.getCompany(cnpj);

            assertNotNull(foundCompany);
            assertEquals(company, foundCompany);
            verify(companyProvider, times(1)).findByCnpj(cnpj);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException if company not found")
        void getCompany_NotFound_ThrowsException() {
            when(companyProvider.findByCnpj(cnpj)).thenReturn(Optional.empty());

            ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> companyService.getCompany(cnpj));

            assertEquals(COMPANY_NOT_FOUND + cnpj, thrown.getMessage());
            verify(companyProvider, times(1)).findByCnpj(cnpj);
        }
    }

    @Nested
    @DisplayName("List Companies")
    class ListCompaniesTests {
        @Test
        @DisplayName("Should list all companies when active is null")
        void listCompanies_ActiveIsNull_ListsAll() {
            when(companyProvider.findAll()).thenReturn(List.of(company));

            List<Company> companies = companyService.listCompanies(null);

            assertNotNull(companies);
            assertEquals(1, companies.size());
            verify(companyProvider, times(1)).findAll();
            verify(companyProvider, never()).findByActive(anyBoolean());
        }

        @Test
        @DisplayName("Should list active companies when active is true")
        void listCompanies_ActiveIsTrue_ListsActive() {
            when(companyProvider.findByActive(true)).thenReturn(List.of(company));

            List<Company> companies = companyService.listCompanies(true);

            assertNotNull(companies);
            assertEquals(1, companies.size());
            verify(companyProvider, times(1)).findByActive(true);
            verify(companyProvider, never()).findAll();
        }

        @Test
        @DisplayName("Should list inactive companies when active is false")
        void listCompanies_ActiveIsFalse_ListsInactive() {
            when(companyProvider.findByActive(false)).thenReturn(List.of());

            List<Company> companies = companyService.listCompanies(false);

            assertNotNull(companies);
            assertTrue(companies.isEmpty());
            verify(companyProvider, times(1)).findByActive(false);
            verify(companyProvider, never()).findAll();
        }
    }

    @Nested
    @DisplayName("Update Company")
    class UpdateCompanyTests {
        @Test
        @DisplayName("Should update an existing company successfully")
        void updateCompany_Success() {
            when(companyProvider.findByCnpj(cnpj)).thenReturn(Optional.of(company));
            doNothing().when(companyProvider).save(any(Company.class));

            assertDoesNotThrow(() -> companyService.updateCompany(cnpj, updateCompanyRequest));

            verify(companyProvider, times(1)).findByCnpj(cnpj);
            verify(companyProvider, times(1)).save(any(Company.class));
        }

        @Test
        @DisplayName("Should update company with new address when provided")
        void updateCompany_WithNewAddress_Success() {
            UpdateAddressRequest newAddressRequest = new UpdateAddressRequest("87654321", "456");
            UpdateCompanyRequest requestWithAddress = new UpdateCompanyRequest(
                    "Updated Name",
                    "updated_email@test.com",
                    true,
                    newAddressRequest
            );
            Address newAddress = new Address("New Street", "456", "87654321", "New City", "New State");

            when(companyProvider.findByCnpj(cnpj)).thenReturn(Optional.of(company));
            when(viaCep.lookup(newAddressRequest.postalCode())).thenReturn(newAddress);
            doNothing().when(companyProvider).save(any(Company.class));

            assertDoesNotThrow(() -> companyService.updateCompany(cnpj, requestWithAddress));

            verify(companyProvider, times(1)).findByCnpj(cnpj);
            verify(viaCep, times(1)).lookup(newAddressRequest.postalCode());
            verify(companyProvider, times(1)).save(any(Company.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException if company not found for update")
        void updateCompany_NotFound_ThrowsException() {
            when(companyProvider.findByCnpj(cnpj)).thenReturn(Optional.empty());

            ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> companyService.updateCompany(cnpj, updateCompanyRequest));

            assertEquals(COMPANY_NOT_FOUND, thrown.getMessage());
            verify(companyProvider, times(1)).findByCnpj(cnpj);
            verify(companyProvider, never()).save(any(Company.class));
        }
    }

    @Nested
    @DisplayName("Toggle Activate")
    class ToggleActivateTests {
        @Test
        @DisplayName("Should toggle company active status successfully")
        void toggleActivate_Success() {
            when(companyProvider.findByCnpj(cnpj)).thenReturn(Optional.of(company));
            doNothing().when(companyProvider).save(any(Company.class));

            assertDoesNotThrow(() -> companyService.toggleActivate(cnpj));

            verify(companyProvider, times(1)).findByCnpj(cnpj);
            verify(companyProvider, times(1)).save(any(Company.class));
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException if company not found for toggle")
        void toggleActivate_NotFound_ThrowsException() {
            when(companyProvider.findByCnpj(cnpj)).thenReturn(Optional.empty());

            ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> companyService.toggleActivate(cnpj));

            assertEquals(COMPANY_NOT_FOUND + cnpj, thrown.getMessage());
            verify(companyProvider, times(1)).findByCnpj(cnpj);
            verify(companyProvider, never()).save(any(Company.class));
        }
    }

    @Nested
    @DisplayName("Delete Company")
    class DeleteCompanyTests {
        @Test
        @DisplayName("Should delete a company by CNPJ successfully")
        void deleteByCnpj_Success() {
            when(companyProvider.findByCnpj(cnpj)).thenReturn(Optional.of(company));
            doNothing().when(companyProvider).deleteByCnpj(cnpj);

            assertDoesNotThrow(() -> companyService.deleteByCnpj(cnpj));

            verify(companyProvider, times(1)).findByCnpj(cnpj);
            verify(companyProvider, times(1)).deleteByCnpj(cnpj);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException if company not found for deletion")
        void deleteByCnpj_NotFound_ThrowsException() {
            when(companyProvider.findByCnpj(cnpj)).thenReturn(Optional.empty());

            ResourceNotFoundException thrown = assertThrows(ResourceNotFoundException.class, () -> companyService.deleteByCnpj(cnpj));

            assertEquals(COMPANY_NOT_FOUND + cnpj, thrown.getMessage());
            verify(companyProvider, times(1)).findByCnpj(cnpj);
            verify(companyProvider, never()).deleteByCnpj(anyString());
        }
    }
}