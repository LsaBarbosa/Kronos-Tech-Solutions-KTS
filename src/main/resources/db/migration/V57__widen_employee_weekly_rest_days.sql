ALTER TABLE tb_employee
    ALTER COLUMN weekly_rest_days TYPE INTEGER
        USING weekly_rest_days::INTEGER;
