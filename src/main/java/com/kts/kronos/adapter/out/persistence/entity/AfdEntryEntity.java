package com.kts.kronos.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_afd_entry", indexes = {
        @Index(name = "idx_afd_nsr_company", columnList = "company_id, nsr"),
        @Index(name = "idx_afd_date", columnList = "company_id, record_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AfdEntryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "afd_id")
    private Long id;

    @Column(name = "nsr", nullable = false)
    private Long nsr;

    @Column(name = "record_type", nullable = false, length = 1)
    private String recordType; // Ex: "7" para marcação REP-P

    @Column(name = "record_date", nullable = false)
    private LocalDateTime recordDate;

    @Column(name = "employee_cpf", length = 11, nullable = false)
    private String employeeCpf;

    @Column(name = "employee_pis", length = 11)
    private String employeePis; // Opcional no layout novo, mas bom ter

    @Column(name = "company_id", columnDefinition = "CHAR(36)", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID companyId;

    @Column(name = "employee_id", columnDefinition = "CHAR(36)", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID employeeId;

    @Column(name = "previous_hash", length = 64)
    private String previousHash; // Hash do registro anterior (NSR-1)

    @Column(name = "current_hash", length = 64, nullable = false)
    private String currentHash; // Hash deste registro
}