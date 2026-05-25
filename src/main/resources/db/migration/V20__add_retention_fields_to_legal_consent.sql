-- Add retention fields to legal_consent table
-- Task: LGPD-FINAL-01-01 - Correct LEGAL_CONSENT retention to preserve evidence

ALTER TABLE tb_legal_consent
ADD COLUMN IF NOT EXISTS retention_applied_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS retention_policy_code VARCHAR(100);

-- Create index for retention tracking
CREATE INDEX IF NOT EXISTS idx_legal_consent_retention_applied
    ON tb_legal_consent (retention_applied_at);

-- Note: Audit logging is optional for migrations
-- The tb_audit_logs table may not have all expected columns at this point
