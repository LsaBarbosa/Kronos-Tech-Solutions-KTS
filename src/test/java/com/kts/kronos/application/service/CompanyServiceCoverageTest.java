package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import com.kts.kronos.adapter.in.web.dto.company.CompanyHardDeleteResultDTO;
import com.kts.kronos.adapter.in.web.dto.company.CompanyResponse;
import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.CacheProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CompanyServiceCoverageTest {

    @Mock private CompanyProvider companyProvider;
    @Mock private AddressLookupProvider viaCep;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private UserProvider userProvider;
    @Mock private UserUseCase userUseCase;
    @Mock private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock private AuthenticationRateLimitService authenticationRateLimitService;
    @Mock private KronosMetrics kronosMetrics;
    @Mock private CacheProvider cacheProvider;
    @Mock private CompanyHardDeleteService companyHardDeleteService;

    private CompanyService service() {
        return new CompanyService(companyProvider, viaCep, employeeProvider, userProvider, userUseCase,
                jwtAuthenticatedUser, authenticationRateLimitService, kronosMetrics,
                cacheProvider, companyHardDeleteService);
    }

    private CompanyService serviceNullCache() {
        return new CompanyService(companyProvider, viaCep, employeeProvider, userProvider, userUseCase,
                jwtAuthenticatedUser, authenticationRateLimitService, kronosMetrics,
                null, companyHardDeleteService);
    }

    private Company company(UUID companyId) {
        return new Company(
                companyId, "KTS", "12345678000199", "kts@kts.com", true,
                new Address("Rua A", "100", "01001000", "São Paulo", "SP"),
                new Location(-23.55, -46.63),
                0L, 0L
        );
    }

    // ── cache() — cacheProvider nulo → carrega direto do loader ──────────────

    @Test
    @DisplayName("getCompanyResponse: cacheProvider nulo → carrega direto do provider (cache null branch)")
    void getCompanyResponse_withNullCacheProvider_callsLoaderDirectly() {
        var companyId = UUID.randomUUID();
        var co = company(companyId);
        when(companyProvider.findByCnpj("12345678000199")).thenReturn(Optional.of(co));
        when(employeeProvider.countByCompanyIds(any())).thenReturn(List.of());

        var result = serviceNullCache().getCompanyResponse("12345678000199");

        assertNotNull(result);
        verify(cacheProvider, never()).getOrLoad(any(), any(), any(), any());
    }

    // ── cache() — cacheProvider presente → delega a getOrLoad ────────────────

    @Test
    @DisplayName("getCompanyResponse: cacheProvider presente → delega a cacheProvider.getOrLoad")
    @SuppressWarnings("unchecked")
    void getCompanyResponse_withCacheProvider_delegatesToGetOrLoad() {
        var companyId = UUID.randomUUID();
        var co = company(companyId);
        CompanyResponse mockResponse = CompanyResponse.fromDomain(co);
        when(cacheProvider.getOrLoad(any(), any(), any(), any())).thenReturn(mockResponse);

        var result = service().getCompanyResponse("12345678000199");

        assertSame(mockResponse, result);
        verify(cacheProvider).getOrLoad(any(), any(), any(), any());
    }

    // ── updateCompany — latitude nula lança BadRequestException ──────────────

    @Test
    @DisplayName("updateCompany: address presente, location presente mas latitude nula → lança BadRequestException")
    void updateCompany_latitudeNull_throws() {
        var co = company(UUID.randomUUID());
        when(companyProvider.findByCnpj(co.cnpj())).thenReturn(Optional.of(co));

        var request = new UpdateCompanyRequest(null, null, null,
                new UpdateAddressRequest("01001000", "500"),
                new Location(null, -43.0));

        assertThrows(BadRequestException.class, () -> service().updateCompany(co.cnpj(), request));
        verify(companyProvider, never()).save(any());
    }

    // ── updateCompany — longitude nula lança BadRequestException ─────────────

    @Test
    @DisplayName("updateCompany: address presente, location presente mas longitude nula → lança BadRequestException")
    void updateCompany_longitudeNull_throws() {
        var co = company(UUID.randomUUID());
        when(companyProvider.findByCnpj(co.cnpj())).thenReturn(Optional.of(co));

        var request = new UpdateCompanyRequest(null, null, null,
                new UpdateAddressRequest("01001000", "500"),
                new Location(-22.9, null));

        assertThrows(BadRequestException.class, () -> service().updateCompany(co.cnpj(), request));
        verify(companyProvider, never()).save(any());
    }

    // ── toggleTerminalFlag ────────────────────────────────────────────────────

    @Test
    @DisplayName("toggleTerminalFlag: inverte terminalFlag, salva empresa e invalida caches")
    void toggleTerminalFlag_flipsFlag() {
        var co = company(UUID.randomUUID());
        when(companyProvider.findByCnpj(co.cnpj())).thenReturn(Optional.of(co));

        service().toggleTerminalFlag(co.cnpj());

        verify(companyProvider).save(argThat(saved -> saved.terminalFlag() != co.terminalFlag()));
        verify(cacheProvider, atLeastOnce()).evictNamespace(any());
    }

    @Test
    @DisplayName("toggleTerminalFlag: CNPJ inexistente → ResourceNotFoundException")
    void toggleTerminalFlag_cnpjNotFound_throws() {
        when(companyProvider.findByCnpj("00000000000000")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service().toggleTerminalFlag("00000000000000"));
    }

    @Test
    @DisplayName("toggleTerminalFlag: empresa com terminalFlag=true → toggle para false (cobre BR L157 outro ramo)")
    void toggleTerminalFlag_fromTrueToFalse() {
        var co = new Company(
                UUID.randomUUID(), "KTS", "12345678000199", "kts@kts.com", true,
                new Address("Rua A", "100", "01001000", "São Paulo", "SP"),
                new Location(-23.55, -46.63),
                0L, 0L, null, null, null, true
        );
        when(companyProvider.findByCnpj(co.cnpj())).thenReturn(Optional.of(co));
        service().toggleTerminalFlag(co.cnpj());
        verify(companyProvider).save(argThat(saved -> !saved.terminalFlag()));
    }

    // ── deleteByCnpj — empresa sem funcionários retorna cedo ─────────────────

    @Test
    @DisplayName("deleteByCnpj: sem funcionários → retorna cedo após salvar empresa inativa")
    void deleteByCnpj_noEmployees_returnsEarly() {
        var co = company(UUID.randomUUID());
        when(companyProvider.findByCnpj(co.cnpj())).thenReturn(Optional.of(co));
        when(employeeProvider.countByCompanyIds(any())).thenReturn(List.of());
        when(employeeProvider.findByCompanyId(co.companyId())).thenReturn(List.of());

        service().deleteByCnpj(co.cnpj());

        verify(companyProvider).save(argThat(saved -> !saved.active()));
        verify(userProvider, never()).findByEmployeeIds(any());
        verify(cacheProvider, atLeastOnce()).evictNamespace(any());
    }

    // ── hardDeleteCompany ─────────────────────────────────────────────────────

    @Test
    @DisplayName("hardDeleteCompany: delega ao serviço e retorna resultado")
    void hardDeleteCompany_returnsResultFromService() {
        var co = company(UUID.randomUUID());
        var dto = new CompanyHardDeleteResultDTO("12345678000199", "KTS", 0, 0, List.of());
        when(companyProvider.findByCnpj(co.cnpj())).thenReturn(Optional.of(co));
        when(companyHardDeleteService.hardDelete(co.companyId(), co.cnpj(), co.name())).thenReturn(dto);

        var result = service().hardDeleteCompany(co.cnpj());

        assertSame(dto, result);
        verify(cacheProvider, atLeastOnce()).evictNamespace(any());
    }

    @Test
    @DisplayName("hardDeleteCompany: CNPJ inexistente → ResourceNotFoundException")
    void hardDeleteCompany_cnpjNotFound_throws() {
        when(companyProvider.findByCnpj("00000000000000")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service().hardDeleteCompany("00000000000000"));
    }

    // ── currentUserIdOrNull — catch RuntimeException retorna null ─────────────

    @Test
    @DisplayName("currentUserIdOrNull: getuserId lança RuntimeException → swallowed, deleteByCnpj conclui")
    void deleteByCnpj_currentUserIdThrows_swallowsException() {
        var co = company(UUID.randomUUID());
        when(companyProvider.findByCnpj(co.cnpj())).thenReturn(Optional.of(co));
        when(employeeProvider.countByCompanyIds(any())).thenReturn(List.of());
        when(employeeProvider.findByCompanyId(co.companyId())).thenReturn(List.of());
        when(jwtAuthenticatedUser.getuserId()).thenThrow(new RuntimeException("Sem contexto de autenticação"));

        assertDoesNotThrow(() -> service().deleteByCnpj(co.cnpj()));
        verify(companyProvider).save(argThat(saved -> !saved.active()));
    }

    // ── invalidateCompanyCaches — cacheProvider nulo retorna cedo ────────────

    @Test
    @DisplayName("invalidateCompanyCaches: cacheProvider nulo → skipa invalidação sem erro")
    void invalidateCompanyCaches_nullCacheProvider_doesNothing() {
        var co = company(UUID.randomUUID());
        when(companyProvider.findByCnpj(co.cnpj())).thenReturn(Optional.of(co));

        assertDoesNotThrow(() -> serviceNullCache().toggleTerminalFlag(co.cnpj()));
        verify(cacheProvider, never()).evictNamespace(any());
    }

    // ── invalidateCompanyCaches — catch RuntimeException swallows ────────────

    @Test
    @DisplayName("invalidateCompanyCaches: evictNamespace lança RuntimeException → logado e engolido")
    void invalidateCompanyCaches_evictThrows_doesNotPropagate() {
        var co = company(UUID.randomUUID());
        when(companyProvider.findByCnpj(co.cnpj())).thenReturn(Optional.of(co));
        doThrow(new RuntimeException("Redis indisponível")).when(cacheProvider).evictNamespace(any());

        assertDoesNotThrow(() -> service().toggleTerminalFlag(co.cnpj()));
    }
}
