package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.FaqArticle;
import com.kts.kronos.domain.model.enuns.FaqStatus;
import com.kts.kronos.domain.model.enuns.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_faq_article")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqArticleEntity {

    @Id
    @Column(name = "id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID id;

    @Column(name = "title", length = 300, nullable = false)
    private String title;

    @Column(name = "short_answer", columnDefinition = "TEXT", nullable = false)
    private String shortAnswer;

    @Column(name = "full_answer", columnDefinition = "TEXT", nullable = false)
    private String fullAnswer;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private FaqStatus status;

    @Column(name = "priority", nullable = false)
    private int priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private FaqCategoryEntity category;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_faq_article_role", joinColumns = @JoinColumn(name = "faq_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    @Builder.Default
    private List<Role> allowedRoles = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_faq_article_screen", joinColumns = @JoinColumn(name = "faq_id"))
    @Column(name = "screen_key", length = 100, nullable = false)
    @Builder.Default
    private List<String> screenKeys = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_faq_article_tag", joinColumns = @JoinColumn(name = "faq_id"))
    @Column(name = "tag", length = 100, nullable = false)
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public FaqArticle toDomain() {
        return new FaqArticle(
                id,
                title,
                shortAnswer,
                fullAnswer,
                status,
                priority,
                category != null ? category.toDomain() : null,
                allowedRoles != null ? List.copyOf(allowedRoles) : List.of(),
                screenKeys != null ? List.copyOf(screenKeys) : List.of(),
                tags != null ? List.copyOf(tags) : List.of(),
                createdAt,
                updatedAt
        );
    }
}
