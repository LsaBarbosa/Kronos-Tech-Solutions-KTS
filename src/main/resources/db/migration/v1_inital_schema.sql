CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS tb_company (
    company_id     UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    name_company   VARCHAR(50)  NOT NULL,
    company_cnpj   VARCHAR(17)  NOT NULL,
    company_email  VARCHAR(50)  NOT NULL,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    street         VARCHAR(255),
    number         VARCHAR(20),
    postal_code    VARCHAR(20) NOT NULL,
    city           VARCHAR(100),
    state          VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS tb_employee (
    employee_id    UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    full_name      VARCHAR(200)  NOT NULL,
    cpf            VARCHAR(14)  NOT NULL,
    job_position   VARCHAR(50)  NOT NULL,
    email          VARCHAR(50)  NOT NULL,
    salary         DOUBLE PRECISION NOT NULL,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    phone          VARCHAR(15),
    street         VARCHAR(255),
    number         VARCHAR(20),
    postal_code    VARCHAR(20) NOT NULL,
    city           VARCHAR(100),
    state          VARCHAR(100),
    company_id     UUID    NOT NULL,
    CONSTRAINT fk_company_company FOREIGN KEY (company_id)
        REFERENCES tb_company(company_id)
);

CREATE TABLE IF NOT EXISTS tb_time_records (
    time_record_id SERIAL PRIMARY KEY,
    start_work     TIMESTAMP,
    end_work       TIMESTAMP,
    status_record  VARCHAR(30),
    is_edite       BOOLEAN       NOT NULL DEFAULT FALSE,
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    employee_id    UUID    NOT NULL,
    CONSTRAINT fk_time_record_employee FOREIGN KEY (employee_id)
        REFERENCES tb_employee(employee_id)
);

CREATE TABLE IF NOT EXISTS tb_document (
    document_id   UUID      PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    employee_id   UUID      NOT NULL,
    file_name     VARCHAR(255)  NOT NULL,
    content_type  VARCHAR(100)  NOT NULL,
    data          BYTEA      NOT NULL,
    uploaded_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    document_type VARCHAR(255) NOT NULL,
    CONSTRAINT fk_document_employee
        FOREIGN KEY (employee_id)
        REFERENCES tb_employee(employee_id)
);

CREATE TABLE IF NOT EXISTS tb_user (
    user_id UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    employee_id UUID NOT NULL,
    CONSTRAINT fk_user_employee FOREIGN KEY (employee_id)
        REFERENCES tb_employee(employee_id)
);

CREATE TABLE IF NOT EXISTS tb_message (
    message_id      UUID PRIMARY KEY NOT NULL DEFAULT uuid_generate_v4(),
    employee_id     UUID NOT NULL,
    company_id      UUID NOT NULL,
    message_text    VARCHAR(1024) NOT NULL,
    priority        VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_message_employee FOREIGN KEY (employee_id)
        REFERENCES tb_employee(employee_id),
    CONSTRAINT fk_message_company FOREIGN KEY (company_id)
        REFERENCES tb_company(company_id)
);

-- Índices importantes
CREATE INDEX IF NOT EXISTS idx_message_company_id ON tb_message(company_id);
CREATE INDEX IF NOT EXISTS idx_company_cnpj ON tb_company(company_cnpj);
CREATE INDEX IF NOT EXISTS idx_company_name ON tb_company(name_company);