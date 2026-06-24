package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.FaqArticleEntity;
import com.kts.kronos.domain.model.enuns.FaqStatus;
import com.kts.kronos.domain.model.enuns.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaqArticleRepository extends JpaRepository<FaqArticleEntity, UUID> {

    /**
     * Search query that:
     * - Filters by ACTIVE status and a specific role in allowedRoles
     * - Matches query string (case-insensitive) against title, shortAnswer, fullAnswer,
     *   category name, tags, and screen keys
     * - When screen is provided (non-null, non-empty), articles linked to that screen come first
     * - Secondary ordering: priority ASC (lower number = higher priority), updatedAt DESC
     *
     * MVP note: uses LOWER + LIKE. Candidate for upgrade to full-text (tsvector/GIN) in next iteration.
     * The count query is provided separately to avoid HHH-90003001 (FETCH + DISTINCT pagination warning).
     */
    @Query(
        value = """
            SELECT DISTINCT a FROM FaqArticleEntity a
            LEFT JOIN a.category cat
            LEFT JOIN a.allowedRoles ar
            LEFT JOIN a.tags t
            LEFT JOIN a.screenKeys sk
            WHERE a.status = :status
              AND ar = :role
              AND (
                   LOWER(a.title)      LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(a.shortAnswer) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(a.fullAnswer)  LIKE LOWER(CONCAT('%', :query, '%'))
                OR (cat IS NOT NULL AND LOWER(cat.name) LIKE LOWER(CONCAT('%', :query, '%')))
                OR LOWER(t)             LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(sk)            LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY a.priority ASC, a.updatedAt DESC
            """,
        countQuery = """
            SELECT COUNT(DISTINCT a.id) FROM FaqArticleEntity a
            LEFT JOIN a.allowedRoles ar
            LEFT JOIN a.tags t
            LEFT JOIN a.screenKeys sk
            LEFT JOIN a.category cat
            WHERE a.status = :status
              AND ar = :role
              AND (
                   LOWER(a.title)      LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(a.shortAnswer) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(a.fullAnswer)  LIKE LOWER(CONCAT('%', :query, '%'))
                OR (cat IS NOT NULL AND LOWER(cat.name) LIKE LOWER(CONCAT('%', :query, '%')))
                OR LOWER(t)             LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(sk)            LIKE LOWER(CONCAT('%', :query, '%'))
              )
            """
    )
    Page<FaqArticleEntity> searchActive(
            @Param("query") String query,
            @Param("role") Role role,
            @Param("status") FaqStatus status,
            Pageable pageable
    );

    /**
     * Contextual: returns ACTIVE articles for a specific screen and role, ordered by priority.
     */
    @Query("""
        SELECT DISTINCT a FROM FaqArticleEntity a
        LEFT JOIN a.category cat
        LEFT JOIN a.allowedRoles ar
        LEFT JOIN a.screenKeys sk
        WHERE a.status = :status
          AND ar = :role
          AND sk = :screen
        ORDER BY a.priority ASC, a.updatedAt DESC
        """)
    List<FaqArticleEntity> findByScreenAndRoleActive(
            @Param("screen") String screen,
            @Param("role") Role role,
            @Param("status") FaqStatus status,
            Pageable pageable
    );

    /**
     * Find single ACTIVE article by ID (no role filtering here — role validation is done in service).
     */
    Optional<FaqArticleEntity> findByIdAndStatus(UUID id, FaqStatus status);
}
