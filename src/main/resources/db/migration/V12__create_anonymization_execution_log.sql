CREATE TABLE tb_anonymization_execution_log (
    execution_id UUID PRIMARY KEY,
    employee_id UUID NOT NULL,
    company_id UUID NOT NULL,
    requested_by_user_id UUID NOT NULL,
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

CREATE INDEX idx_anon_execution_employee_id ON tb_anonymization_execution_log(employee_id);
CREATE INDEX idx_anon_execution_company_id ON tb_anonymization_execution_log(company_id);
CREATE INDEX idx_anon_execution_resource_type ON tb_anonymization_execution_log(resource_type);
CREATE INDEX idx_anon_execution_status ON tb_anonymization_execution_log(status);
CREATE INDEX idx_anon_execution_created_at ON tb_anonymization_execution_log(created_at);
