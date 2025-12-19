package com.kts.kronos.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tb_company_nsr")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class CompanyNsrEntity {
    @Id
    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "last_nsr")
    private Long lastNsr;

}