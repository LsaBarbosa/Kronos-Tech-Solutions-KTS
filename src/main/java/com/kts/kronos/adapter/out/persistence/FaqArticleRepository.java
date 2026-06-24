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
    // Full-text search (PostgreSQL: pg_trgm + tsvector)
    //
    // Ranking strategy:
    //   1. ts_rank on search_vector (weighted A=title, B=short_answer, C=full_answer)
    //   2. + similarity(title, :query) * 0.3  — trigram bonus for title fuzzy match
    //   3. priority ascending (1 = highest)
    //
    // The native query also handles screen-aware re-ranking: when :screen is non-empty,
    // articles linked to that screen receive a +1.0 bonus so they float to the top.
    //
    // The count query is kept separate to avoid HHH-90003001 warnings.
    // -----------------------------------------------------------------------

    /**
     * Full-text search using PostgreSQL pg_trgm + tsvector.
     * Ranks by: screen match bonus DESC, ts_rank + trigram similarity DESC, priority ASC, updatedAt DESC.
     */
    @Query(
        value = """
            SELECT DISTINCT a.*
            FROM tb_faq_article a
            JOIN tb_faq_article_role ar ON ar.faq_id = a.id
            LEFT JOIN tb_faq_article_tag t ON t.faq_id = a.id
            LEFT JOIN tb_faq_article_screen sk ON sk.faq_id = a.id
            LEFT JOIN tb_faq_category cat ON cat.id = a.category_id
            WHERE a.status = :status
              AND ar.role = :role
              AND (
                    a.search_vector @@ plainto_tsquery('portuguese', :query)
                 OR similarity(a.title, :query) > 0.1
                 OR similarity(a.short_answer, :query) > 0.05
                 OR similarity(coalesce(cat.name, ''), :query) > 0.1
                 OR EXISTS (
                        SELECT 1 FROM tb_faq_article_tag t2
                        WHERE t2.faq_id = a.id
                          AND similarity(t2.tag, :query) > 0.1
                    )
              )
            ORDER BY
              (CASE WHEN :screen IS NOT NULL AND :screen <> ''
                    AND EXISTS (SELECT 1 FROM tb_faq_article_screen sk2
                                WHERE sk2.faq_id = a.id AND sk2.screen_key = :screen)
                    THEN 1.0 ELSE 0.0 END) DESC,
              (ts_rank(a.search_vector, plainto_tsquery('portuguese', :query))
                + similarity(a.title, :query) * 0.3) DESC,
              a.priority ASC,
              a.updated_at DESC
            """,
        countQuery = """
            SELECT COUNT(DISTINCT a.id)
            FROM tb_faq_article a
            JOIN tb_faq_article_role ar ON ar.faq_id = a.id
            LEFT JOIN tb_faq_category cat ON cat.id = a.category_id
            WHERE a.status = :status
              AND ar.role = :role
              AND (
                    a.search_vector @@ plainto_tsquery('portuguese', :query)
                 OR similarity(a.title, :query) > 0.1
                 OR similarity(a.short_answer, :query) > 0.05
                 OR similarity(coalesce(cat.name, ''), :query) > 0.1
                 OR EXISTS (
                        SELECT 1 FROM tb_faq_article_tag t2
                        WHERE t2.faq_id = a.id
                          AND similarity(t2.tag, :query) > 0.1
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
     * Returns the ts_rank + similarity score for a specific article and query.
     * Used by FaqProviderImpl to populate the relevanceScore field on domain objects.
     */
    @Query(
        value = """
            SELECT (ts_rank(a.search_vector, plainto_tsquery('portuguese', :query))
                   + similarity(a.title, :query) * 0.3)
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
