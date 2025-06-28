package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.Company;
import jakarta.persistence.*;
import java.util.UUID;

import lombok.*;
@Entity
@Table(name = "tb_company")
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode
@Builder
public class CompanyEntity {
    @Id
    @Column(name = "company_id", columnDefinition = "BINARY(16)")
    private UUID id;

    @Column(name = "name_company", length = 50, nullable = false)
    private String name;

    @Column(name = "company_cnpj", length = 17, nullable = false)
    private String cnpj;

    @Column(name = "company_email", length = 50, nullable = false)
    private String email;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "address_id", columnDefinition = "BINARY(16)")
    private UUID addressId;

    public Company toDomain(){
        return new Company(
                id, name, cnpj, email, active, addressId
        );
    }
}
