package com.kts.kronos.adapter.out.security;

import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityUserMapperTest {

    @Test
    void shouldMapDomainUserToSpringUserDetails() {
        var user = new User(UUID.randomUUID(), "john", "hash", Role.MANAGER, true, UUID.randomUUID());

        var userDetails = SecurityUserMapper.toSpringUser(user);

        assertThat(userDetails.getUsername()).isEqualTo("john");
        assertThat(userDetails.getPassword()).isEqualTo("hash");
        assertThat(userDetails.getAuthorities()).extracting("authority").contains("ROLE_MANAGER");
        assertThat(userDetails.isEnabled()).isTrue();
    }
}
