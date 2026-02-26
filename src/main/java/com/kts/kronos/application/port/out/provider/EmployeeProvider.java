package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.Employee;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeProvider {
    Employee save(Employee employee);
    Optional<Employee> findById(UUID id);
    Optional<Employee> findByCpf(String cpf);
    List<Employee> findAll();
     void deleteById(UUID id);
    List<Employee> findByCompanyId(UUID companyId);
    List<Employee> findByCompanyIdAndActive(UUID companyId, boolean active);
    boolean cpfExists(String cpf);
    long countByCompanyIdAndActive(UUID companyId, boolean active);
    List<Employee> findByIdIn(List<UUID> ids);
    Map<UUID, Long> countByCompanyIdsAndActive(List<UUID> companyIds, boolean active);
    Optional<Employee> findByIdForUpdate(UUID id);
}

