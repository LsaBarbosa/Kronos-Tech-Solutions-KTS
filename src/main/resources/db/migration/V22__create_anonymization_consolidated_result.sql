-- Create table for persisting anonymization consolidated results
-- Task: LGPD-FINAL-02-02 - Persist anonymization consolidated result and link to LGPD request

CREATE TABLE tb_anonymization_consolidated_result (
    consolidated_execution_id UUID PRIMARY KEY,
    request_id UUID,
    employee_id UUID NOT NULL,
    company_id UUID NOT NULL,
    requested_by_user_id UUID NOT NULL,
    consolidated_status VARCHAR(50) NOT NULL,
    execution_mode VARCHAR(20) NOT NULL,
    total_scanned BIGINT NOT NULL DEFAULT 0,
    total_affected BIGINT NOT NULL DEFAULT 0,
    total_skipped BIGINT NOT NULL DEFAULT 0,
    total_errors BIGINT NOT NULL DEFAULT 0,
    failed_domains TEXT,
    warnings TEXT,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    FOREIGN KEY (request_id) REFERENCES tb_lgpd_request(request_id),
    FOREIGN KEY (employee_id) REFERENCES tb_employee(employee_id),
    FOREIGN KEY (company_id) REFERENCES tb_company(company_id)
);

-- Create index for faster lookups by request_id
CREATE INDEX idx_anonymization_result_request_id ON tb_anonymization_consolidated_result(request_id);

-- Create index for lookups by employee_id and company_id
CREATE INDEX idx_anonymization_result_employee_company ON tb_anonymization_consolidated_result(employee_id, company_id);

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
    'ANONYMIZATION_CONSOLIDATED_RESULT',
    NULL,
    'INFO',
    '{"migration": "V22__create_anonymization_consolidated_result", "description": "Created table for persisting anonymization consolidated results"}',
    '0.0.0.0',
    'Migration',
    now()
);
