package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, UUID> {
    boolean existsByCpf(String cpf);
    Optional<EmployeeEntity> findByCpf(String cpf);
    void deleteById(UUID id);
    List<EmployeeEntity> findByCompanyId(UUID companyId);
    List<EmployeeEntity> findByCompanyIdAndActive(UUID companyId, boolean active);
    long countByCompanyIdAndActive(UUID companyId, boolean active);
    List<EmployeeEntity> findByEmployeeIdIn(List<UUID> employeeIds);
}
