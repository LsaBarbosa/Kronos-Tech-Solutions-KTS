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
    @DisplayName("existsByUsername: deve delegar para repository ignorando case")
    void shouldCheckUsernameExistence() {
        when(repository.existsByUsernameIgnoreCase("john")).thenReturn(true);

        assertTrue(provider.existsByUsername("john"));

        verify(repository).existsByUsernameIgnoreCase("john");
    }

    @Test
    @DisplayName("findById: deve mapear usuário encontrado")
    void shouldFindById() {
        User user = buildUser(true);
        when(repository.findById(user.userId())).thenReturn(Optional.of(UserEntity.fromDomain(user)));

        Optional<User> result = provider.findById(user.userId());

        assertTrue(result.isPresent());
        assertEquals(user.userId(), result.get().userId());
    }

    @Test
    @DisplayName("findAll: deve mapear todos os usuários")
    void shouldFindAllUsers() {
        User active = buildUser(true);
        User inactive = buildUser(false);
        when(repository.findAll()).thenReturn(List.of(UserEntity.fromDomain(active), UserEntity.fromDomain(inactive)));

        List<User> result = provider.findAll();

        assertEquals(2, result.size());
        assertEquals(List.of(active.userId(), inactive.userId()), result.stream().map(User::userId).toList());
    }

    @Test
    @DisplayName("findByActive: deve escolher repository conforme status")
    void shouldFindByActiveStatus() {
        User active = buildUser(true);
        User inactive = buildUser(false);
        when(repository.findByActiveTrue()).thenReturn(List.of(UserEntity.fromDomain(active)));
        when(repository.findByActiveFalse()).thenReturn(List.of(UserEntity.fromDomain(inactive)));

        List<User> activeUsers = provider.findByActive(true);
        List<User> inactiveUsers = provider.findByActive(false);

        assertTrue(activeUsers.getFirst().active());
        assertFalse(inactiveUsers.getFirst().active());
        verify(repository).findByActiveTrue();
        verify(repository).findByActiveFalse();
    }

    @Test
    @DisplayName("findByEmployeeId: deve mapear usuário encontrado")
    void shouldFindByEmployeeId() {
        User user = buildUser(true);
        when(repository.findByEmployeeId(user.employeeId())).thenReturn(Optional.of(UserEntity.fromDomain(user)));

        Optional<User> result = provider.findByEmployeeId(user.employeeId());

        assertTrue(result.isPresent());
        assertEquals(user.employeeId(), result.get().employeeId());
    }

    @Test
    @DisplayName("existsByEmployeeId: deve delegar para repository")
    void shouldCheckEmployeeIdExistence() {
        UUID employeeId = UUID.randomUUID();
        when(repository.existsByEmployeeId(employeeId)).thenReturn(true);

        assertTrue(provider.existsByEmployeeId(employeeId));

        verify(repository).existsByEmployeeId(employeeId);
    }

    @Test
    @DisplayName("findByEmployeeIds: deve retornar vazio sem acessar repository quando coleção estiver vazia")
    void shouldReturnEmptyWhenEmployeeIdsAreEmpty() {
        List<User> result = provider.findByEmployeeIds(Set.of());

        assertTrue(result.isEmpty());
        verify(repository, never()).findByEmployeeIdIn(any());
    }

    @Test
    @DisplayName("findByEmployeeIds: deve retornar vazio sem acessar repository quando coleção for nula")
    void shouldReturnEmptyWhenEmployeeIdsAreNull() {
        List<User> result = provider.findByEmployeeIds(null);

        assertTrue(result.isEmpty());
        verify(repository, never()).findByEmployeeIdIn(any());
    }

    @Test
    @DisplayName("findByEmployeeIds: deve mapear usuários encontrados")
    void shouldFindByEmployeeIds() {
        User user = buildUser(true);
        when(repository.findByEmployeeIdIn(List.of(user.employeeId())))
                .thenReturn(List.of(UserEntity.fromDomain(user)));

        List<User> result = provider.findByEmployeeIds(List.of(user.employeeId()));

        assertEquals(1, result.size());
        assertEquals(user.employeeId(), result.getFirst().employeeId());
    }

    @Test
    @DisplayName("findByEmployeeIdsAndActive: deve retornar vazio sem acessar repository quando coleção for nula")
    void shouldReturnEmptyWhenEmployeeIdsAndActiveAreNull() {
        List<User> result = provider.findByEmployeeIdsAndActive(null, true);

        assertTrue(result.isEmpty());
        verify(repository, never()).findByEmployeeIdInAndActive(any(), anyBoolean());
    }

    @Test
    @DisplayName("findByEmployeeIdsAndActive: deve retornar vazio sem acessar repository quando coleção estiver vazia")
    void shouldReturnEmptyWhenEmployeeIdsAndActiveAreEmpty() {
        List<User> result = provider.findByEmployeeIdsAndActive(List.of(), true);

        assertTrue(result.isEmpty());
        verify(repository, never()).findByEmployeeIdInAndActive(any(), anyBoolean());
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

    @Test
    @DisplayName("findAllByIds: deve mapear retorno iterável do repository")
    void shouldFindAllByIds() {
        User user = buildUser(true);
        when(repository.findAllById(List.of(user.userId()))).thenReturn(List.of(UserEntity.fromDomain(user)));

        List<User> result = provider.findAllByIds(List.of(user.userId()));

        assertEquals(1, result.size());
        assertEquals(user.userId(), result.getFirst().userId());
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
