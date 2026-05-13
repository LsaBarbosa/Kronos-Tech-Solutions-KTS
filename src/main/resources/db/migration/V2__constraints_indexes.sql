CREATE UNIQUE INDEX IF NOT EXISTS uk_tb_company_company_cnpj ON tb_company (company_cnpj);
CREATE UNIQUE INDEX IF NOT EXISTS uk_tb_employee_cpf ON tb_employee (cpf);
CREATE UNIQUE INDEX IF NOT EXISTS uk_tb_employee_pis ON tb_employee (pis) WHERE pis IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_tb_user_username_lower ON tb_user (LOWER(username));
CREATE UNIQUE INDEX IF NOT EXISTS uk_tb_user_employee_id ON tb_user (employee_id) WHERE employee_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_tb_password_reset_token_user_id ON tb_password_reset_token (user_id);

CREATE INDEX IF NOT EXISTS idx_tb_employee_company_active ON tb_employee (company_id, is_active);
CREATE INDEX IF NOT EXISTS idx_tb_document_employee_type_uploaded ON tb_document (employee_id, document_type, uploaded_at);
CREATE INDEX IF NOT EXISTS idx_tb_time_records_employee_start ON tb_time_records (employee_id, start_work);
CREATE INDEX IF NOT EXISTS idx_tb_time_records_employee_active_start ON tb_time_records (employee_id, is_active, start_work);
CREATE INDEX IF NOT EXISTS idx_afd_nsr_company ON tb_afd_entry (company_id, nsr);
CREATE INDEX IF NOT EXISTS idx_afd_date ON tb_afd_entry (company_id, record_date);
CREATE INDEX IF NOT EXISTS idx_tb_message_company_created ON tb_message (company_id, created_at);
CREATE INDEX IF NOT EXISTS idx_tb_message_employee_created ON tb_message (employee_id, created_at);

ALTER TABLE tb_employee
    ADD CONSTRAINT fk_tb_employee_company
    FOREIGN KEY (company_id) REFERENCES tb_company (company_id);

ALTER TABLE tb_user
    ADD CONSTRAINT fk_tb_user_employee
    FOREIGN KEY (employee_id) REFERENCES tb_employee (employee_id);

ALTER TABLE tb_document
    ADD CONSTRAINT fk_tb_document_employee
    FOREIGN KEY (employee_id) REFERENCES tb_employee (employee_id);

ALTER TABLE tb_time_records
    ADD CONSTRAINT fk_tb_time_records_employee
    FOREIGN KEY (employee_id) REFERENCES tb_employee (employee_id);

ALTER TABLE tb_time_record_approval
    ADD CONSTRAINT fk_tb_time_record_approval_record
    FOREIGN KEY (time_record_id) REFERENCES tb_time_records (time_record_id);

ALTER TABLE tb_afd_entry
    ADD CONSTRAINT fk_tb_afd_entry_company
    FOREIGN KEY (company_id) REFERENCES tb_company (company_id);

ALTER TABLE tb_afd_entry
    ADD CONSTRAINT fk_tb_afd_entry_employee
    FOREIGN KEY (employee_id) REFERENCES tb_employee (employee_id);

ALTER TABLE tb_company_nsr
    ADD CONSTRAINT fk_tb_company_nsr_company
    FOREIGN KEY (company_id) REFERENCES tb_company (company_id);

ALTER TABLE tb_password_reset_token
    ADD CONSTRAINT fk_tb_password_reset_token_user
    FOREIGN KEY (user_id) REFERENCES tb_user (user_id);

ALTER TABLE tb_message
    ADD CONSTRAINT fk_tb_message_employee
    FOREIGN KEY (employee_id) REFERENCES tb_employee (employee_id);

ALTER TABLE tb_message
    ADD CONSTRAINT fk_tb_message_company
    FOREIGN KEY (company_id) REFERENCES tb_company (company_id);
