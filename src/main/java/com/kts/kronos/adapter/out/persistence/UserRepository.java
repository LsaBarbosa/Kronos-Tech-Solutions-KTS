package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.in.web.dto.security.RecoverPasswordProjection;
import com.kts.kronos.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByEmployeeId(UUID employeeId);

    List<UserEntity> findByActiveTrue();

    List<UserEntity> findByActiveFalse();

    List<UserEntity> findByUserIdIn(List<UUID> userIds);

    @Query("""
                SELECT u FROM UserEntity u 
                WHERE u.employeeId IN (
                    SELECT e.employeeId FROM EmployeeEntity e WHERE e.companyId = :companyId
                )
                AND (:active IS NULL OR u.active = :active)
            """)
    List<UserEntity> findByCompanyIdAndActive(@Param("companyId") UUID companyId, @Param("active") Boolean active);

    @Query("""
            SELECT new com.kts.kronos.adapter.out.persistence.projection.RecoverPasswordProjection(
            u.userId,
            u.username,
            e.email
                )
            FROM UserEntity u
            JOIN EmployeeEntity e ON e.employeeId = u.employeeId
            WHERE e.cpf = :cpf
            AND LOWER(e.email) = LOWER(:email)
            """)
    Optional<RecoverPasswordProjection> findRecoverPasswordByCpfAndEmail(@Param("cpf") String cpf, @Param("email") String email);
}
