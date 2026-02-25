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

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.expiryDate <= :now")
    int deleteExpiredTokens(LocalDateTime now);

    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO tb_password_reset_token (token, user_id, expiry_date, created_at)
        VALUES (:token, :userId, :expiryDate, :createdAt)
        ON CONFLICT (user_id)
        DO UPDATE SET token = EXCLUDED.token,
                      expiry_date = EXCLUDED.expiry_date,
                      created_at = EXCLUDED.created_at
    """, nativeQuery = true)
    void upsertTokenByUserId(String token, UUID userId, LocalDateTime expiryDate, LocalDateTime createdAt);

}