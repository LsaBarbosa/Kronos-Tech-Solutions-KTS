-- Kronos local schema - branch PROD_HOSTINGER
-- Target: PostgreSQL + Flyway
-- Path suggested: src/main/resources/db/migration/V1__create_kronos_schema.sql

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================================
-- 1. Empresas / tenants
-- =========================================================
CREATE TABLE tb_company (
    company_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name_company         VARCHAR(50)  NOT NULL,
    company_cnpj         VARCHAR(17)  NOT NULL,
    company_email        VARCHAR(50)  NOT NULL,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,

    street               VARCHAR(255),
    number               VARCHAR(20),
    postal_code          VARCHAR(20)  NOT NULL,
    city                 VARCHAR(100),
    state                VARCHAR(100),

    latitude             DOUBLE PRECISION,
    longitude            DOUBLE PRECISION,

    deleted_at           TIMESTAMP WITHOUT TIME ZONE,
    deleted_by           UUID,
    deactivation_reason  VARCHAR(255),

    CONSTRAINT uk_tb_company_cnpj UNIQUE (company_cnpj),
    CONSTRAINT chk_tb_company_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_tb_company_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
);

CREATE INDEX idx_company_name ON tb_company (name_company);
CREATE INDEX idx_company_active ON tb_company (is_active);
CREATE INDEX idx_company_deleted_at ON tb_company (deleted_at);

-- =========================================================
-- 2. Colaboradores
-- =========================================================
CREATE TABLE tb_employee (
    employee_id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name                    VARCHAR(200) NOT NULL,
    cpf                          VARCHAR(14)  NOT NULL,
    pis                          VARCHAR(14),
    job_position                 VARCHAR(50)  NOT NULL,
    email                        VARCHAR(50)  NOT NULL,
    salary                       DOUBLE PRECISION NOT NULL,
    phone                        VARCHAR(15),
    is_active                    BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_message_timestamp  TIMESTAMP WITHOUT TIME ZONE,
    is_home_office               BOOLEAN NOT NULL DEFAULT FALSE,
    face_s3_object_key           VARCHAR(512),

    street                       VARCHAR(255),
    number                       VARCHAR(20),
    postal_code                  VARCHAR(20) NOT NULL,
    city                         VARCHAR(100),
    state                        VARCHAR(100),

    company_id                   UUID NOT NULL,

    work_start_time              TIME WITHOUT TIME ZONE,
    work_end_time                TIME WITHOUT TIME ZONE,
    break_start_time             TIME WITHOUT TIME ZONE,
    break_end_time               TIME WITHOUT TIME ZONE,
    schedule_type                VARCHAR(50),
    scale_start_date             DATE,
    preferred_day_off            VARCHAR(20),
    weekend_off_index            INTEGER,
    fixed_work_days              TEXT,

    deleted_at                   TIMESTAMP WITHOUT TIME ZONE,
    deleted_by                   UUID,
    deactivation_reason          VARCHAR(255),

    CONSTRAINT fk_employee_company
        FOREIGN KEY (company_id) REFERENCES tb_company (company_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uk_tb_employee_cpf UNIQUE (cpf),
    CONSTRAINT uk_tb_employee_pis UNIQUE (pis),
    CONSTRAINT chk_tb_employee_salary_non_negative CHECK (salary >= 0)
);

CREATE INDEX idx_emp_company ON tb_employee (company_id);
CREATE INDEX idx_emp_company_active ON tb_employee (company_id, is_active);
CREATE INDEX idx_emp_full_name_lower ON tb_employee (LOWER(full_name));
CREATE INDEX idx_emp_cpf_email_lower ON tb_employee (cpf, LOWER(email));
CREATE INDEX idx_emp_deleted_at ON tb_employee (deleted_at);

-- =========================================================
-- 3. Usuários de acesso
-- =========================================================
CREATE TABLE tb_user (
    user_id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username             VARCHAR(50)  NOT NULL,
    password             VARCHAR(200) NOT NULL,
    role                 VARCHAR(50)  NOT NULL,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    employee_id          UUID         NOT NULL,

    deleted_at           TIMESTAMP WITHOUT TIME ZONE,
    deleted_by           UUID,
    deactivation_reason  VARCHAR(255),

    CONSTRAINT fk_user_employee
        FOREIGN KEY (employee_id) REFERENCES tb_employee (employee_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT uk_tb_user_username UNIQUE (username),
    CONSTRAINT uk_tb_user_employee UNIQUE (employee_id),
    CONSTRAINT chk_tb_user_role CHECK (role IN ('CTO', 'MANAGER', 'PARTNER'))
);

CREATE UNIQUE INDEX uix_tb_user_username_lower ON tb_user (LOWER(username));
CREATE INDEX idx_tb_user_employee_id ON tb_user (employee_id);
CREATE INDEX idx_user_active ON tb_user (is_active);
CREATE INDEX idx_user_deleted_at ON tb_user (deleted_at);

-- =========================================================
-- 4. Tokens de redefinição de senha
-- =========================================================
CREATE TABLE tb_password_reset_token (
    token        VARCHAR(64) PRIMARY KEY,
    user_id      UUID NOT NULL,
    expiry_date  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_token_user
        FOREIGN KEY (user_id) REFERENCES tb_user (user_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT uk_tb_password_reset_token_user UNIQUE (user_id)
);

CREATE INDEX idx_token_expiry ON tb_password_reset_token (token, expiry_date);
CREATE INDEX idx_token_expiry_date ON tb_password_reset_token (expiry_date);

-- =========================================================
-- 5. Registros de ponto
-- =========================================================
CREATE TABLE tb_time_records (
    time_record_id       BIGSERIAL PRIMARY KEY,
    start_work           TIMESTAMP WITHOUT TIME ZONE,
    end_work             TIMESTAMP WITHOUT TIME ZONE,
    original_start_work  TIMESTAMP WITHOUT TIME ZONE,
    original_end_work    TIMESTAMP WITHOUT TIME ZONE,
    status_record        VARCHAR(50),
    is_edite             BOOLEAN NOT NULL DEFAULT FALSE,
    is_active            BOOLEAN NOT NULL DEFAULT TRUE,
    employee_id          UUID NOT NULL,
    latitude             DOUBLE PRECISION,
    longitude            DOUBLE PRECISION,
    end_latitude         DOUBLE PRECISION,
    end_longitude        DOUBLE PRECISION,
    nsr_checkin          BIGINT,
    nsr_checkout         BIGINT,

    CONSTRAINT fk_time_record_employee
        FOREIGN KEY (employee_id) REFERENCES tb_employee (employee_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_tb_time_record_start_end CHECK (
        end_work IS NULL OR start_work IS NULL OR end_work >= start_work
    ),
    CONSTRAINT chk_tb_time_record_latitude CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_tb_time_record_longitude CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180),
    CONSTRAINT chk_tb_time_record_end_latitude CHECK (end_latitude IS NULL OR end_latitude BETWEEN -90 AND 90),
    CONSTRAINT chk_tb_time_record_end_longitude CHECK (end_longitude IS NULL OR end_longitude BETWEEN -180 AND 180)
);

CREATE INDEX idx_tr_employee_open
    ON tb_time_records (employee_id, start_work DESC)
    WHERE end_work IS NULL;

CREATE INDEX idx_tr_employee_start_desc
    ON tb_time_records (employee_id, start_work DESC);

CREATE INDEX idx_tr_employee_status_start
    ON tb_time_records (employee_id, status_record, start_work);

CREATE INDEX idx_tr_employee_active_start
    ON tb_time_records (employee_id, is_active, start_work DESC);

CREATE INDEX idx_tb_time_records_report_opt
    ON tb_time_records (employee_id, (start_work::date), status_record)
    WHERE is_active = TRUE;

CREATE INDEX idx_tr_status_start_not_null
    ON tb_time_records (status_record, start_work DESC, employee_id)
    WHERE start_work IS NOT NULL;

CREATE INDEX idx_tr_nsr_checkin
    ON tb_time_records (employee_id, nsr_checkin)
    WHERE nsr_checkin IS NOT NULL;

CREATE INDEX idx_tr_nsr_checkout
    ON tb_time_records (employee_id, nsr_checkout)
    WHERE nsr_checkout IS NOT NULL;

-- =========================================================
-- 6. Documentos
-- =========================================================
CREATE TABLE tb_document (
    document_id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id          UUID NOT NULL,
    file_name            VARCHAR(255) NOT NULL,
    content_type         VARCHAR(255) NOT NULL,
    time_record_id       BIGINT,
    storage_path         VARCHAR(512) NOT NULL,
    uploaded_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    document_type        VARCHAR(255) NOT NULL,
    deleted_by_employee  BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_by_manager   BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_document_employee
        FOREIGN KEY (employee_id) REFERENCES tb_employee (employee_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_document_time_record
        FOREIGN KEY (time_record_id) REFERENCES tb_time_records (time_record_id)
        ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE INDEX idx_tb_document_employee_type
    ON tb_document (employee_id, document_type);

CREATE INDEX idx_doc_emp_type_uploaded
    ON tb_document (employee_id, document_type, uploaded_at DESC);

CREATE INDEX idx_doc_emp_type_uploaded_emp
    ON tb_document (employee_id, document_type, uploaded_at DESC)
    WHERE deleted_by_employee = FALSE;

CREATE INDEX idx_doc_emp_type_uploaded_mgr
    ON tb_document (employee_id, document_type, uploaded_at DESC)
    WHERE deleted_by_manager = FALSE;

CREATE INDEX idx_document_time_record_id
    ON tb_document (time_record_id);

CREATE INDEX idx_document_time_record_id_in
    ON tb_document (time_record_id, uploaded_at DESC)
    WHERE time_record_id IS NOT NULL;

-- =========================================================
-- 7. Mensagens internas
-- =========================================================
CREATE TABLE tb_message (
    message_id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id            UUID NOT NULL,
    company_id             UUID NOT NULL,
    title                  VARCHAR(256) NOT NULL,
    message_text           VARCHAR(1024) NOT NULL,
    priority               VARCHAR(20) NOT NULL,
    created_at             TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    recipient_employee_id  UUID,

    CONSTRAINT fk_message_sender_employee
        FOREIGN KEY (employee_id) REFERENCES tb_employee (employee_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_message_company
        FOREIGN KEY (company_id) REFERENCES tb_company (company_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_message_recipient_employee
        FOREIGN KEY (recipient_employee_id) REFERENCES tb_employee (employee_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_tb_message_priority CHECK (priority IN ('NORMAL', 'ALERT', 'CRITICAL'))
);

CREATE INDEX idx_message_company_id
    ON tb_message (company_id);

CREATE INDEX idx_msg_company_employee_created
    ON tb_message (company_id, employee_id, created_at DESC);

CREATE INDEX idx_msg_company_recipient_created
    ON tb_message (company_id, recipient_employee_id, created_at DESC);

CREATE INDEX idx_message_created_at
    ON tb_message (created_at);

-- =========================================================
-- 8. Aprovação de alteração de ponto
-- =========================================================
CREATE TABLE tb_time_record_approval (
    time_record_id          BIGINT PRIMARY KEY,
    requesting_employee_id  UUID NOT NULL,
    manager_id              UUID NOT NULL,
    new_start_work          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    new_end_work            TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at              TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_approval_time_record
        FOREIGN KEY (time_record_id) REFERENCES tb_time_records (time_record_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_approval_requesting_employee
        FOREIGN KEY (requesting_employee_id) REFERENCES tb_employee (employee_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_approval_manager_user
        FOREIGN KEY (manager_id) REFERENCES tb_user (user_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_tb_approval_start_end CHECK (new_end_work >= new_start_work)
);

CREATE INDEX idx_approval_requesting_employee
    ON tb_time_record_approval (requesting_employee_id);

CREATE INDEX idx_approval_manager
    ON tb_time_record_approval (manager_id);

CREATE INDEX idx_approval_created_at
    ON tb_time_record_approval (created_at DESC);

-- =========================================================
-- 9. Controle de NSR por empresa
-- =========================================================
CREATE TABLE tb_company_nsr (
    company_id  UUID PRIMARY KEY,
    last_nsr    BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_company_nsr_company
        FOREIGN KEY (company_id) REFERENCES tb_company (company_id)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT chk_tb_company_nsr_non_negative CHECK (last_nsr >= 0)
);

-- =========================================================
-- 10. AFD / trilha fiscal
-- =========================================================
CREATE TABLE tb_afd_entry (
    afd_id         BIGSERIAL PRIMARY KEY,
    nsr            BIGINT NOT NULL,
    record_type    VARCHAR(1) NOT NULL,
    record_date    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    employee_cpf   VARCHAR(11) NOT NULL,
    employee_pis   VARCHAR(11),
    company_id     UUID NOT NULL,
    employee_id    UUID NOT NULL,
    previous_hash  VARCHAR(64),
    current_hash   VARCHAR(64) NOT NULL
);

CREATE UNIQUE INDEX uix_afd_company_nsr
    ON tb_afd_entry (company_id, nsr);

CREATE INDEX idx_afd_nsr_company
    ON tb_afd_entry (company_id, nsr);

CREATE INDEX idx_afd_date
    ON tb_afd_entry (company_id, record_date);

CREATE INDEX idx_afd_company_employee_date
    ON tb_afd_entry (company_id, employee_id, record_date DESC);

-- =========================================================
-- 11. Auditoria
-- =========================================================
CREATE TABLE tb_audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    action      VARCHAR(255) NOT NULL,
    ip_address  VARCHAR(255),
    user_agent  VARCHAR(255),
    details     TEXT,
    timestamp   TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_logs_user_id
    ON tb_audit_logs (user_id);

CREATE INDEX idx_audit_logs_timestamp
    ON tb_audit_logs (timestamp DESC);

CREATE INDEX idx_audit_logs_action_timestamp
    ON tb_audit_logs (action, timestamp DESC);
