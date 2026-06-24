-- V42 — FAQ tables
-- MVP: uses ILIKE for search. Candidate for upgrade to tsvector/GIN + pg_trgm in a future migration.

-- -------------------------------------------------------------------------
-- 1. Categories
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tb_faq_category (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP   NOT NULL DEFAULT now(),
    CONSTRAINT pk_faq_category PRIMARY KEY (id),
    CONSTRAINT uq_faq_category_name UNIQUE (name)
);

-- -------------------------------------------------------------------------
-- 2. Articles
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tb_faq_article (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    title        VARCHAR(300) NOT NULL,
    short_answer TEXT         NOT NULL,
    full_answer  TEXT         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    priority     INT          NOT NULL DEFAULT 5,
    category_id  UUID,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT pk_faq_article PRIMARY KEY (id),
    CONSTRAINT fk_faq_article_category FOREIGN KEY (category_id)
        REFERENCES tb_faq_category (id) ON DELETE SET NULL,
    CONSTRAINT chk_faq_article_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_faq_article_priority CHECK (priority >= 1 AND priority <= 10)
);

-- -------------------------------------------------------------------------
-- 3. Roles allowed per article  (many-to-many)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tb_faq_article_role (
    faq_id UUID        NOT NULL,
    role   VARCHAR(20) NOT NULL,
    CONSTRAINT pk_faq_article_role PRIMARY KEY (faq_id, role),
    CONSTRAINT fk_faq_article_role_article FOREIGN KEY (faq_id)
        REFERENCES tb_faq_article (id) ON DELETE CASCADE,
    CONSTRAINT chk_faq_role CHECK (role IN ('PARTNER', 'MANAGER', 'CTO'))
);

-- -------------------------------------------------------------------------
-- 4. Screen keys per article  (many-to-many)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tb_faq_article_screen (
    faq_id     UUID         NOT NULL,
    screen_key VARCHAR(100) NOT NULL,
    CONSTRAINT pk_faq_article_screen PRIMARY KEY (faq_id, screen_key),
    CONSTRAINT fk_faq_article_screen_article FOREIGN KEY (faq_id)
        REFERENCES tb_faq_article (id) ON DELETE CASCADE
);

-- -------------------------------------------------------------------------
-- 5. Tags per article  (many-to-many)
-- -------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tb_faq_article_tag (
    faq_id UUID         NOT NULL,
    tag    VARCHAR(100) NOT NULL,
    CONSTRAINT pk_faq_article_tag PRIMARY KEY (faq_id, tag),
    CONSTRAINT fk_faq_article_tag_article FOREIGN KEY (faq_id)
        REFERENCES tb_faq_article (id) ON DELETE CASCADE
);

-- -------------------------------------------------------------------------
-- 6. Basic indexes for query performance
-- -------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_faq_article_status   ON tb_faq_article (status);
CREATE INDEX IF NOT EXISTS idx_faq_article_priority ON tb_faq_article (priority);
CREATE INDEX IF NOT EXISTS idx_faq_article_role     ON tb_faq_article_role (role);
CREATE INDEX IF NOT EXISTS idx_faq_article_screen   ON tb_faq_article_screen (screen_key);
