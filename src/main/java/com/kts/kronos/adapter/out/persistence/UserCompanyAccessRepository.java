package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.UserCompanyAccessEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserCompanyAccessRepository extends JpaRepository<UserCompanyAccessEntity, UUID> {

    List<UserCompanyAccessEntity> findByUserIdAndActiveTrue(UUID userId);

    Optional<UserCompanyAccessEntity> findByUserIdAndCompanyIdAndActiveTrue(UUID userId, UUID companyId);

    Optional<UserCompanyAccessEntity> findByUserIdAndDefaultCompanyTrueAndActiveTrue(UUID userId);

    boolean existsByUserIdAndCompanyIdAndActiveTrue(UUID userId, UUID companyId);

    List<UserCompanyAccessEntity> findByCompanyId(UUID companyId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM UserCompanyAccessEntity a WHERE a.companyId = :companyId")
    int deleteByCompanyId(@org.springframework.data.repository.query.Param("companyId") UUID companyId);
}
