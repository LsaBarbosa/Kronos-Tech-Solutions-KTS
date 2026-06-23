package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.UserCompanyAccess;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_user_company_access")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCompanyAccessEntity {

    @Id
    @Column(name = "access_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID accessId;

    @Column(name = "user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID userId;

    @Column(name = "company_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID companyId;

    @Column(name = "employee_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID employeeId;

    @Column(name = "role", length = 50, nullable = false)
    private String role;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private boolean defaultCompany = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public UserCompanyAccess toDomain() {
        return new UserCompanyAccess(
                accessId, userId, companyId, employeeId, role,
                active, defaultCompany, createdAt, updatedAt
        );
    }

    public static UserCompanyAccessEntity fromDomain(UserCompanyAccess domain) {
        return UserCompanyAccessEntity.builder()
                .accessId(domain.accessId())
                .userId(domain.userId())
                .companyId(domain.companyId())
                .employeeId(domain.employeeId())
                .role(domain.role())
                .active(domain.active())
                .defaultCompany(domain.defaultCompany())
                .createdAt(domain.createdAt())
                .updatedAt(domain.updatedAt())
                .build();
    }
}
