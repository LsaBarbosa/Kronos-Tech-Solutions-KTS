-- Expansão de tb_audit_logs para rastreamento ampliado de LGPD/segurança
ALTER TABLE tb_audit_logs
ADD COLUMN company_id UUID NULL,
ADD COLUMN resource_type VARCHAR(80) NULL,
ADD COLUMN resource_id VARCHAR(80) NULL,
ADD COLUMN correlation_id VARCHAR(80) NULL,
ADD COLUMN risk_level VARCHAR(40) NULL;

CREATE INDEX idx_audit_logs_company_id ON tb_audit_logs(company_id);
CREATE INDEX idx_audit_logs_resource ON tb_audit_logs(resource_type, resource_id);

-- Tabela de incidentes de segurança
-- Permite registrar e rastrear incidentes que envolvam dados pessoais
CREATE TABLE tb_security_incident (
    incident_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    severity VARCHAR(40) NOT NULL,
    personal_data_involved BOOLEAN NOT NULL DEFAULT FALSE,
    sensitive_data_involved BOOLEAN NOT NULL DEFAULT FALSE,
    affected_subjects_estimate INTEGER,
    status VARCHAR(60) NOT NULL,
    notified_anpd_at TIMESTAMPTZ,
    notified_subjects_at TIMESTAMPTZ,
    created_by_user_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ
);

CREATE INDEX idx_security_incident_status ON tb_security_incident(status);
CREATE INDEX idx_security_incident_detected_at ON tb_security_incident(detected_at DESC);
