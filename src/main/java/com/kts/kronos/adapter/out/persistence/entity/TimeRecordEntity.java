package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.StatusRecord;
import com.kts.kronos.domain.model.TimeRecord;
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
@Table(name = "tb_time_records")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TimeRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "time_record_id")
    private Long timeRecordId;

    @Column(name = "start_work")
    private LocalDateTime startWork;

    @Column(name = "end_work")
    private LocalDateTime  endWork;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_record", length = 10)
    private StatusRecord statusRecord;

    @Column(name = "is_edite", nullable = false)
    private boolean edited = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "employee_id", columnDefinition = "CHAR(36)", nullable = false)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID employeeId;

    public TimeRecord toDomain() {
        return new TimeRecord(
                timeRecordId,
                startWork,
                endWork,
                statusRecord,
                edited,
                active,
                employeeId
        );
    }

    public static TimeRecordEntity fromDomain(TimeRecord tr) {
        return TimeRecordEntity.builder()
                .timeRecordId(tr.timeRecordId())
                .startWork(tr.startWork())
                .endWork(tr.endWork())
                .statusRecord(tr.statusRecord())
                .edited(tr.edited())
                .active(tr.active())
                .employeeId(tr.employeeId())
                .build();
    }
}
