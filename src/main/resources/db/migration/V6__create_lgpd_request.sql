CREATE TABLE tb_lgpd_request (
    request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES tb_employee(employee_id),
    requested_by_user_id UUID REFERENCES tb_user(user_id),
    company_id UUID NOT NULL REFERENCES tb_company(company_id),
    request_type VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    description TEXT NOT NULL,
    resolution_notes TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NULL,
    resolved_at TIMESTAMPTZ NULL,
    resolved_by_user_id UUID REFERENCES tb_user(user_id)
);

CREATE INDEX idx_lgpd_request_employee_created
    ON tb_lgpd_request(employee_id, created_at DESC);

CREATE INDEX idx_lgpd_request_company_status
    ON tb_lgpd_request(company_id, status);

CREATE TABLE tb_lgpd_request_history (
    history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES tb_lgpd_request(request_id),
    status VARCHAR(40) NOT NULL,
    notes TEXT NULL,
    changed_by_user_id UUID REFERENCES tb_user(user_id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_lgpd_request_history_request_created
    ON tb_lgpd_request_history(request_id, created_at ASC);
