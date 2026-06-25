-- V44 — FAQ full-text search upgrade
-- Adds pg_trgm extension, trigram indexes, and tsvector column for ranking.
-- The search_vector is maintained via trigger to avoid GENERATED ALWAYS AS limitations
-- with concatenated TEXT columns in older PostgreSQL versions.

-- -------------------------------------------------------------------------
-- 1. Enable pg_trgm
-- -------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- -------------------------------------------------------------------------
-- 2. Trigram indexes on searchable columns
-- -------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_faq_article_title_trgm
    ON tb_faq_article USING gin (title gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_faq_article_short_answer_trgm
    ON tb_faq_article USING gin (short_answer gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_faq_article_full_answer_trgm
    ON tb_faq_article USING gin (full_answer gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_faq_article_tag_trgm
    ON tb_faq_article_tag USING gin (tag gin_trgm_ops);

-- -------------------------------------------------------------------------
-- 3. search_vector column (tsvector) + GIN index
-- Portuguese dictionary is used; fallback to 'simple' if 'portuguese' is absent.
-- -------------------------------------------------------------------------
ALTER TABLE tb_faq_article
    ADD COLUMN IF NOT EXISTS search_vector tsvector;

-- Initial population of search_vector for existing rows
UPDATE tb_faq_article
SET search_vector = (
    setweight(to_tsvector('portuguese', coalesce(title, '')), 'A') ||
    setweight(to_tsvector('portuguese', coalesce(short_answer, '')), 'B') ||
    setweight(to_tsvector('portuguese', coalesce(full_answer, '')), 'C')
);

CREATE INDEX IF NOT EXISTS idx_faq_article_search_vector
    ON tb_faq_article USING gin (search_vector);

-- -------------------------------------------------------------------------
-- 4. Trigger to keep search_vector updated on INSERT / UPDATE
-- -------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION faq_article_search_vector_update()
RETURNS trigger AS $$
BEGIN
    NEW.search_vector :=
        setweight(to_tsvector('portuguese', coalesce(NEW.title, '')), 'A') ||
        setweight(to_tsvector('portuguese', coalesce(NEW.short_answer, '')), 'B') ||
        setweight(to_tsvector('portuguese', coalesce(NEW.full_answer, '')), 'C');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_faq_article_search_vector ON tb_faq_article;

CREATE TRIGGER trg_faq_article_search_vector
    BEFORE INSERT OR UPDATE OF title, short_answer, full_answer
    ON tb_faq_article
    FOR EACH ROW
    EXECUTE FUNCTION faq_article_search_vector_update();
