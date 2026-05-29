ALTER TABLE tb_audit_logs
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE tb_audit_logs
    ADD COLUMN target_employee_id UUID NULL;

CREATE INDEX idx_audit_logs_target_employee_id
    ON tb_audit_logs (target_employee_id);
