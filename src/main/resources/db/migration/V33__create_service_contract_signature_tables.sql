-- Tabela principal do contrato de serviço enviado pelo MANAGER.
CREATE TABLE tb_service_contract (
    contract_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    source_document_id UUID NOT NULL,
    source_document_owner_employee_id UUID NOT NULL,
    created_by_user_id UUID NOT NULL,
    created_by_employee_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    document_hash_sha256 CHAR(64) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('ACTIVE', 'VOIDED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NULL,
    voided_at TIMESTAMP WITH TIME ZONE NULL,
    voided_by_user_id UUID NULL,
    void_reason TEXT NULL
);

CREATE INDEX idx_service_contract_company_id
    ON tb_service_contract(company_id);

-- Atribuição: contrato ↔ colaborador. Um colaborador pode ter no máximo
-- uma atribuição (PENDING, SIGNED ou CANCELLED) por contrato.
CREATE TABLE tb_service_contract_assignment (
    assignment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL,
    company_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    assigned_by_user_id UUID NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING', 'SIGNED', 'CANCELLED')),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    signed_at TIMESTAMP WITH TIME ZONE NULL,
    cancelled_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT fk_service_contract_assignment_contract
        FOREIGN KEY (contract_id) REFERENCES tb_service_contract(contract_id) ON DELETE CASCADE,
    CONSTRAINT uk_service_contract_assignment_contract_employee
        UNIQUE (contract_id, employee_id)
);

CREATE INDEX idx_service_contract_assignment_employee_status
    ON tb_service_contract_assignment(employee_id, status);

CREATE INDEX idx_service_contract_assignment_contract_employee
    ON tb_service_contract_assignment(contract_id, employee_id);

-- Assinatura: 1 linha ACTIVE por atribuição.
CREATE TABLE tb_service_contract_signature (
    signature_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL UNIQUE,
    contract_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    company_id UUID NOT NULL,
    signer_user_id UUID NOT NULL,
    signed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    signed_at_zone VARCHAR(64) NOT NULL,
    signature_type VARCHAR(40) NOT NULL,
    signature_method VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('ACTIVE', 'VOIDED')),
    signed_document_id UUID NULL,
    contract_document_hash_sha256 CHAR(64) NOT NULL,
    signed_pdf_hash_sha256 CHAR(64) NOT NULL,
    declaration_version VARCHAR(40) NOT NULL,
    declaration_hash_sha256 CHAR(64) NOT NULL,
    declaration_text TEXT NOT NULL,
    ip_address VARCHAR(100) NULL,
    user_agent TEXT NULL,
    evidence_json TEXT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NULL,
    voided_at TIMESTAMP WITH TIME ZONE NULL,
    voided_by_user_id UUID NULL,
    void_reason TEXT NULL,
    CONSTRAINT fk_service_contract_signature_assignment
        FOREIGN KEY (assignment_id) REFERENCES tb_service_contract_assignment(assignment_id)
);

CREATE INDEX idx_service_contract_signature_contract_employee
    ON tb_service_contract_signature(contract_id, employee_id);

CREATE INDEX idx_service_contract_signature_assignment
    ON tb_service_contract_signature(assignment_id);
