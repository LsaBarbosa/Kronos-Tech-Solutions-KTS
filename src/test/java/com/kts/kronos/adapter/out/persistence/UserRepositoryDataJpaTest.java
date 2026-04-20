package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.UserEntity;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.support.jpa.AbstractPostgresDataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryDataJpaTest extends AbstractPostgresDataJpaTest {

    @Autowired
    private UserRepository repository;

    @Test
    @DisplayName("findByUsernameIgnoreCase: deve buscar ignorando case")
    void shouldFindByUsernameIgnoreCase() {
        UUID employeeId = UUID.randomUUID();

        repository.save(user("John", true, employeeId));

        assertTrue(repository.findByUsernameIgnoreCase("john").isPresent());
        assertTrue(repository.findByUsernameIgnoreCase("JOHN").isPresent());
        assertTrue(repository.existsByUsernameIgnoreCase("john"));
    }

    @Test
    @DisplayName("findByEmployeeIdInAndActive: deve filtrar por employeeIds e ativo")
    void shouldFindByEmployeeIdsAndActive() {
        UUID employeeA = UUID.randomUUID();
        UUID employeeB = UUID.randomUUID();

        repository.save(user("john", true, employeeA));
        repository.save(user("mary", false, employeeB));

        List<UserEntity> result = repository.findByEmployeeIdInAndActive(List.of(employeeA, employeeB), true);

        assertEquals(1, result.size());
        assertEquals(employeeA, result.getFirst().getEmployeeId());
        assertTrue(result.getFirst().isActive());
    }

    @Test
    @DisplayName("findByEmployeeId/existsByEmployeeId: deve localizar por colaborador")
    void shouldFindAndCheckExistenceByEmployeeId() {
        UUID employeeId = UUID.randomUUID();
        repository.save(user("john", true, employeeId));

        assertTrue(repository.findByEmployeeId(employeeId).isPresent());
        assertTrue(repository.existsByEmployeeId(employeeId));
        assertTrue(repository.findByEmployeeId(UUID.randomUUID()).isEmpty());
    }

    @Test
    @DisplayName("findByActiveTrue/findByActiveFalse: deve separar ativos e inativos")
    void shouldFindByActiveFlags() {
        UUID employeeA = UUID.randomUUID();
        UUID employeeB = UUID.randomUUID();
        repository.save(user("john", true, employeeA));
        repository.save(user("mary", false, employeeB));

        assertEquals(1, repository.findByActiveTrue().size());
        assertEquals(1, repository.findByActiveFalse().size());
    }

    @Test
    @DisplayName("findByEmployeeIdIn: deve filtrar coleção de colaboradores")
    void shouldFindByEmployeeIds() {
        UUID employeeA = UUID.randomUUID();
        UUID employeeB = UUID.randomUUID();
        UUID employeeC = UUID.randomUUID();
        repository.save(user("john", true, employeeA));
        repository.save(user("mary", false, employeeB));
        repository.save(user("carl", true, employeeC));

        List<UserEntity> result = repository.findByEmployeeIdIn(List.of(employeeA, employeeC));

        assertEquals(2, result.size());
    }

    private UserEntity user(String username, boolean active, UUID employeeId) {
        return UserEntity.builder()
                .userId(UUID.randomUUID())
                .username(username)
                .password("encoded-password")
                .role(Role.MANAGER)
                .active(active)
                .employeeId(employeeId)
                .build();
    }
}
