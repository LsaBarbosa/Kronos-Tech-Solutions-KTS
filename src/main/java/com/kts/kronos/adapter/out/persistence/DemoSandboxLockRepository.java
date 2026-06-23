package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.DemoSandboxLockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

public interface DemoSandboxLockRepository extends JpaRepository<DemoSandboxLockEntity, String> {

    @Modifying
    @Transactional
    @Query("DELETE FROM DemoSandboxLockEntity l WHERE l.lockKey = :lockKey AND l.expiresAt < :now")
    int deleteExpired(@Param("lockKey") String lockKey, @Param("now") LocalDateTime now);
}
