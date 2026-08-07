ALTER TABLE tb_employee
    ADD COLUMN IF NOT EXISTS weekend_work_start_time  TIME WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS weekend_work_end_time    TIME WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS weekend_break_start_time TIME WITHOUT TIME ZONE,
    ADD COLUMN IF NOT EXISTS weekend_break_end_time   TIME WITHOUT TIME ZONE;
