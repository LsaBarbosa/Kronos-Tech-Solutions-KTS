package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.Employee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_employee")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeEntity {
    @Id
    @Column(name = "employee_id", length = 36, nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID employeeId;

    @Column(name = "full_name", length = 200, nullable = false)
    private String fullName;

    @Column(name = "cpf", length = 14, nullable = false, unique = true)
    private String cpf;

    @Column(name = "job_position", length = 50, nullable = false)
    private String jobPosition;

    @Column(name = "email", length = 50, nullable = false)
    private String email;

    @Column(name = "salary", nullable = false)
    private double salary;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "is_active", nullable = false)
    private final boolean active = true;

    @Column(name = "last_seen_message_timestamp") // Novo campo
    private LocalDateTime lastSeenMessageTimestamp;

    @Builder.Default
    @Column(name = "is_home_office", nullable = false)
    private boolean homeOffice = false;

    @Column(name = "face_s3_object_key", length = 512)
    private String faceS3ObjectKey;

    @Embedded
    private AddressEmbeddable address;

    @Column(name = "company_id", columnDefinition = "CHAR(36)", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID companyId;

    public Employee toDomain(){
        return new Employee(
                employeeId, fullName, cpf, jobPosition, email,
                salary, phone, active, address.toDomain(), companyId, lastSeenMessageTimestamp,homeOffice,faceS3ObjectKey
        );
    }

    public static EmployeeEntity fromDomain(Employee employee) {
        return EmployeeEntity.builder()
                .employeeId(employee.employeeId())
                .fullName(employee.fullName())
                .cpf(employee.cpf())
                .jobPosition(employee.jobPosition())
                .email(employee.email())
                .salary(employee.salary())
                .phone(employee.phone())
                .address(AddressEmbeddable.fromDomain(employee.address()))
                .companyId(employee.companyId())
                .lastSeenMessageTimestamp(employee.lastSeenMessageTimestamp())
                .homeOffice(employee.homeOffice())
                .faceS3ObjectKey(employee.faceS3ObjectKey())
                .build();
    }

}
