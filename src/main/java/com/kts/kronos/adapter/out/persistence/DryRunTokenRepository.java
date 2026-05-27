package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.DryRunTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DryRunTokenRepository extends JpaRepository<DryRunTokenEntity, UUID> {

    Optional<DryRunTokenEntity> findByTokenValue(UUID tokenValue);

    List<DryRunTokenEntity> findByRequestId(UUID requestId);

    List<DryRunTokenEntity> findByRequestIdAndStatus(UUID requestId, DryRunTokenEntity.DryRunTokenStatus status);

    @Modifying
    @Query("UPDATE DryRunTokenEntity t SET t.status = 'EXPIRED' WHERE t.expiresAt <= :now AND t.status = 'PENDING'")
    int expireOldTokens(@Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM DryRunTokenEntity t WHERE t.expiresAt <= :cutoffDate")
    int deleteExpiredTokens(@Param("cutoffDate") Instant cutoffDate);

    @Query("SELECT COUNT(t) > 0 FROM DryRunTokenEntity t WHERE t.requestId = :requestId AND t.status = 'PENDING' AND t.expiresAt > :now")
    boolean existsValidTokenForRequest(@Param("requestId") UUID requestId, @Param("now") Instant now);
}
