package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.config.chat.TawkChatProperties;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class SupportChatServiceTest {

    private TawkChatProperties props;
    private JwtAuthenticatedUser jwtAuthenticatedUser;
    private EmployeeProvider employeeProvider;
    private CompanyProvider companyProvider;
    private SupportChatService service;

    @BeforeEach
    void setUp() {
        props = mock(TawkChatProperties.class);
        jwtAuthenticatedUser = mock(JwtAuthenticatedUser.class);
        employeeProvider = mock(EmployeeProvider.class);
        companyProvider = mock(CompanyProvider.class);
        service = new SupportChatService(props, jwtAuthenticatedUser, employeeProvider, companyProvider);
        ReflectionTestUtils.setField(service, "activeProfile", "test");
    }

    private Employee mockEmployee(UUID employeeId, UUID companyId, String name, String email) {
        var employee = mock(Employee.class);
        when(employee.fullName()).thenReturn(name);
        when(employee.email()).thenReturn(email);
        when(employee.companyId()).thenReturn(companyId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        return employee;
    }

    private Company mockCompany(UUID companyId, String name) {
        var company = mock(Company.class);
        when(company.name()).thenReturn(name);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        return company;
    }

    private void stubCommonProps() {
        when(props.getSecureKey()).thenReturn("test-secret-key");
        when(props.getIdentityTtlSeconds()).thenReturn(3600L);
        when(props.getMaxMetadataEntries()).thenReturn(20);
        when(props.getMaxValueLength()).thenReturn(255);
    }

    // -------------------------------------------------------------------------
    // getConfig
    // -------------------------------------------------------------------------

    @Test
    void getConfig_whenDisabled_returnsEnabledFalse() {
        when(props.isEnabled()).thenReturn(false);
        var result = service.getConfig();
        assertThat(result.enabled()).isFalse();
        assertThat(result.propertyId()).isEmpty();
        assertThat(result.widgetId()).isEmpty();
    }

    @Test
    void getConfig_whenEnabled_returnsCorrectIds() {
        when(props.isEnabled()).thenReturn(true);
        when(props.getPropertyId()).thenReturn("prop-123");
        when(props.getWidgetId()).thenReturn("widget-456");
        var result = service.getConfig();
        assertThat(result.enabled()).isTrue();
        assertThat(result.propertyId()).isEqualTo("prop-123");
        assertThat(result.widgetId()).isEqualTo("widget-456");
    }

    // -------------------------------------------------------------------------
    // getIdentity — userId isolation
    // -------------------------------------------------------------------------

    @Test
    void getIdentity_returnsUniqueUserIdPerUser() {
        stubCommonProps();
        var employeeId = UUID.randomUUID();
        var companyId = UUID.randomUUID();
        var userAId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
        var userBId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

        mockEmployee(employeeId, companyId, "Alice", "alice@kts.com");
        mockCompany(companyId, "Empresa X");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);

        when(jwtAuthenticatedUser.getuserId()).thenReturn(userAId);
        var identityA = service.getIdentity();

        when(jwtAuthenticatedUser.getuserId()).thenReturn(userBId);
        var identityB = service.getIdentity();

        assertThat(identityA.userId()).isEqualTo(userAId.toString());
        assertThat(identityB.userId()).isEqualTo(userBId.toString());
        assertThat(identityA.userId()).isNotEqualTo(identityB.userId());
        assertThat(identityA.hash()).isNotEqualTo(identityB.hash());
    }

    @Test
    void getIdentity_usersInSameCompany_haveDistinctUserIds() {
        stubCommonProps();
        var companyId = UUID.randomUUID();
        var empAId = UUID.randomUUID();
        var empBId = UUID.randomUUID();
        var userAId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
        var userBId = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

        mockCompany(companyId, "Same Company");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);

        var empA = mock(Employee.class);
        when(empA.fullName()).thenReturn("Alice");
        when(empA.email()).thenReturn("alice@kts.com");
        when(empA.companyId()).thenReturn(companyId);
        when(employeeProvider.findById(empAId)).thenReturn(Optional.of(empA));

        var empB = mock(Employee.class);
        when(empB.fullName()).thenReturn("Bob");
        when(empB.email()).thenReturn("bob@kts.com");
        when(empB.companyId()).thenReturn(companyId);
        when(employeeProvider.findById(empBId)).thenReturn(Optional.of(empB));

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(empAId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userAId);
        var identityA = service.getIdentity();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(empBId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userBId);
        var identityB = service.getIdentity();

        assertThat(identityA.userId()).isNotEqualTo(identityB.userId());
        assertThat(identityA.hash()).isNotEqualTo(identityB.hash());
    }

    // -------------------------------------------------------------------------
    // getIdentity — attributes
    // -------------------------------------------------------------------------

    @Test
    void getIdentity_attributesContainExpectedFields() {
        stubCommonProps();
        var userId = UUID.randomUUID();
        var employeeId = UUID.randomUUID();
        var companyId = UUID.randomUUID();

        mockEmployee(employeeId, companyId, "Lucas", "lucas@kts.com");
        mockCompany(companyId, "Kronos Corp");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);

        var result = service.getIdentity();

        assertThat(result.attributes()).containsKey("user-id");
        assertThat(result.attributes()).containsKey("user-role");
        assertThat(result.attributes()).containsKey("company-id");
        assertThat(result.attributes()).containsKey("company-name");
        assertThat(result.attributes()).containsKey("environment");
        assertThat(result.attributes().get("user-id")).isEqualTo(userId.toString());
        assertThat(result.attributes().get("user-role")).isEqualTo("manager");
        assertThat(result.attributes().get("company-id")).isEqualTo(companyId.toString());
        assertThat(result.attributes().get("company-name")).isEqualTo("Kronos Corp");
    }

    @Test
    void getIdentity_attributesDoNotContainSecrets() {
        stubCommonProps();
        var userId = UUID.randomUUID();
        var employeeId = UUID.randomUUID();
        var companyId = UUID.randomUUID();
        var secureKey = "SENSITIVE_SECURE_KEY_MUST_NOT_LEAK";
        var webhookSecret = "SENSITIVE_WEBHOOK_SECRET";

        when(props.getSecureKey()).thenReturn(secureKey);
        when(props.getIdentityTtlSeconds()).thenReturn(3600L);
        when(props.getMaxMetadataEntries()).thenReturn(20);
        when(props.getMaxValueLength()).thenReturn(255);

        mockEmployee(employeeId, companyId, "User", "user@kts.com");
        mockCompany(companyId, "Company");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);

        var result = service.getIdentity();

        var allValues = new StringBuilder();
        result.attributes().values().forEach(allValues::append);

        assertThat(allValues.toString()).doesNotContain(secureKey);
        assertThat(allValues.toString()).doesNotContain(webhookSecret);
        assertThat(result.hash()).doesNotContain(secureKey);
    }

    @Test
    void getIdentity_attributesAreTruncatedToMaxLength() {
        when(props.getSecureKey()).thenReturn("key");
        when(props.getIdentityTtlSeconds()).thenReturn(3600L);
        when(props.getMaxMetadataEntries()).thenReturn(20);
        when(props.getMaxValueLength()).thenReturn(10);

        var userId = UUID.randomUUID();
        var employeeId = UUID.randomUUID();
        var companyId = UUID.randomUUID();

        mockEmployee(employeeId, companyId, "Lucas", "lucas@kts.com");
        var company = mock(Company.class);
        when(company.name()).thenReturn("A very long company name that exceeds limit");
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);

        var result = service.getIdentity();

        result.attributes().values().forEach(value ->
                assertThat(value.length()).isLessThanOrEqualTo(10)
        );
    }

    @Test
    void getIdentity_attributesAreNullSafe_whenCompanyNotFound() {
        stubCommonProps();
        var userId = UUID.randomUUID();
        var employeeId = UUID.randomUUID();
        var companyId = UUID.randomUUID();

        mockEmployee(employeeId, companyId, "User", "user@kts.com");
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);

        var result = service.getIdentity();

        assertThat(result.attributes()).doesNotContainKey("company-name");
        assertThat(result).isNotNull();
    }

    // -------------------------------------------------------------------------
    // getIdentity — tags
    // -------------------------------------------------------------------------

    @Test
    void getIdentity_tagsContainKronosAndRole() {
        stubCommonProps();
        var userId = UUID.randomUUID();
        var employeeId = UUID.randomUUID();
        var companyId = UUID.randomUUID();

        mockEmployee(employeeId, companyId, "User", "user@kts.com");
        mockCompany(companyId, "Company");
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);

        var result = service.getIdentity();

        assertThat(result.tags()).contains("kronos", "cliente-logado", "manager");
        assertThat(result.tags().size()).isLessThanOrEqualTo(10);
    }

    // -------------------------------------------------------------------------
    // handleWebhook
    // -------------------------------------------------------------------------

    @Test
    void handleWebhook_rejectsOversizedPayload() {
        when(props.getMaxWebhookPayloadCharacters()).thenReturn(10);
        assertThrows(com.kts.kronos.application.exceptions.ForbiddenException.class,
                () -> service.handleWebhook("x".repeat(11), "sha256=anything"));
    }

    @Test
    void handleWebhook_rejectsNullPayload() {
        when(props.getMaxWebhookPayloadCharacters()).thenReturn(8192);
        assertThrows(com.kts.kronos.application.exceptions.ForbiddenException.class,
                () -> service.handleWebhook(null, "sha256=anything"));
    }

    @Test
    void handleWebhook_rejectsInvalidSignature() {
        when(props.getMaxWebhookPayloadCharacters()).thenReturn(8192);
        when(props.getWebhookSecret()).thenReturn("secret");
        assertThrows(com.kts.kronos.application.exceptions.ForbiddenException.class,
                () -> service.handleWebhook("{\"event\":\"test\"}", "sha256=invalidsignature"));
    }
}
