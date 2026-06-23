package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.domain.model.Company;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;
@Entity
@Table(name = "tb_company")
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode
@Builder
public class CompanyEntity {
    @Id
    @Column(name = "company_id", length = 36, nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(name = "name_company", length = 50, nullable = false)
    private String name;

    @Column(name = "company_cnpj", length = 17, nullable = false)
    private String cnpj;

    @Column(name = "company_email", length = 50, nullable = false)
    private String email;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Embedded
    private AddressEmbeddable address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID deletedBy;

    @Column(name = "deactivation_reason", length = 255)
    private String deactivationReason;

    @Builder.Default
    @Column(name = "is_sandbox", nullable = false)
    private boolean sandbox = false;

    @Column(name = "sandbox_key", length = 50)
    private String sandboxKey;

    public Company toDomain(){
        return new Company(
                id,
                name,
                cnpj,
                email,
                active,
                address.toDomain(),
                new Location(latitude, longitude),
                0L,
                0L,
                deletedAt,
                deletedBy,
                deactivationReason
        );
    }
    public static CompanyEntity fromDomain(Company company) {
        return CompanyEntity.builder()
                .id(company.companyId())
                .name(company.name())
                .cnpj(company.cnpj())
                .email(company.email())
                .active(company.active())
                .address(AddressEmbeddable.fromDomain(company.address()))
                .latitude(company.location().latitude())
                .longitude(company.location().longitude())
                .deletedAt(company.deletedAt())
                .deletedBy(company.deletedBy())
                .deactivationReason(company.deactivationReason())
                .build();
    }
}
