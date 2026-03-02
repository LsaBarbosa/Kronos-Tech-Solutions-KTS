package com.kts.kronos.adapter.out.security;

import com.kts.kronos.adapter.out.persistence.UserRepository;
import com.kts.kronos.adapter.out.persistence.entity.UserEntity;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    void shouldLoadNormalizedUsernameSuccessfully() {
        var entity = UserEntity.builder()
                .userId(UUID.randomUUID())
                .username("john")
                .password("hash")
                .role(Role.CTO)
                .active(true)
                .employeeId(UUID.randomUUID())
                .build();
        when(userRepository.findByUsernameIgnoreCase("john")).thenReturn(Optional.of(entity));

        var details = service.loadUserByUsername("  JOHN ");

        assertThat(details.getUsername()).isEqualTo("john");
        assertThat(details.getAuthorities()).extracting("authority").contains("ROLE_CTO");
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByUsernameIgnoreCase("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("nope"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage(CustomUserDetailsService.INVALID_ACCESS);
    }

    @Test
    void shouldThrowWhenUserIsInactive() {
        var entity = UserEntity.builder()
                .userId(UUID.randomUUID())
                .username("inactive")
                .password("hash")
                .role(Role.MANAGER)
                .active(false)
                .employeeId(UUID.randomUUID())
                .build();
        when(userRepository.findByUsernameIgnoreCase("inactive")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.loadUserByUsername("inactive"))
                .isInstanceOf(DisabledException.class)
                .hasMessage(CustomUserDetailsService.INACTIVED_ACCOUNT);
    }
}
