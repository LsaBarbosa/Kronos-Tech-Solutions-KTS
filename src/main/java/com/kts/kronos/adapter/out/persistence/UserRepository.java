package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmployeeId(UUID employeeId);
    List<UserEntity> findByActiveTrue();
    List<UserEntity> findByActiveFalse();
}
