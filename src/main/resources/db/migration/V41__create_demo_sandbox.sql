-- V10: CTO Demo Sandbox support
-- Adds sandbox identification to companies + audit/lock infrastructure

-- Sandbox metadata on company
ALTER TABLE tb_company
    ADD COLUMN IF NOT EXISTS is_sandbox  BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS sandbox_key VARCHAR(50);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tb_company_sandbox_key
    ON tb_company(sandbox_key)
    WHERE sandbox_key IS NOT NULL;

-- Demo job audit (retained after purge; contains no personal data)
CREATE TABLE IF NOT EXISTS tb_demo_job_audit (
    audit_id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id                UUID         NOT NULL,
    operation             VARCHAR(20)  NOT NULL CHECK (operation IN ('CREATE','PURGE','VALIDATE')),
    status                VARCHAR(20)  NOT NULL CHECK (status IN ('STARTED','SUCCESS','FAILED','PARTIAL')),
    sandbox_key           VARCHAR(50)  NOT NULL,
    actor_user_id         UUID,
    actor_role            VARCHAR(50),
    started_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    finished_at           TIMESTAMP,
    duration_ms           BIGINT,
    companies_count       INTEGER      NOT NULL DEFAULT 0,
    users_count           INTEGER      NOT NULL DEFAULT 0,
    employees_count       INTEGER      NOT NULL DEFAULT 0,
    point_records_count   INTEGER      NOT NULL DEFAULT 0,
    documents_count       INTEGER      NOT NULL DEFAULT 0,
    requests_count        INTEGER      NOT NULL DEFAULT 0,
    files_count           INTEGER      NOT NULL DEFAULT 0,
    sessions_count        INTEGER      NOT NULL DEFAULT 0,
    cache_keys_count      INTEGER      NOT NULL DEFAULT 0,
    error_message         VARCHAR(500),
    app_version           VARCHAR(50),
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_demo_job_audit_job_id  ON tb_demo_job_audit(job_id);
CREATE INDEX IF NOT EXISTS idx_demo_job_audit_sandbox ON tb_demo_job_audit(sandbox_key, started_at DESC);

-- Advisory lock for demo create/purge (prevents concurrent executions)
CREATE TABLE IF NOT EXISTS tb_demo_sandbox_lock (
    lock_key    VARCHAR(100) PRIMARY KEY,
    locked_at   TIMESTAMP    NOT NULL,
    locked_by   UUID,
    job_id      UUID         NOT NULL,
    expires_at  TIMESTAMP    NOT NULL
);