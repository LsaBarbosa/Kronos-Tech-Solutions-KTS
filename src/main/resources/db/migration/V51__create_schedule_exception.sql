CREATE TABLE tb_employee_schedule_exception (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id     UUID NOT NULL REFERENCES tb_employee(employee_id) ON DELETE CASCADE,
    exception_date  DATE NOT NULL,
    work_start_time  TIME WITHOUT TIME ZONE,
    work_end_time    TIME WITHOUT TIME ZONE,
    break_start_time TIME WITHOUT TIME ZONE,
    break_end_time   TIME WITHOUT TIME ZONE,
    is_day_off      BOOLEAN NOT NULL DEFAULT FALSE,
    description     VARCHAR(255),
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uix_exception_employee_date UNIQUE (employee_id, exception_date)
);

CREATE INDEX idx_exception_employee_date ON tb_employee_schedule_exception(employee_id, exception_date);
