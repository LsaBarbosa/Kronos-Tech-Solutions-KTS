ALTER TABLE tb_retention_policy
    ADD COLUMN policy_type VARCHAR(40) NOT NULL DEFAULT 'TIME_BASED';
