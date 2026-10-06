package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.application.port.out.projection.CompanyEmployeeCountsProjection;
import com.kts.kronos.domain.model.Employee;

import java.util.Collection;
import java.util.List;
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
    List<Employee> findAllByIds(Collection<UUID> ids);
    List<CompanyEmployeeCountsProjection> countByCompanyIds(Collection<UUID> companyIds);

    boolean cpfExistsInCompany(UUID companyId, String cpf);
    Optional<Employee> findByCompanyIdAndCpf(UUID companyId, String cpf);
    List<Employee> findAllByCpf(String cpf);
    Optional<Employee> findByPis(String pis);
    Optional<Employee> findByCompanyIdAndPis(UUID companyId, String pis);
}
