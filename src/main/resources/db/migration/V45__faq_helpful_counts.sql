-- V45 — FAQ helpful / not-helpful counters
-- Adds counters to tb_faq_article to support the POST /faqs/{faqId}/helpful endpoint.

ALTER TABLE tb_faq_article
    ADD COLUMN IF NOT EXISTS helpful_count     INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS not_helpful_count INT NOT NULL DEFAULT 0;

COMMENT ON COLUMN tb_faq_article.helpful_count     IS 'Number of users who marked this article as helpful.';
COMMENT ON COLUMN tb_faq_article.not_helpful_count IS 'Number of users who marked this article as not helpful.';
