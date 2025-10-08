package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.PasswordResetToken;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_password_reset_token")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetTokenEntity {
    @Id
    @Column(name = "token", length = 36, nullable = false)
    private String token;

    @Column(name = "user_id", columnDefinition = "CHAR(36)", nullable = false, unique = true)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID userId;

    @Column(name = "expiry_date", nullable = false)
    private LocalDateTime expiryDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PasswordResetToken toDomain() {
        return new PasswordResetToken(token, userId, expiryDate,createdAt);
    }

    public static PasswordResetTokenEntity fromDomain(PasswordResetToken domain) {
        return PasswordResetTokenEntity.builder()
                .token(domain.token())
                .userId(domain.userId())
                .expiryDate(domain.expiryDate())
                .createdAt(domain.createdAt())
                .build();
    }
}
