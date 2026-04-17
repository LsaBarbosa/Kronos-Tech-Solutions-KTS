package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.UserRepository;
import com.kts.kronos.adapter.out.persistence.entity.UserEntity;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProviderImplTest {

    @InjectMocks
    private UserProviderImpl provider;

    @Mock
    private UserRepository repository;

    @Test
    @DisplayName("save: deve converter domínio para entity")
    void shouldSaveUser() {
        User user = buildUser(true);

        when(repository.save(any(UserEntity.class))).thenReturn(UserEntity.fromDomain(user));

        provider.save(user);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(repository).save(captor.capture());

        UserEntity entity = captor.getValue();
        assertEquals(user.userId(), entity.getUserId());
        assertEquals(user.username(), entity.getUsername());
        assertEquals(user.role(), entity.getRole());
    }

    @Test
    @DisplayName("findByUsername: deve mapear retorno do repository")
    void shouldFindByUsername() {
        User user = buildUser(true);

        when(repository.findByUsernameIgnoreCase("john"))
                .thenReturn(Optional.of(UserEntity.fromDomain(user)));

        Optional<User> result = provider.findByUsername("john");

        assertTrue(result.isPresent());
        assertEquals(user.userId(), result.get().userId());
        assertEquals(user.username(), result.get().username());
    }

    @Test
    @DisplayName("findByEmployeeIds: deve retornar vazio sem acessar repository quando coleção estiver vazia")
    void shouldReturnEmptyWhenEmployeeIdsAreEmpty() {
        List<User> result = provider.findByEmployeeIds(Set.of());

        assertTrue(result.isEmpty());
        verify(repository, never()).findByEmployeeIdIn(any());
    }

    @Test
    @DisplayName("findByEmployeeIdsAndActive: deve mapear usuários filtrados")
    void shouldFindByEmployeeIdsAndActive() {
        User activeUser = buildUser(true);

        when(repository.findByEmployeeIdInAndActive(List.of(activeUser.employeeId()), true))
                .thenReturn(List.of(UserEntity.fromDomain(activeUser)));

        List<User> result = provider.findByEmployeeIdsAndActive(List.of(activeUser.employeeId()), true);

        assertEquals(1, result.size());
        assertTrue(result.getFirst().active());
        assertEquals(activeUser.employeeId(), result.getFirst().employeeId());
    }

    @Test
    @DisplayName("deleteById: deve delegar para repository")
    void shouldDeleteById() {
        UUID userId = UUID.randomUUID();

        provider.deleteById(userId);

        verify(repository).deleteById(userId);
    }

    private User buildUser(boolean active) {
        return new User(
                UUID.randomUUID(),
                "john",
                "encoded-password",
                Role.MANAGER,
                active,
                UUID.randomUUID()
        );
    }
}