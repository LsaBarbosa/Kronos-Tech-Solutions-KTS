package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.domain.model.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class EmployeeProviderImpl implements EmployeeProvider {
    private final EmployeeRepository repository;
    @Override
    public Employee save(Employee employee) {
        var entity = EmployeeEntity.fromDomain(employee);
         var saved =repository.save(entity);
       return   saved.toDomain();
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
    public boolean cpfExists(String cpf) {
        return repository.existsByCpf(cpf);
    }
    @Override
    public List<Employee> findAll() {
        return repository.findAll()
                .stream()
                .map(EmployeeEntity::toDomain)
                .toList();
    }


    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public List<Employee> findByCompanyId(UUID companyId) {
        return repository.findByCompanyId(companyId)
                .stream()
                .map(EmployeeEntity::toDomain)
                .toList();
    }
    @Override
    public List<Employee> findByCompanyIdAndActive(UUID companyId, boolean active) {
        return repository.findByCompanyIdAndActive(companyId, active)
                .stream()
                .map(EmployeeEntity::toDomain)
                .toList();
    }
    @Override
    public long countByCompanyIdAndActive(UUID companyId, boolean active) {
        return repository.countByCompanyIdAndActive(companyId, active);
    }

    @Override

    public List<Employee> findByIdIn(List<UUID> ids) {

        if (ids == null || ids.isEmpty()) {

            return List.of();

        }

        return repository.findByEmployeeIdIn(ids)

                .stream()

                .map(EmployeeEntity::toDomain)

                .toList();

    }
}
