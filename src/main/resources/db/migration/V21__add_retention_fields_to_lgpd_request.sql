-- Add retention fields to lgpd_request table
-- Task: LGPD-FINAL-01-02 - Correct LGPD_REQUEST retention to preserve evidence

ALTER TABLE tb_lgpd_request
ADD COLUMN IF NOT EXISTS retention_applied_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS retention_policy_code VARCHAR(100);

-- Create index for retention tracking
CREATE INDEX IF NOT EXISTS idx_lgpd_request_retention_applied
    ON tb_lgpd_request (retention_applied_at);

-- Note: Audit logging is optional for migrations
-- The tb_audit_logs table may not have all expected columns at this point
