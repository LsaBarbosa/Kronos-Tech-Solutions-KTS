package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.BlacklistedToken;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_blacklisted_token")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlacklistedTokenEntity {
    @Id
    private String tokenHash;
    private LocalDateTime expiresAt;

    public BlacklistedToken toDomain() {
        return new BlacklistedToken(tokenHash, expiresAt);
    }

    public static BlacklistedTokenEntity fromDomain(BlacklistedToken domain) {
        return BlacklistedTokenEntity.builder()
                .tokenHash(domain.tokenHash())
                .expiresAt(domain.expiresAt())
                .build();
    }
}
