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