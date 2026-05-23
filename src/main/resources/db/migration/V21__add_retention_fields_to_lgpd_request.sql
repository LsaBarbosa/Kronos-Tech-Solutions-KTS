-- Add retention fields to lgpd_request table
-- Task: LGPD-FINAL-01-02 - Correct LGPD_REQUEST retention to preserve evidence

ALTER TABLE tb_lgpd_request
ADD COLUMN IF NOT EXISTS retention_applied_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS retention_policy_code VARCHAR(100);

-- Create index for retention tracking
CREATE INDEX IF NOT EXISTS idx_lgpd_request_retention_applied
    ON tb_lgpd_request (retention_applied_at);

-- Log the migration
INSERT INTO tb_audit_log (
    audit_log_id,
    action,
    resource_type,
    resource_id,
    severity,
    details,
    ip_address,
    user_agent,
    created_at
) VALUES (
    gen_random_uuid(),
    'MIGRATION_APPLIED',
    'LGPD_REQUEST',
    NULL,
    'INFO',
    '{"migration": "V21__add_retention_fields_to_lgpd_request", "description": "Added retention tracking fields for evidence preservation"}',
    '0.0.0.0',
    'Migration',
    now()
);
