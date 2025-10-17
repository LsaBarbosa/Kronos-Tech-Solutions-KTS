package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.BreakRecordApprovalRequest;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_break_record_approval")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BreakRecordApprovalEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "break_approval_id")
    private Long breakApprovalId;

    @Column(name = "time_record_id", nullable = false)
    private Long timeRecordId;

    @Column(name = "break_record_id", nullable = false)
    private Long breakRecordId;

    @Column(name = "new_start_break", nullable = false)
    private LocalDateTime newStartBreak;

    @Column(name = "new_end_break", nullable = false)
    private LocalDateTime newEndBreak;

    // Relacionamento Many-to-One de volta para a aprovação principal
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_record_id", insertable = false, updatable = false)
    private TimeRecordApprovalEntity timeRecordApproval;

    public BreakRecordApprovalRequest toDomain() {
        return new BreakRecordApprovalRequest(
                breakApprovalId, timeRecordId, breakRecordId, newStartBreak, newEndBreak
        );
    }

    public static BreakRecordApprovalEntity fromDomain(BreakRecordApprovalRequest domain) {
        return BreakRecordApprovalEntity.builder()
                .breakApprovalId(domain.breakApprovalId())
                .timeRecordId(domain.timeRecordId())
                .breakRecordId(domain.breakRecordId())
                .newStartBreak(domain.newStartBreak())
                .newEndBreak(domain.newEndBreak())
                .build();
    }
}
