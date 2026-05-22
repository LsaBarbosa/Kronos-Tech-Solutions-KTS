ALTER TABLE tb_document ADD COLUMN deleted_by_retention BOOLEAN DEFAULT FALSE;
ALTER TABLE tb_document ADD COLUMN retention_deleted_at TIMESTAMP NULL;
ALTER TABLE tb_document ADD COLUMN retention_policy_code VARCHAR(100) NULL;

CREATE INDEX idx_document_deleted_by_retention ON tb_document(deleted_by_retention);
CREATE INDEX idx_document_retention_deleted_at ON tb_document(retention_deleted_at);
CREATE INDEX idx_document_retention_policy ON tb_document(retention_policy_code);
