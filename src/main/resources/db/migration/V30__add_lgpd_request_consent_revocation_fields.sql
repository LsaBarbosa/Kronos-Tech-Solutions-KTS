ALTER TABLE tb_lgpd_request
    ADD COLUMN IF NOT EXISTS target_consent_type VARCHAR(80),
    ADD COLUMN IF NOT EXISTS consent_revocation_executed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN IF NOT EXISTS consent_revocation_no_active_consent BOOLEAN NOT NULL DEFAULT FALSE;
