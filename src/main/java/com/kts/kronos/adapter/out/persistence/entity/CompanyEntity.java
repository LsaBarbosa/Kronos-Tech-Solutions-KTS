package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.Company;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "name_company", length = 50, nullable = false)
    private String name;

    @Column(name = "company_cnpj", length = 17, nullable = false)
    private String cnpj;

    @Column(name = "company_email", length = 50, nullable = false)
    private String email;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Embedded
    private AddressEmbeddable address;

    public Company toDomain(){
        return new Company(
                id, name, cnpj, email, active,  address.toDomain()
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
                .build();
    }
}
