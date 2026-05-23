-- Add retention fields to legal_consent table
-- Task: LGPD-FINAL-01-01 - Correct LEGAL_CONSENT retention to preserve evidence

ALTER TABLE tb_legal_consent
ADD COLUMN IF NOT EXISTS retention_applied_at TIMESTAMP WITH TIME ZONE,
ADD COLUMN IF NOT EXISTS retention_policy_code VARCHAR(100);

-- Create index for retention tracking
CREATE INDEX IF NOT EXISTS idx_legal_consent_retention_applied
    ON tb_legal_consent (retention_applied_at);

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
    'LEGAL_CONSENT',
    NULL,
    'INFO',
    '{"migration": "V20__add_retention_fields_to_legal_consent", "description": "Added retention tracking fields for evidence preservation"}',
    '0.0.0.0',
    'Migration',
    now()
);
