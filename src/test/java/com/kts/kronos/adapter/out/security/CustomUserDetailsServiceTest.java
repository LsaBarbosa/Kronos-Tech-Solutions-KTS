package com.kts.kronos.adapter.out.security;

import com.kts.kronos.adapter.out.persistence.UserRepository;
import com.kts.kronos.adapter.out.persistence.entity.UserEntity;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private CustomUserDetailsService service;

    @Test
    @DisplayName("loadUserByUsername: deve retornar UserDetails para usuario ativo")
    void shouldLoadActiveUserByUsernameIgnoringCase() {
        UserEntity entity = userEntity("manager", true, Role.MANAGER);
        when(repository.findByUsernameIgnoreCase("MANAGER")).thenReturn(Optional.of(entity));

        var userDetails = service.loadUserByUsername("MANAGER");

        assertEquals("manager", userDetails.getUsername());
        assertEquals("encoded-password", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_MANAGER")));
        verify(repository).findByUsernameIgnoreCase("MANAGER");
    }

    @Test
    @DisplayName("loadUserByUsername: deve falhar quando usuario nao existir")
    void shouldThrowWhenUserDoesNotExist() {
        when(repository.findByUsernameIgnoreCase("missing")).thenReturn(Optional.empty());

        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing")
        );

        assertEquals("Usuário ou senha inválidos", exception.getMessage());
    }

    @Test
    @DisplayName("loadUserByUsername: deve falhar quando usuario estiver inativo")
    void shouldThrowWhenUserIsInactive() {
        UserEntity entity = userEntity("partner", false, Role.PARTNER);
        when(repository.findByUsernameIgnoreCase("partner")).thenReturn(Optional.of(entity));

        DisabledException exception = assertThrows(
                DisabledException.class,
                () -> service.loadUserByUsername("partner")
        );

        assertTrue(exception.getMessage().contains("conta foi desativada"));
    }

    private static UserEntity userEntity(String username, boolean active, Role role) {
        return UserEntity.builder()
                .userId(UUID.randomUUID())
                .username(username)
                .password("encoded-password")
                .role(role)
                .active(active)
                .employeeId(UUID.randomUUID())
                .build();
    }
}
