package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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

    @Column(name = "pis", length = 14, unique = true)
    private String pis;

    @Column(name = "job_position", length = 50, nullable = false)
    private String jobPosition;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "salary", nullable = false)
    private double salary;

    @Column(name = "phone", length = 15)
    private String phone;

    @Builder.Default // Adicione isso para o Lombok
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "last_seen_message_timestamp") // Novo campo
    private LocalDateTime lastSeenMessageTimestamp;

    @Builder.Default
    @Column(name = "is_home_office", nullable = false)
    private boolean homeOffice = false;

    @Column(name = "face_s3_object_key", length = 512)
    private String faceS3ObjectKey;

    @Embedded
    private AddressEmbeddable address;

    @Column(name = "company_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID companyId;

    @Column(name = "work_start_time")
    private LocalTime workStartTime;

    @Column(name = "work_end_time")
    private LocalTime workEndTime;

    @Column(name = "break_start_time")
    private LocalTime breakStartTime;

    @Column(name = "break_end_time")
    private LocalTime breakEndTime;
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type")
    private WorkScheduleType scheduleType;

    @Column(name = "scale_start_date")
    private LocalDate scaleStartDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_day_off")
    private DayOfWeek preferredDayOff;

    @Column(name = "weekend_off_index")
    private Integer weekendOffIndex;

    // Salvamos a lista de dias (ex: "MONDAY,TUESDAY") como texto no banco
    @Column(name = "fixed_work_days")
    private String fixedWorkDays;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID deletedBy;

    @Column(name = "deactivation_reason", length = 255)
    private String deactivationReason;

    // --- MÉTODOS DE CONVERSÃO ---

    public Employee toDomain() {
        return new Employee(
                employeeId, fullName, cpf, pis, jobPosition, email,
                salary, phone, active,
                address != null ? address.toDomain() : null,
                companyId, lastSeenMessageTimestamp,
                homeOffice,
                faceS3ObjectKey,
                workStartTime, workEndTime, breakStartTime, breakEndTime,
                scheduleType, scaleStartDate, preferredDayOff, weekendOffIndex,
                convertStringToSet(this.fixedWorkDays), // Converte String -> Set<DayOfWeek>
                deletedAt, deletedBy, deactivationReason
        );
    }

    public static EmployeeEntity fromDomain(Employee domain) {
        return EmployeeEntity.builder()
                .employeeId(domain.employeeId())
                .fullName(domain.fullName())
                .cpf(domain.cpf())
                .pis(domain.pis())
                .jobPosition(domain.jobPosition())
                .email(domain.email())
                .salary(domain.salary())
                .phone(domain.phone())
                .active(domain.active())
                .address(domain.address() != null ? AddressEmbeddable.fromDomain(domain.address()) : null)
                .companyId(domain.companyId())
                .lastSeenMessageTimestamp(domain.lastSeenMessageTimestamp())
                .homeOffice(domain.homeOffice())
                .faceS3ObjectKey(domain.faceS3ObjectKey())
                .workStartTime(domain.workStartTime())
                .workEndTime(domain.workEndTime())
                .breakStartTime(domain.breakStartTime())
                .breakEndTime(domain.breakEndTime())
                // Novos campos
                .scheduleType(domain.scheduleType())
                .scaleStartDate(domain.scaleStartDate())
                .preferredDayOff(domain.preferredDayOff())
                .weekendOffIndex(domain.weekendOffIndex())
                .fixedWorkDays(convertSetToString(domain.fixedWorkDays())) // Converte Set<DayOfWeek> -> String
                .deletedAt(domain.deletedAt())
                .deletedBy(domain.deletedBy())
                .deactivationReason(domain.deactivationReason())
                .build();
    }

    // Auxiliares de Conversão
    private static Set<DayOfWeek> convertStringToSet(String data) {
        if (data == null || data.isBlank()) return Collections.emptySet();
        return Arrays.stream(data.split(","))
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet());
    }

    private static String convertSetToString(Set<DayOfWeek> days) {
        if (days == null || days.isEmpty()) return null;
        return days.stream()
                .map(DayOfWeek::name)
                .collect(Collectors.joining(","));
    }
}
