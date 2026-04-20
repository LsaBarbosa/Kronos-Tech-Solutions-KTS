package com.kts.kronos.adapter.out.security;

import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.lang.reflect.Constructor;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityUserMapperTest {

    @Test
    @DisplayName("toSpringUser: deve mapear credenciais, role e status ativo")
    void shouldMapActiveDomainUserToSpringUser() {
        User domain = new User(
                UUID.randomUUID(),
                "manager",
                "encoded-password",
                Role.MANAGER,
                true,
                UUID.randomUUID()
        );

        var userDetails = SecurityUserMapper.toSpringUser(domain);

        assertEquals("manager", userDetails.getUsername());
        assertEquals("encoded-password", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_MANAGER")));
    }

    @Test
    @DisplayName("toSpringUser: deve desabilitar usuario inativo")
    void shouldDisableInactiveDomainUser() {
        User domain = new User(
                UUID.randomUUID(),
                "partner",
                "encoded-password",
                Role.PARTNER,
                false,
                UUID.randomUUID()
        );

        var userDetails = SecurityUserMapper.toSpringUser(domain);

        assertFalse(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_PARTNER")));
    }

    @Test
    @DisplayName("construtor privado: deve existir apenas para impedir instanciacao publica")
    void shouldKeepPrivateConstructor() throws Exception {
        Constructor<SecurityUserMapper> constructor = SecurityUserMapper.class.getDeclaredConstructor();

        assertFalse(constructor.canAccess(null));
        constructor.setAccessible(true);
        constructor.newInstance();
    }
}
