package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.ScheduleException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "tb_employee_schedule_exception")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleExceptionEntity {

    @Id
    @Column(name = "id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID employeeId;

    @Column(name = "exception_date", nullable = false)
    private LocalDate exceptionDate;

    @Column(name = "work_start_time")
    private LocalTime workStartTime;

    @Column(name = "work_end_time")
    private LocalTime workEndTime;

    @Column(name = "break_start_time")
    private LocalTime breakStartTime;

    @Column(name = "break_end_time")
    private LocalTime breakEndTime;

    @Column(name = "is_day_off", nullable = false)
    private boolean isDayOff;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public ScheduleException toDomain() {
        return new ScheduleException(
                id,
                employeeId,
                exceptionDate,
                workStartTime,
                workEndTime,
                breakStartTime,
                breakEndTime,
                isDayOff,
                description
        );
    }

    public static ScheduleExceptionEntity fromDomain(ScheduleException domain) {
        return ScheduleExceptionEntity.builder()
                .id(domain.id())
                .employeeId(domain.employeeId())
                .exceptionDate(domain.exceptionDate())
                .workStartTime(domain.workStartTime())
                .workEndTime(domain.workEndTime())
                .breakStartTime(domain.breakStartTime())
                .breakEndTime(domain.breakEndTime())
                .isDayOff(domain.isDayOff())
                .description(domain.description())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
