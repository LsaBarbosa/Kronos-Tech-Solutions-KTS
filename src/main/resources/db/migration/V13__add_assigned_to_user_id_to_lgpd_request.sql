ALTER TABLE tb_lgpd_request
    ADD COLUMN assigned_to_user_id UUID REFERENCES tb_user(user_id);

CREATE INDEX idx_lgpd_request_assigned_to
    ON tb_lgpd_request(assigned_to_user_id);
