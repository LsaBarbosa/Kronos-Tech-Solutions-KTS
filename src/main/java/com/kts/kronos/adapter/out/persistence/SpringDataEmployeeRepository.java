package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataEmployeeRepository extends JpaRepository<EmployeeEntity, UUID> {
    Optional<EmployeeEntity> findByEmployeeIdAndActiveTrue(UUID employeeId);
    Optional<EmployeeEntity> findByCpf(String cpf);
    List<EmployeeEntity> findByActiveTrue();
    List<EmployeeEntity> findByActiveFalse();
    void deleteById(UUID id);
}
