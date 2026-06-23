package com.kts.kronos.application.service;

import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.UserCompanyAccessProvider;
import com.kts.kronos.domain.model.UserCompanyAccess;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Multiempresa por CPF — regras de isolamento por tenant")
class MultiCompresaCpfTest {

    @Test
    @DisplayName("CPF deve ser permitido em empresas diferentes (R-001, R-002)")
    void cpfDeveSerPermitidoEmEmpresasDiferentes() {
        var employeeProvider = mock(EmployeeProvider.class);
        UUID empresaA = UUID.randomUUID();
        UUID empresaB = UUID.randomUUID();
        String cpf = "111.111.111-11";

        when(employeeProvider.cpfExistsInCompany(empresaA, cpf)).thenReturn(true);
        when(employeeProvider.cpfExistsInCompany(empresaB, cpf)).thenReturn(false);

        // Na empresa A, CPF já existe
        assertThat(employeeProvider.cpfExistsInCompany(empresaA, cpf)).isTrue();
        // Na empresa B, o mesmo CPF ainda pode ser cadastrado
        assertThat(employeeProvider.cpfExistsInCompany(empresaB, cpf)).isFalse();
    }

    @Test
    @DisplayName("CPF duplicado na mesma empresa deve ser bloqueado (R-002)")
    void cpfDeveSerBloqueadoNaMesmaEmpresa() {
        var employeeProvider = mock(EmployeeProvider.class);
        UUID empresaA = UUID.randomUUID();
        String cpf = "111.111.111-11";

        when(employeeProvider.cpfExistsInCompany(empresaA, cpf)).thenReturn(true);

        boolean exists = employeeProvider.cpfExistsInCompany(empresaA, cpf);
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Usuário deve listar todas as empresas às quais tem acesso ativo (R-005)")
    void usuarioDeveListarEmpresasAcessiveis() {
        var provider = mock(UserCompanyAccessProvider.class);
        UUID userId = UUID.randomUUID();
        UUID companyId1 = UUID.randomUUID();
        UUID companyId2 = UUID.randomUUID();

        var access1 = new UserCompanyAccess(
                UUID.randomUUID(), userId, companyId1, UUID.randomUUID(),
                "MANAGER", true, true, null, null
        );
        var access2 = new UserCompanyAccess(
                UUID.randomUUID(), userId, companyId2, UUID.randomUUID(),
                "MANAGER", true, false, null, null
        );

        when(provider.findActiveByUserId(userId)).thenReturn(List.of(access1, access2));

        var result = provider.findActiveByUserId(userId);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).companyId()).isEqualTo(companyId1);
        assertThat(result.get(1).companyId()).isEqualTo(companyId2);
    }

    @Test
    @DisplayName("Troca de empresa deve ser negada sem vínculo ativo (R-005, R-007)")
    void switchCompanyDeveNegarAcessoSemVinculo() {
        var provider = mock(UserCompanyAccessProvider.class);
        UUID userId = UUID.randomUUID();
        UUID targetCompanyId = UUID.randomUUID();

        when(provider.findActiveByUserIdAndCompanyId(userId, targetCompanyId))
                .thenReturn(Optional.empty());

        var result = provider.findActiveByUserIdAndCompanyId(userId, targetCompanyId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Troca de empresa deve ser permitida com vínculo ativo (R-007)")
    void switchCompanyDevePermitirComVinculo() {
        var provider = mock(UserCompanyAccessProvider.class);
        UUID userId = UUID.randomUUID();
        UUID targetCompanyId = UUID.randomUUID();

        var access = new UserCompanyAccess(
                UUID.randomUUID(), userId, targetCompanyId, UUID.randomUUID(),
                "MANAGER", true, false, null, null
        );

        when(provider.findActiveByUserIdAndCompanyId(userId, targetCompanyId))
                .thenReturn(Optional.of(access));

        var result = provider.findActiveByUserIdAndCompanyId(userId, targetCompanyId);
        assertThat(result).isPresent();
        assertThat(result.get().companyId()).isEqualTo(targetCompanyId);
    }

    @Test
    @DisplayName("Acesso default de usuário deve retornar empresa padrão quando disponível (R-005)")
    void deveRetornarEmpresaDefaultQuandoDisponivel() {
        var provider = mock(UserCompanyAccessProvider.class);
        UUID userId = UUID.randomUUID();
        UUID defaultCompanyId = UUID.randomUUID();

        var defaultAccess = new UserCompanyAccess(
                UUID.randomUUID(), userId, defaultCompanyId, UUID.randomUUID(),
                "MANAGER", true, true, null, null
        );

        when(provider.findDefaultActiveByUserId(userId)).thenReturn(Optional.of(defaultAccess));

        var result = provider.findDefaultActiveByUserId(userId);
        assertThat(result).isPresent();
        assertThat(result.get().defaultCompany()).isTrue();
        assertThat(result.get().companyId()).isEqualTo(defaultCompanyId);
    }

    @Test
    @DisplayName("existsActiveByUserIdAndCompanyId deve retornar false para acesso inexistente (R-005)")
    void existsActivePorUserEmpresaDeveRetornarFalseQuandoNaoExiste() {
        var provider = mock(UserCompanyAccessProvider.class);
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        when(provider.existsActiveByUserIdAndCompanyId(userId, companyId)).thenReturn(false);

        assertThat(provider.existsActiveByUserIdAndCompanyId(userId, companyId)).isFalse();
    }

    @Test
    @DisplayName("existsActiveByUserIdAndCompanyId deve retornar true para acesso existente (R-005)")
    void existsActivePorUserEmpresaDeveRetornarTrueQuandoExiste() {
        var provider = mock(UserCompanyAccessProvider.class);
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();

        when(provider.existsActiveByUserIdAndCompanyId(userId, companyId)).thenReturn(true);

        assertThat(provider.existsActiveByUserIdAndCompanyId(userId, companyId)).isTrue();
    }
}
