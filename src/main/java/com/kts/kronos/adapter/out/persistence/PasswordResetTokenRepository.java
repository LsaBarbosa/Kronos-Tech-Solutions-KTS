package com.kts.kronos.adapter.out.persistence;
import com.kts.kronos.adapter.out.persistence.entity.PasswordResetTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, String> {

    Optional<PasswordResetTokenEntity> findByTokenAndExpiryDateAfter(String token, LocalDateTime now);

    Optional<PasswordResetTokenEntity> findByUserId(UUID userId);

    @Query("SELECT COUNT(t) FROM PasswordResetTokenEntity t WHERE t.expiryDate <= :cutoff")
    long countExpiredBefore(LocalDateTime cutoff);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.expiryDate <= :cutoff")
    int deleteExpiredBefore(LocalDateTime cutoff);

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.expiryDate <= :now")
    void deleteExpiredTokens(LocalDateTime now);
}