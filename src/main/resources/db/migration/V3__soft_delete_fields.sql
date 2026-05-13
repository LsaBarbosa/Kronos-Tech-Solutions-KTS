ALTER TABLE tb_user
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by UUID,
    ADD COLUMN IF NOT EXISTS deactivation_reason VARCHAR(255);

ALTER TABLE tb_employee
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by UUID,
    ADD COLUMN IF NOT EXISTS deactivation_reason VARCHAR(255);

ALTER TABLE tb_company
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by UUID,
    ADD COLUMN IF NOT EXISTS deactivation_reason VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_tb_user_active_deleted_at ON tb_user (is_active, deleted_at);
CREATE INDEX IF NOT EXISTS idx_tb_employee_active_deleted_at ON tb_employee (is_active, deleted_at);
CREATE INDEX IF NOT EXISTS idx_tb_company_active_deleted_at ON tb_company (is_active, deleted_at);
