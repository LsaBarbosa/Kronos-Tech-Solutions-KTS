package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<CompanyEntity, UUID> {
    Optional<CompanyEntity> findByCnpj(String cnpj);
    List<CompanyEntity> findByActiveTrue();
    List<CompanyEntity> findByActiveFalse();
    void deleteByCnpj(String cnpj);
}
