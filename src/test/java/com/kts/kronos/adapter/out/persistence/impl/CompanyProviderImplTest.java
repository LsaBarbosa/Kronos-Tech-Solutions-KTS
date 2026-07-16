package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.out.persistence.CompanyRepository;
import com.kts.kronos.adapter.out.persistence.entity.AddressEmbeddable;
import com.kts.kronos.adapter.out.persistence.entity.CompanyEntity;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyProviderImplTest {

    @Mock
    private CompanyRepository repository;

    @InjectMocks
    private CompanyProviderImpl provider;

    @Test
    void deveBuscarPorCnpjMapeandoParaDominio() {
        when(repository.findByCnpj("12345678000199")).thenReturn(Optional.of(companyEntity(true)));

        var result = provider.findByCnpj("12345678000199");

        assertEquals("KTS", result.orElseThrow().name());
    }

    @Test
    void deveVerificarExistenciaPorCnpj() {
        when(repository.existsByCnpj("12345678000199")).thenReturn(true);

        assertTrue(provider.existsByCnpj("12345678000199"));

        verify(repository).existsByCnpj("12345678000199");
    }

    @Test
    void deveListarTodasAsEmpresasMapeandoParaDominio() {
        when(repository.findAll()).thenReturn(List.of(companyEntity(true), companyEntity(false)));

        var result = provider.findAll();

        assertEquals(2, result.size());
        assertEquals("KTS", result.getFirst().name());
    }

    @Test
    void deveBuscarPorIdMapeandoParaDominio() {
        var entity = companyEntity(true);
        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));

        var result = provider.findById(entity.getId());

        assertTrue(result.isPresent());
        assertEquals(entity.getId(), result.get().companyId());
    }

    @Test
    void deveEscolherRepositorioCorretoNoFindByActive() {
        when(repository.findByActiveTrue()).thenReturn(List.of(companyEntity(true)));
        when(repository.findByActiveFalse()).thenReturn(List.of(companyEntity(false)));

        assertEquals(1, provider.findByActive(true).size());
        assertEquals(1, provider.findByActive(false).size());

        verify(repository).findByActiveTrue();
        verify(repository).findByActiveFalse();
    }

    @Test
    void deveSalvarEmpresaConvertendoEntidade() {
        var company = new Company(
                UUID.randomUUID(),
                "KTS",
                "12345678000199",
                "contato@kts.com",
                true,
                new Address("Rua A", "10", "65000000", "São Luís", "MA"),
                new Location(-2.53, -44.30),
                0,
                0
        );

        when(repository.save(org.mockito.ArgumentMatchers.any(CompanyEntity.class)))
                .thenReturn(companyEntity(true));

        provider.save(company);

        verify(repository).save(org.mockito.ArgumentMatchers.any(CompanyEntity.class));
    }

    @Test
    void deveConverterDominioParaEntityPeloHelperLegado() throws Exception {
        var company = new Company(
                UUID.randomUUID(),
                "KTS",
                "12345678000199",
                "contato@kts.com",
                true,
                new Address("Rua A", "10", "65000000", "São Luís", "MA"),
                new Location(-2.53, -44.30),
                0,
                0
        );
        var method = CompanyProviderImpl.class.getDeclaredMethod("toEntity", Company.class);
        method.setAccessible(true);

        CompanyEntity entity = (CompanyEntity) method.invoke(provider, company);

        assertEquals(company.companyId(), entity.getId());
        assertEquals(company.name(), entity.getName());
        assertEquals(company.cnpj(), entity.getCnpj());
        assertEquals(company.email(), entity.getEmail());
        assertEquals(company.location().latitude(), entity.getLatitude());
        assertEquals(company.location().longitude(), entity.getLongitude());
        assertEquals(company.address().street(), entity.getAddress().getStreet());
    }

    @Test
    void deveExcluirPorCnpjDelegandoParaRepository() {
        provider.deleteByCnpj("12345678000199");

        verify(repository).deleteByCnpj("12345678000199");
    }

    @Test
    void isSandbox_delegatesParaRepository() {
        UUID companyId = UUID.randomUUID();
        when(repository.existsByIdAndSandboxTrue(companyId)).thenReturn(true);
        assertTrue(provider.isSandbox(companyId));
        verify(repository).existsByIdAndSandboxTrue(companyId);
    }

    private static CompanyEntity companyEntity(boolean active) {
        return CompanyEntity.builder()
                .id(UUID.randomUUID())
                .name("KTS")
                .cnpj("12345678000199")
                .email("contato@kts.com")
                .active(active)
                .address(AddressEmbeddable.builder()
                        .street("Rua A")
                        .number("10")
                        .postalCode("65000000")
                        .city("São Luís")
                        .state("MA")
                        .build())
                .latitude(-2.53)
                .longitude(-44.30)
                .build();
    }
}
