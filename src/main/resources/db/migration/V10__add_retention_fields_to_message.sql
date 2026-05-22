ALTER TABLE tb_message ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE tb_message ADD COLUMN deleted_by_system BOOLEAN DEFAULT FALSE;
ALTER TABLE tb_message ADD COLUMN retention_policy_code VARCHAR(100) NULL;

CREATE INDEX idx_message_deleted_at ON tb_message(deleted_at);
CREATE INDEX idx_message_deleted_by_system ON tb_message(deleted_by_system);
CREATE INDEX idx_message_retention_policy ON tb_message(retention_policy_code);
