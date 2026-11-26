ALTER TABLE tb_employee
    ADD COLUMN weekly_rest_days INTEGER;

ALTER TABLE tb_employee
    ADD CONSTRAINT chk_employee_weekly_rest_days
        CHECK (weekly_rest_days IS NULL OR weekly_rest_days BETWEEN 0 AND 6);
