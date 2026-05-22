ALTER TABLE tb_employee
    ALTER COLUMN email TYPE VARCHAR(100);

CREATE TABLE tb_retention_policy (
    policy_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_code VARCHAR(80) NOT NULL,
    description VARCHAR(255) NOT NULL,
    resource_type VARCHAR(80) NOT NULL,
    retention_days INTEGER NOT NULL,
    execution_mode VARCHAR(20) NOT NULL DEFAULT 'DRY_RUN',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    preserve_labor_data BOOLEAN NOT NULL DEFAULT TRUE,
    preserve_fiscal_data BOOLEAN NOT NULL DEFAULT TRUE,
    last_executed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NULL
);

CREATE UNIQUE INDEX uix_retention_policy_code
    ON tb_retention_policy(policy_code);

CREATE INDEX idx_retention_policy_enabled
    ON tb_retention_policy(enabled)
    WHERE enabled = true;

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
    'BIOMETRIC_RETENTION_REVIEW',
    'Review biometric artifacts and consent evidence retention windows.',
    'BIOMETRIC_CONSENT',
    3650,
    'DRY_RUN',
    TRUE,
    TRUE,
    TRUE,
    now()
),
(
    gen_random_uuid(),
    'LGPD_REQUEST_RETENTION_REVIEW',
    'Review LGPD request and history retention windows.',
    'LGPD_REQUEST',
    1825,
    'DRY_RUN',
    TRUE,
    TRUE,
    TRUE,
    now()
);
