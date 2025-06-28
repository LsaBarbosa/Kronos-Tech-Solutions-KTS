package com.kts.kronos;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.dto.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.UpdateCompanyCommand;
import com.kts.kronos.app.exceptions.BadRequestException;
import com.kts.kronos.app.exceptions.ResourceNotFoundException;
import com.kts.kronos.app.port.in.usecase.CompanyUseCase;
import com.kts.kronos.app.port.out.repository.CompanyRepository;
import com.kts.kronos.app.service.CompanyService;
import com.kts.kronos.domain.model.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class CompanyServiceTest {
    @Mock
    private CompanyRepository repository;

    @InjectMocks
    private CompanyService service;

    private UUID companyId;
    private UUID addressId;
    private String name;
    private String cnpj;
    private String email;
    private Company existingCompany;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        name = "Acme Corp";
        cnpj = "12345678901234";
        email = "acme@example.com";
        existingCompany = new Company(companyId, name, cnpj, email, true, addressId);
    }

    @Test
    void createCompany_success() {
        var req = new CreateCompanyRequest(name, cnpj, email, addressId);
        when(repository.findByCnpj(cnpj)).thenReturn(Optional.empty());
        var saved = new Company(companyId, name, cnpj, email, true, addressId);
        when(repository.save(any(Company.class))).thenReturn(saved);

         service.createCompany(req);
        verify(repository).findByCnpj(cnpj);

        var captor = ArgumentCaptor.forClass(Company.class);
        verify(repository).save(captor.capture());
        Company toSave = captor.getValue();
        assertEquals(name, toSave.name());
        assertTrue(toSave.active());
    }

    @Test
    void createCompany_duplicate_throws() {
        var req = new CreateCompanyRequest(name, cnpj, email, addressId);
        when(repository.findByCnpj(cnpj)).thenReturn(Optional.of(existingCompany));

        assertThrows(BadRequestException.class, () -> service.createCompany(req));
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
    void listCompanies_nullActive_returnsAll() {
        var list = List.of(existingCompany);
        when(repository.findAll()).thenReturn(list);

        List<Company> result = service.listCompanies(null);

        assertEquals(list, result);
    }

    @Test
    void listCompanies_activeTrue() {
        var activeList = List.of(existingCompany);
        when(repository.findByActive(true)).thenReturn(activeList);

        List<Company> result = service.listCompanies(true);

        assertEquals(activeList, result);
    }

    @Test
    void listCompanies_activeFalse() {
        var inactive = new Company(companyId, name, cnpj, email, false, addressId);
        when(repository.findByActive(false)).thenReturn(List.of(inactive));

        List<Company> result = service.listCompanies(false);

        assertEquals(List.of(inactive), result);
    }

    @Test
    void updateCompany_success() {
        var cmd = new UpdateCompanyCommand("NewName", null, null, null);
        when(repository.findByCnpj(cnpj)).thenReturn(Optional.of(existingCompany));
        var updatedDomain = new Company(companyId, "NewName", cnpj, email, true, addressId);
        when(repository.save(any(Company.class))).thenReturn(updatedDomain);

         service.updateCompany(cnpj, cmd);
        verify(repository).save(any(Company.class));
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
