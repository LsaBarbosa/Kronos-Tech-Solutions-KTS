package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.SpringDataEmployeeRepository;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.application.port.out.repository.EmployeeRepository;
import com.kts.kronos.domain.model.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class EmployeeRepositoryAdapter implements EmployeeRepository {
    private final SpringDataEmployeeRepository repository;
    @Override
    public Employee save(Employee employee) {
        var entity = EmployeeEntity.fromDomain(employee);
         var saved =repository.save(entity);
         return saved.toDomain();
    }

    @Override
    public Optional<Employee> findById(UUID id) {
        Optional<EmployeeEntity> opt = repository.findById(id);
        return opt.map(EmployeeEntity::toDomain);
    }

    @Override
    public Optional<Employee> findByCpf(String cpf) {
        return repository.findByCpf(cpf).map(EmployeeEntity::toDomain);
    }

    @Override
    public List<Employee> findAll() {
        return repository.findAll()
                .stream()
                .map(EmployeeEntity::toDomain)
                .toList();
    }

    @Override
    public List<Employee> findByActive(boolean active) {
        List<EmployeeEntity> entities = active
                ? repository.findByActiveTrue()
                : repository.findByActiveFalse();
        return entities.stream()
                .map(EmployeeEntity::toDomain)
                .toList();
    }


    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

}
