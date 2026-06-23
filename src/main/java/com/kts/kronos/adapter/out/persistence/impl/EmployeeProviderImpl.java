package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.application.port.out.projection.CompanyEmployeeCountsProjection;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.domain.model.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@RequiredArgsConstructor
@Component
public class EmployeeProviderImpl implements EmployeeProvider {
    private static final Pattern NON_DIGIT_PATTERN = Pattern.compile("\\D");
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
        for (String candidate : buildCpfCandidates(cpf)) {
            Optional<EmployeeEntity> opt = repository.findByCpf(candidate);
            if (opt.isPresent()) {
                return opt.map(EmployeeEntity::toDomain);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean cpfExists(String cpf) {
        for (String candidate : buildCpfCandidates(cpf)) {
            if (repository.existsByCpf(candidate)) {
                return true;
            }
        }
        return false;
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
    public List<Employee> findAllByIds(Collection<UUID> ids) {
        List<Employee> result = new ArrayList<>();
        repository.findAllById(ids).forEach(entity -> result.add(entity.toDomain()));
        return result;
    }

    @Override
    public List<CompanyEmployeeCountsProjection> countByCompanyIds(Collection<UUID> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return List.of();
        }
        return repository.countByCompanyIds(companyIds);
    }

    @Override
    public boolean cpfExistsInCompany(UUID companyId, String cpf) {
        for (String candidate : buildCpfCandidates(cpf)) {
            if (repository.existsByCompanyIdAndCpfAndDeletedAtIsNull(companyId, candidate)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Optional<Employee> findByCompanyIdAndCpf(UUID companyId, String cpf) {
        for (String candidate : buildCpfCandidates(cpf)) {
            Optional<EmployeeEntity> opt = repository.findByCompanyIdAndCpfAndDeletedAtIsNull(companyId, candidate);
            if (opt.isPresent()) {
                return opt.map(EmployeeEntity::toDomain);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Employee> findAllByCpf(String cpf) {
        for (String candidate : buildCpfCandidates(cpf)) {
            List<EmployeeEntity> results = repository.findAllByCpf(candidate);
            if (!results.isEmpty()) {
                return results.stream().map(EmployeeEntity::toDomain).toList();
            }
        }
        return List.of();
    }

    private List<String> buildCpfCandidates(String cpf) {
        if (cpf == null) {
            return List.of();
        }

        String raw = cpf.trim();
        if (raw.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(raw);

        String digits = NON_DIGIT_PATTERN.matcher(raw).replaceAll("");
        if (!digits.isEmpty()) {
            candidates.add(digits);
            if (digits.length() == 11) {
                candidates.add(formatCpf(digits));
            }
        }

        return List.copyOf(candidates);
    }

    private String formatCpf(String digits) {
        return "%s.%s.%s-%s".formatted(
                digits.substring(0, 3),
                digits.substring(3, 6),
                digits.substring(6, 9),
                digits.substring(9, 11)
        );
    }
}
