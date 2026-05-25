ALTER TABLE tb_audit_logs ADD COLUMN minimized_at TIMESTAMP;
CREATE INDEX idx_tb_audit_logs_minimized_at ON tb_audit_logs (minimized_at);
