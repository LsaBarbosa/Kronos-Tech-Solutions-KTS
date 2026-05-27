-- Add content_hash_sha256 field to track the LegalText version hash at consent time
ALTER TABLE tb_legal_consent ADD COLUMN IF NOT EXISTS content_hash_sha256 VARCHAR(128);

-- Index for efficient lookup of valid current consents by version and hash
CREATE INDEX IF NOT EXISTS idx_legal_consent_biometric_current
ON tb_legal_consent (employee_id, consent_type, version, content_hash_sha256)
WHERE revoked_at IS NULL;
