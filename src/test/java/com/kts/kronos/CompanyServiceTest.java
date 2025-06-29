package com.kts.kronos;


import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyCommand;
import com.kts.kronos.app.exceptions.BadRequestException;
import com.kts.kronos.app.exceptions.ResourceNotFoundException;
import com.kts.kronos.app.port.out.repository.AddressLookupPort;
import com.kts.kronos.app.port.out.repository.CompanyRepository;
import com.kts.kronos.app.service.CompanyService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompanyServiceTest {
    @Mock
    private CompanyRepository repository;

    @InjectMocks
    private CompanyService service;
    @Mock
    private AddressLookupPort viaCep;
    private UUID companyId;
    private String name;
    private String cnpj;
    private String email;
    private Company existingCompany;
    private Address existingAddress;


    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        name = "Acme Corp";
        cnpj = "12345678901234";
        email = "acme@example.com";
        existingAddress = new Address("Old St", "10", "87654321", "OldCity", "OldState");
        existingCompany = new Company(companyId, name, cnpj, email, true, existingAddress);
    }

    @Test
    void createCompany_success() {
        String postalCode = "12345678";
        String number     = "100";
        var addrReq = new AddressRequest(postalCode, number);
        var req = new CreateCompanyRequest(name, cnpj, email, addrReq);

        when(repository.findByCnpj(cnpj)).thenReturn(Optional.empty());
        // stub do lookup viaCep
        var lookupAddr = new Address("Street Name", null, postalCode, "City", "State");
        when(viaCep.lookup(postalCode)).thenReturn(lookupAddr);
        // stub do save final, retornando a company já salva
        var saved = new Company(companyId, name, cnpj, email, true, lookupAddr.withNumber(number));
        when(repository.save(any(Company.class))).thenReturn(saved);

        // executa o create
        service.createCompany(req);

        // valida que procurou por CNPJ e lookup de CEP
        verify(repository).findByCnpj(cnpj);
        verify(viaCep).lookup(postalCode);

        // captura o objeto passado para save
        var captor = ArgumentCaptor.forClass(Company.class);
        verify(repository).save(captor.capture());
        Company toSave = captor.getValue();

        assertEquals(name, toSave.name());
        assertTrue(toSave.active());
        assertEquals("Street Name", toSave.address().street());
        assertEquals(number, toSave.address().number());
        assertEquals(postalCode, toSave.address().postalCode());
    }

    @Test
    void createCompany_duplicate_throws() {
        String postalCode = "12345678";
        String number     = "100";
        var addrReq = new AddressRequest(postalCode, number);
        var req = new CreateCompanyRequest(name, cnpj, email, addrReq);

        when(repository.findByCnpj(cnpj)).thenReturn(Optional.of(existingCompany));

        assertThrows(BadRequestException.class, () -> service.createCompany(req));

        // não deve chamar nem save nem lookup
        verify(viaCep, never()).lookup(anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void getCompany_found() {
        when(repository.findByCnpj(cnpj)).thenReturn(Optional.of(existingCompany));

        Company result = service.getCompany(cnpj);

        assertEquals(existingCompany, result);
    }

    @Test
    void getCompany_notFound_throws() {
        when(repository.findByCnpj(cnpj)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getCompany(cnpj));
    }

    @Test
    void listCompanies_Actived_nullActive_returnsAll() {
        var list = List.of(existingCompany);
        when(repository.findAll()).thenReturn(list);

        List<Company> result = service.listCompanies(null);

        assertEquals(list, result);
    }

    @Test
    void listCompanies_Actived_activeTrue() {
        var activeList = List.of(existingCompany);
        when(repository.findByActive(true)).thenReturn(activeList);

        List<Company> result = service.listCompanies(true);

        assertEquals(activeList, result);
    }

    @Test
    void listCompanies_Actived_activeFalse() {
        var inactive = new Company(companyId, name, cnpj, email, false, existingAddress);
        when(repository.findByActive(false)).thenReturn(List.of(inactive));

        List<Company> result = service.listCompanies(false);

        assertEquals(List.of(inactive), result);
    }

    @Test
    void updateCompany_success() {
        var cmd = new UpdateCompanyCommand("NewName", null, null, null);
        when(repository.findByCnpj(cnpj)).thenReturn(Optional.of(existingCompany));
        var updatedDomain = new Company(companyId, "NewName", cnpj, email, true, existingAddress);
        when(repository.save(any(Company.class))).thenReturn(updatedDomain);

        service.updateCompany(cnpj, cmd);

        verify(repository).save(any(Company.class));
        // como não forneceu address no comando, não deve chamar lookup
        verify(viaCep, never()).lookup(anyString());
    }

    @Test
    void updateCompany_notFound_throws() {
        var cmd = new UpdateCompanyCommand("NewName", null, null, null);
        when(repository.findByCnpj(cnpj)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateCompany(cnpj, cmd));
    }


    @Test
    void deactivateCompany_success() {
        when(repository.findByCnpj(cnpj)).thenReturn(Optional.of(existingCompany));

        service.deactivateCompany(cnpj);

        var captor = ArgumentCaptor.forClass(Company.class);
        verify(repository).save(captor.capture());
        Company saved = captor.getValue();
        assertFalse(saved.active());
    }

    @Test
    void deleteByCnpj_success() {
        when(repository.findByCnpj(cnpj)).thenReturn(Optional.of(existingCompany));

        service.deleteByCnpj(cnpj);

        verify(repository).deleteByCnpj(cnpj);
    }

    @Test
    void deleteByCnpj_notFound_throws() {
        when(repository.findByCnpj(cnpj)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteByCnpj(cnpj));
    }
}
