package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.Employee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeProvider {
    void save(Employee employee);
    Optional<Employee> findById(UUID id);
    Optional<Employee> findByCpf(String cpf);
    List<Employee> findAll();
    List<Employee> findByActive(boolean active);
    void deleteById(UUID id);

}
