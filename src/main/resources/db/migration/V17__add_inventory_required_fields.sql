-- Add mandatory LGPD inventory fields for Sprint 6 compliance
-- Fields: description, riskLevel, ripdRequired, version, operators

ALTER TABLE tb_data_processing_inventory
ADD COLUMN description TEXT NULL;

ALTER TABLE tb_data_processing_inventory
ADD COLUMN risk_level VARCHAR(50) NULL;

ALTER TABLE tb_data_processing_inventory
ADD COLUMN ripd_required BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE tb_data_processing_inventory
ADD COLUMN version VARCHAR(20) NULL;

ALTER TABLE tb_data_processing_inventory
ADD COLUMN operators TEXT NULL;

-- Create indexes for common queries
CREATE INDEX idx_inventory_risk_level ON tb_data_processing_inventory(risk_level);
CREATE INDEX idx_inventory_ripd_required ON tb_data_processing_inventory(ripd_required);
