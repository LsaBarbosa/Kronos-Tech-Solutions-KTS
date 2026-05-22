CREATE TABLE tb_legal_consent (
    consent_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL,
    user_id UUID,
    consent_type VARCHAR(80) NOT NULL,
    legal_basis VARCHAR(80) NOT NULL,
    purpose VARCHAR(255) NOT NULL,
    version VARCHAR(30) NOT NULL,
    granted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE NULL,
    ip_address VARCHAR(80),
    user_agent TEXT,
    evidence_document_id UUID,
    evidence_hash_sha256 VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NULL,

    CONSTRAINT fk_legal_consent_employee
        FOREIGN KEY (employee_id) REFERENCES tb_employee (employee_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_legal_consent_user
        FOREIGN KEY (user_id) REFERENCES tb_user (user_id)
        ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_legal_consent_document
        FOREIGN KEY (evidence_document_id) REFERENCES tb_document (document_id)
        ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE INDEX idx_legal_consent_employee_type
    ON tb_legal_consent (employee_id, consent_type);

CREATE INDEX idx_legal_consent_active
    ON tb_legal_consent (employee_id, consent_type)
    WHERE revoked_at IS NULL;

CREATE UNIQUE INDEX uix_legal_consent_active_by_employee_type
    ON tb_legal_consent (employee_id, consent_type)
    WHERE revoked_at IS NULL;
