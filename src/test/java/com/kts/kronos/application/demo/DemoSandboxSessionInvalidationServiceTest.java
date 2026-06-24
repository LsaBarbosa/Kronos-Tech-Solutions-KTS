package com.kts.kronos.application.demo;

import com.kts.kronos.adapter.out.persistence.UserRepository;
import com.kts.kronos.adapter.out.persistence.entity.UserEntity;
import com.kts.kronos.application.service.demo.DemoSandboxSessionInvalidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemoSandboxSessionInvalidationServiceTest {

    @Mock UserRepository userRepository;

    @InjectMocks DemoSandboxSessionInvalidationService service;

    @Test
    void shouldIncrementSessionVersionAndReturnOneWhenUserFound() {
        UserEntity user = UserEntity.builder()
                .userId(UUID.randomUUID())
                .username("kronos_teste")
                .sessionVersion(3L)
                .build();
        when(userRepository.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        int result = service.invalidateSandboxUserSession("kronos_teste");

        assertThat(result).isEqualTo(1);
        assertThat(user.getSessionVersion()).isEqualTo(4L);
        verify(userRepository).save(user);
    }

    @Test
    void shouldReturnZeroWhenUserNotFound() {
        when(userRepository.findByUsernameIgnoreCase("kronos_teste")).thenReturn(Optional.empty());

        int result = service.invalidateSandboxUserSession("kronos_teste");

        assertThat(result).isZero();
        verify(userRepository, never()).save(any());
    }
}
