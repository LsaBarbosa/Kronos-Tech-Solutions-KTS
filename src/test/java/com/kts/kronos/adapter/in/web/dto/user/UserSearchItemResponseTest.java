package com.kts.kronos.adapter.in.web.dto.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserSearchItemResponseTest {

    @Test
    void fromDomain_shouldMapEmployeeIdSoFrontendCanLinkAccounts() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID employeeId = UUID.fromString("00000000-0000-0000-0000-000000000aaa");

        User user = new User(
                userId,
                "maria.silva",
                "secret-hash",
                Role.MANAGER,
                true,
                employeeId
        );

        UserSearchItemResponse response = UserSearchItemResponse.fromDomain(user, true);

        assertEquals(userId, response.userId());
        assertEquals(employeeId, response.employeeId());
        assertEquals("maria.silva", response.username());
        assertEquals("MANAGER", response.role());
        assertTrue(response.active());
        assertTrue(response.biometricConsentAccepted());
    }

    @Test
    void fromDomain_shouldPropagateBiometricConsentAcceptedFalseWhenMissing() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID employeeId = UUID.fromString("00000000-0000-0000-0000-000000000ccc");

        User user = new User(
                userId,
                "ana.lima",
                "secret-hash",
                Role.PARTNER,
                true,
                employeeId
        );

        UserSearchItemResponse response = UserSearchItemResponse.fromDomain(user, false);

        assertFalse(response.biometricConsentAccepted());
    }

    @Test
    void shouldNotExposeSensitiveDomainFields() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID employeeId = UUID.fromString("00000000-0000-0000-0000-000000000bbb");

        User user = new User(
                userId,
                "joao.souza",
                "secret-hash",
                Role.PARTNER,
                true,
                employeeId
        );

        UserSearchItemResponse response = UserSearchItemResponse.fromDomain(user, false);
        String json = new ObjectMapper().writeValueAsString(response);

        assertFalse(json.contains("password"), "DTO resumido não deve serializar password");
        assertFalse(json.contains("sessionVersion"), "DTO resumido não deve serializar sessionVersion");
        assertFalse(json.contains("deletedAt"), "DTO resumido não deve serializar deletedAt");
        assertFalse(json.contains("deletedBy"), "DTO resumido não deve serializar deletedBy");
        assertFalse(json.contains("deactivationReason"), "DTO resumido não deve serializar deactivationReason");
        assertTrue(json.contains("\"employeeId\""), "DTO resumido deve expor employeeId para vínculo no front");
        assertTrue(json.contains("\"biometricConsentAccepted\""), "DTO resumido deve expor biometricConsentAccepted para filtros");
    }
}
