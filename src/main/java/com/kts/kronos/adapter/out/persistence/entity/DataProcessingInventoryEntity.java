package com.kts.kronos.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_data_processing_inventory")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DataProcessingInventoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "inventory_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID inventoryId;

    @Column(name = "process_code", nullable = false, unique = true, length = 100)
    private String processCode;

    @Column(name = "process_name", nullable = false, length = 200)
    private String processName;

    @Column(name = "data_category", nullable = false, length = 100)
    private String dataCategory;

    @Column(name = "data_fields", nullable = false, columnDefinition = "TEXT")
    private String dataFields;

    @Column(name = "data_subject_category", nullable = false, length = 100)
    private String dataSubjectCategory;

    @Column(name = "purpose", nullable = false, columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "legal_basis", nullable = false, length = 100)
    private String legalBasis;

    @Column(name = "sensitive_data", nullable = false)
    private Boolean sensitiveData = false;

    @Column(name = "source_system", nullable = false, length = 100)
    private String sourceSystem;

    @Column(name = "storage_location", length = 200)
    private String storageLocation;

    @Column(name = "retention_policy_code", length = 100)
    private String retentionPolicyCode;

    @Column(name = "external_sharing", columnDefinition = "TEXT")
    private String externalSharing;

    @Column(name = "international_transfer", nullable = false)
    private Boolean internationalTransfer = false;

    @Column(name = "security_measures", columnDefinition = "TEXT")
    private String securityMeasures;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
