CREATE TABLE tb_retention_execution_log (
    execution_id UUID PRIMARY KEY,
    policy_code VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    execution_mode VARCHAR(30) NOT NULL,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP NULL,
    status VARCHAR(30) NOT NULL,
    scanned_count BIGINT NOT NULL DEFAULT 0,
    affected_count BIGINT NOT NULL DEFAULT 0,
    skipped_count BIGINT NOT NULL DEFAULT 0,
    error_count BIGINT NOT NULL DEFAULT 0,
    notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_retention_execution_policy_code ON tb_retention_execution_log(policy_code);
CREATE INDEX idx_retention_execution_resource_type ON tb_retention_execution_log(resource_type);
CREATE INDEX idx_retention_execution_status ON tb_retention_execution_log(status);
CREATE INDEX idx_retention_execution_created_at ON tb_retention_execution_log(created_at);
