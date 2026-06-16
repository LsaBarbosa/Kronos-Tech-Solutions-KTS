CREATE TABLE tb_timesheet_signature (
    signature_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL,
    company_id UUID NOT NULL,
    signer_user_id UUID NOT NULL,
    reference_year INTEGER NOT NULL,
    reference_month INTEGER NOT NULL CHECK (reference_month BETWEEN 1 AND 12),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    signed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    signed_at_zone VARCHAR(64) NOT NULL,
    signature_type VARCHAR(40) NOT NULL,
    signature_method VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('ACTIVE', 'VOIDED')),
    point_mirror_document_id UUID NULL,
    point_mirror_hash_sha256 VARCHAR(64) NOT NULL,
    records_snapshot_hash_sha256 VARCHAR(64) NOT NULL,
    declaration_version VARCHAR(40) NOT NULL,
    declaration_hash_sha256 VARCHAR(64) NOT NULL,
    declaration_text TEXT NOT NULL,
    ip_address VARCHAR(100) NULL,
    user_agent TEXT NULL,
    evidence_json TEXT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NULL,
    voided_at TIMESTAMP WITH TIME ZONE NULL,
    voided_by_user_id UUID NULL,
    void_reason TEXT NULL
);

CREATE INDEX idx_timesheet_signature_employee
    ON tb_timesheet_signature(employee_id);

CREATE INDEX idx_timesheet_signature_company_period
    ON tb_timesheet_signature(company_id, reference_year, reference_month);

CREATE INDEX idx_timesheet_signature_signed_at
    ON tb_timesheet_signature(signed_at);

-- Garante que cada colaborador só pode ter uma assinatura ACTIVE por mês/ano.
-- Assinaturas em estado VOIDED não bloqueiam novas assinaturas.
CREATE UNIQUE INDEX uk_timesheet_signature_active_period
    ON tb_timesheet_signature(employee_id, reference_year, reference_month)
    WHERE status = 'ACTIVE';
