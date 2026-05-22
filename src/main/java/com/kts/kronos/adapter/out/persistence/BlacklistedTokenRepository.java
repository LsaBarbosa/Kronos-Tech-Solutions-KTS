package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.BlacklistedTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedTokenEntity, String> {
    boolean existsByTokenHash(String tokenHash);

    @Query("SELECT COUNT(b) FROM BlacklistedTokenEntity b WHERE b.expiresAt <= :cutoff")
    long countExpiredBefore(LocalDateTime cutoff);

    @Modifying
    @Transactional
    @Query("DELETE FROM BlacklistedTokenEntity b WHERE b.expiresAt <= :cutoff")
    int deleteExpiredBefore(LocalDateTime cutoff);

    @Modifying
    @Transactional
    @Query("DELETE FROM BlacklistedTokenEntity b WHERE b.expiresAt <= :now")
    void deleteExpiredTokens(LocalDateTime now);
}
