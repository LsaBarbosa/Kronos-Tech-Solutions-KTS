package com.kts.kronos.adapter.out.persistence.entity;
import com.kts.kronos.domain.model.BreakRecord;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_break_records")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BreakRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "break_record_id")
    private Long breakRecordId;

    @Column(name = "time_record_id", nullable = false)
    private Long timeRecordId; // Chave estrangeira explícita

    @Column(name = "start_break", nullable = false)
    private LocalDateTime startBreak;

    @Column(name = "end_break")
    private LocalDateTime endBreak;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // Relacionamento com TimeRecordEntity (Muitos para Um)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_record_id", insertable = false, updatable = false)
    private TimeRecordEntity timeRecord;


    public BreakRecord toDomain() {
        return new BreakRecord(
                breakRecordId, timeRecordId, startBreak, endBreak, active
        );
    }

    public static BreakRecordEntity fromDomain(BreakRecord domain) {
        return BreakRecordEntity.builder()
                .breakRecordId(domain.breakRecordId())
                .timeRecordId(domain.timeRecordId())
                .startBreak(domain.startBreak())
                .endBreak(domain.endBreak())
                .active(domain.active())
                .build();
    }
}