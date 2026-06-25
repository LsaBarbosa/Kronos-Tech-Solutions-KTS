package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.in.web.dto.faq.FaqCategoryWithCountResponse;
import com.kts.kronos.adapter.out.persistence.entity.FaqArticleEntity;
import com.kts.kronos.domain.model.enuns.FaqStatus;
import com.kts.kronos.domain.model.enuns.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FaqArticleRepository extends JpaRepository<FaqArticleEntity, UUID> {

    // -----------------------------------------------------------------------
    // Search strategy:
    //   Primary  — ILIKE on title, short_answer, full_answer, category name, tags
    //   Enhanced — tsvector @@ plainto_tsquery('portuguese') when search_vector is populated
    //   Ranking  — screen match bonus DESC, ts_rank DESC, priority ASC, updatedAt DESC
    //
    // Design decisions:
    //   - No SELECT DISTINCT: role is filtered in the JOIN condition (one row per article).
    //     Tags/screens are matched via EXISTS subqueries, avoiding fan-out duplicates.
    //   - PostgreSQL rule: ORDER BY expressions must appear in SELECT list when using DISTINCT.
    //     Removing DISTINCT eliminates this constraint entirely.
    //   - ILIKE is the reliable fallback; tsvector enhances ranking when available.
    // -----------------------------------------------------------------------

    /**
     * Full-text search: ILIKE + optional tsvector ranking.
     * Role is applied as a JOIN condition — one row per article, no DISTINCT needed.
     */
    @Query(
        value = """
            SELECT a.*
            FROM tb_faq_article a
            JOIN tb_faq_article_role ar ON ar.faq_id = a.id AND ar.role = :role
            LEFT JOIN tb_faq_category cat ON cat.id = a.category_id
            WHERE a.status = :status
              AND (
                    (a.search_vector IS NOT NULL
                        AND a.search_vector @@ plainto_tsquery('portuguese', :query))
                 OR a.title        ILIKE '%' || :query || '%'
                 OR a.short_answer ILIKE '%' || :query || '%'
                 OR a.full_answer  ILIKE '%' || :query || '%'
                 OR coalesce(cat.name, '') ILIKE '%' || :query || '%'
                 OR EXISTS (
                        SELECT 1 FROM tb_faq_article_tag t
                        WHERE t.faq_id = a.id
                          AND t.tag ILIKE '%' || :query || '%'
                    )
              )
            ORDER BY
              (CASE WHEN :screen IS NOT NULL AND :screen <> ''
                    AND EXISTS (SELECT 1 FROM tb_faq_article_screen sk
                                WHERE sk.faq_id = a.id AND sk.screen_key = :screen)
                    THEN 1 ELSE 0 END) DESC,
              COALESCE(ts_rank(a.search_vector, plainto_tsquery('portuguese', :query)), 0) DESC,
              a.priority ASC,
              a.updated_at DESC
            """,
        countQuery = """
            SELECT COUNT(a.id)
            FROM tb_faq_article a
            JOIN tb_faq_article_role ar ON ar.faq_id = a.id AND ar.role = :role
            LEFT JOIN tb_faq_category cat ON cat.id = a.category_id
            WHERE a.status = :status
              AND (
                    (a.search_vector IS NOT NULL
                        AND a.search_vector @@ plainto_tsquery('portuguese', :query))
                 OR a.title        ILIKE '%' || :query || '%'
                 OR a.short_answer ILIKE '%' || :query || '%'
                 OR a.full_answer  ILIKE '%' || :query || '%'
                 OR coalesce(cat.name, '') ILIKE '%' || :query || '%'
                 OR EXISTS (
                        SELECT 1 FROM tb_faq_article_tag t
                        WHERE t.faq_id = a.id
                          AND t.tag ILIKE '%' || :query || '%'
                    )
              )
            """,
        nativeQuery = true
    )
    Page<FaqArticleEntity> searchActiveFullText(
            @Param("query") String query,
            @Param("screen") String screen,
            @Param("role") String role,
            @Param("status") String status,
            Pageable pageable
    );

    /**
     * ts_rank score for a specific article. Returns 0 when search_vector is NULL.
     */
    @Query(
        value = """
            SELECT COALESCE(ts_rank(a.search_vector, plainto_tsquery('portuguese', :query)), 0)
            FROM tb_faq_article a
            WHERE a.id = :faqId
            """,
        nativeQuery = true
    )
    Double computeRelevanceScore(@Param("faqId") UUID faqId, @Param("query") String query);

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

    /**
     * Returns categories that have at least one ACTIVE FAQ for the given role.
     * Result is a list of [categoryId, categoryName, faqCount] projections.
     */
    @Query(
        value = """
            SELECT new com.kts.kronos.adapter.in.web.dto.faq.FaqCategoryWithCountResponse(
                       cat.id, cat.name, COUNT(DISTINCT a.id))
            FROM FaqArticleEntity a
            JOIN a.category cat
            JOIN a.allowedRoles ar
            WHERE a.status = :status
              AND ar = :role
            GROUP BY cat.id, cat.name
            ORDER BY cat.name ASC
            """
    )
    List<FaqCategoryWithCountResponse> findActiveCategoriesForRole(
            @Param("role") Role role,
            @Param("status") FaqStatus status
    );

    /**
     * Increments the helpful_count for the given article.
     */
    @Modifying
    @Query(
        value = "UPDATE tb_faq_article SET helpful_count = helpful_count + 1 WHERE id = :faqId",
        nativeQuery = true
    )
    void incrementHelpfulCount(@Param("faqId") UUID faqId);

    /**
     * Increments the not_helpful_count for the given article.
     */
    @Modifying
    @Query(
        value = "UPDATE tb_faq_article SET not_helpful_count = not_helpful_count + 1 WHERE id = :faqId",
        nativeQuery = true
    )
    void incrementNotHelpfulCount(@Param("faqId") UUID faqId);
}
