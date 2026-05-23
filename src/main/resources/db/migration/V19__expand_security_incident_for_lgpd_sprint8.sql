-- Sprint 8: Expandir SecurityIncident para avaliação de risco e relatório
-- Adiciona campos para análise de impacto, decisão de comunicação e medidas corretivas

ALTER TABLE tb_security_incident ADD COLUMN incident_confirmed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tb_security_incident ADD COLUMN data_categories VARCHAR(1000);
ALTER TABLE tb_security_incident ADD COLUMN incident_cause VARCHAR(1000);
ALTER TABLE tb_security_incident ADD COLUMN confidentiality_impact VARCHAR(100);
ALTER TABLE tb_security_incident ADD COLUMN integrity_impact VARCHAR(100);
ALTER TABLE tb_security_incident ADD COLUMN availability_impact VARCHAR(100);
ALTER TABLE tb_security_incident ADD COLUMN risk_to_subjects VARCHAR(1000);
ALTER TABLE tb_security_incident ADD COLUMN communication_required BOOLEAN;
ALTER TABLE tb_security_incident ADD COLUMN anpd_communication_deadline TIMESTAMP;
ALTER TABLE tb_security_incident ADD COLUMN subjects_communication_deadline TIMESTAMP;
ALTER TABLE tb_security_incident ADD COLUMN containment_actions TEXT;
ALTER TABLE tb_security_incident ADD COLUMN corrective_actions TEXT;
ALTER TABLE tb_security_incident ADD COLUMN evidence_links TEXT;

-- Criar tabela de auditoria para relatórios gerados (para rastreamento de quem gerou)
CREATE TABLE IF NOT EXISTS tb_security_incident_report (
    report_id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES tb_security_incident(incident_id) ON DELETE CASCADE,
    report_type VARCHAR(50) NOT NULL, -- JSON, PDF
    generated_by_user_id UUID NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    report_content TEXT,
    UNIQUE(incident_id, report_type, generated_at)
);

-- Índices para performance
CREATE INDEX idx_security_incident_confirmed ON tb_security_incident(incident_confirmed);
CREATE INDEX idx_security_incident_communication_required ON tb_security_incident(communication_required);
CREATE INDEX idx_security_incident_report_incident ON tb_security_incident_report(incident_id);
CREATE INDEX idx_security_incident_report_generated_at ON tb_security_incident_report(generated_at);
