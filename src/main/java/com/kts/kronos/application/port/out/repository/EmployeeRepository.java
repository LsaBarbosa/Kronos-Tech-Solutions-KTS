package com.kts.kronos.application.port.out.repository;

import com.kts.kronos.domain.model.Employee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository {
    Employee save(Employee employee);
    Optional<Employee> findById(UUID id);
    Optional<Employee> findByCpf(String cpf);
    List<Employee> findAll();
    List<Employee> findByActive(boolean active);
    void deleteById(UUID id);

}
