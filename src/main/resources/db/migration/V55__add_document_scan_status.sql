ALTER TABLE tb_document
    ADD COLUMN scan_status VARCHAR(16) NOT NULL DEFAULT 'CLEAN';

ALTER TABLE tb_document
    ADD COLUMN pending_scan_payload BYTEA;

CREATE INDEX idx_tb_document_pending_scan
    ON tb_document (uploaded_at)
    WHERE scan_status = 'PENDING';
