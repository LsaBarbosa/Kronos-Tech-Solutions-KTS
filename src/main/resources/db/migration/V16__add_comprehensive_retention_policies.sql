-- Insert additional retention policies for Sprint 5
-- These policies cover all retention resource types for comprehensive data lifecycle management

INSERT INTO tb_retention_policy (
    policy_id,
    policy_code,
    description,
    resource_type,
    retention_days,
    execution_mode,
    enabled,
    preserve_labor_data,
    preserve_fiscal_data,
    created_at
) VALUES
(
    gen_random_uuid(),
    'TOKEN_RETENTION_90D',
    'Delete expired authentication tokens after 90 days.',
    'BLACKLISTED_TOKEN',
    90,
    'APPLY',
    TRUE,
    FALSE,
    FALSE,
    now()
),
(
    gen_random_uuid(),
    'MESSAGE_RETENTION_180D',
    'Archive or delete messages older than 180 days, preserving labor and fiscal records.',
    'MESSAGE',
    180,
    'DRY_RUN',
    TRUE,
    TRUE,
    TRUE,
    now()
),
(
    gen_random_uuid(),
    'DOCUMENT_RETENTION_365D',
    'Delete documents older than 365 days, preserving legal and fiscal documents.',
    'DOCUMENT',
    365,
    'DRY_RUN',
    TRUE,
    TRUE,
    TRUE,
    now()
),
(
    gen_random_uuid(),
    'AUDIT_LOG_RETENTION_730D',
    'Anonymize audit logs older than 2 years (730 days) for compliance.',
    'AUDIT_LOG',
    730,
    'APPLY',
    TRUE,
    TRUE,
    TRUE,
    now()
),
(
    gen_random_uuid(),
    'LEGAL_CONSENT_RETENTION_1825D',
    'Delete legal consent records after 5 years (1825 days) following LGPD guidelines.',
    'LEGAL_CONSENT',
    1825,
    'APPLY',
    TRUE,
    FALSE,
    FALSE,
    now()
),
(
    gen_random_uuid(),
    'BIOMETRIC_RETENTION_365D',
    'Delete biometric artifacts after 1 year (365 days) of employee departure.',
    'BIOMETRIC_ARTIFACT',
    365,
    'DRY_RUN',
    TRUE,
    FALSE,
    FALSE,
    now()
),
(
    gen_random_uuid(),
    'LGPD_REQUEST_RETENTION_1825D',
    'Delete LGPD requests after 5 years (1825 days) of closure.',
    'LGPD_REQUEST',
    1825,
    'APPLY',
    TRUE,
    FALSE,
    FALSE,
    now()
);

-- Create index for faster lookups by policy code
CREATE INDEX IF NOT EXISTS idx_retention_policy_code_lookup
    ON tb_retention_policy(policy_code);

-- Create index for resource type to speed up processor discovery
CREATE INDEX IF NOT EXISTS idx_retention_policy_resource_type
    ON tb_retention_policy(resource_type);
