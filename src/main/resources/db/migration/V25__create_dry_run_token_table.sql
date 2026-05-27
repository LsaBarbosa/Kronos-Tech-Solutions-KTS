CREATE TABLE tb_dry_run_token (
    token_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL,
    token_value UUID NOT NULL UNIQUE,
    employee_id UUID NOT NULL,
    company_id UUID NOT NULL,
    generated_by_user_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'CONSUMED', 'EXPIRED')),
    CONSTRAINT fk_request_id FOREIGN KEY (request_id) REFERENCES tb_lgpd_request(request_id) ON DELETE CASCADE
);

CREATE INDEX idx_dry_run_token_request_id ON tb_dry_run_token(request_id);
CREATE INDEX idx_dry_run_token_value ON tb_dry_run_token(token_value);
CREATE INDEX idx_dry_run_token_expires_at ON tb_dry_run_token(expires_at);
