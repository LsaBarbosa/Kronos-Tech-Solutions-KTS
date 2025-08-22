CREATE TABLE IF NOT EXISTS tb_company (
    company_id     CHAR(36) PRIMARY KEY NOT NULL,
    name_company   VARCHAR(50)  NOT NULL,
    company_cnpj   VARCHAR(17)  NOT NULL,
    company_email  VARCHAR(50)  NOT NULL,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    street      VARCHAR(255),
    number      VARCHAR(20),
    postal_code VARCHAR(20) NOT NULL,
    city        VARCHAR(100),
    state       VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS tb_employee (
    employee_id    CHAR(36) PRIMARY KEY NOT NULL,
    full_name      VARCHAR(200)  NOT NULL,
    cpf            VARCHAR(14)  NOT NULL,
    job_position   VARCHAR(50)  NOT NULL,
    email          VARCHAR(50)  NOT NULL,
    salary         DOUBLE       NOT NULL,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    phone          VARCHAR(15),
    street      VARCHAR(255),
    number      VARCHAR(20),
    postal_code VARCHAR(20) NOT NULL,
    city        VARCHAR(100),
    state       VARCHAR(100),
    company_id     CHAR(36)    NOT NULL,
    CONSTRAINT fk_company_company FOREIGN KEY (company_id)
        REFERENCES tb_company(company_id)
);

CREATE TABLE IF NOT EXISTS tb_time_records (
    time_record_id BIGINT       PRIMARY KEY AUTO_INCREMENT,
    start_work     TIMESTAMP,
    end_work       TIMESTAMP,
    status_record  VARCHAR(30),
    is_edite       BOOLEAN       NOT NULL DEFAULT FALSE,
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    employee_id    CHAR(36)    NOT NULL,
    CONSTRAINT fk_time_record_employee FOREIGN KEY (employee_id)
        REFERENCES tb_employee(employee_id)
);

CREATE TABLE IF NOT EXISTS tb_document (
    document_id   CHAR(36)      PRIMARY KEY NOT NULL,
    employee_id   CHAR(36)      NOT NULL,
    file_name     VARCHAR(255)  NOT NULL,
    content_type  VARCHAR(100)  NOT NULL,
    data          LONGBLOB      NOT NULL,
    uploaded_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_employee
        FOREIGN KEY (employee_id)
        REFERENCES tb_employee(employee_id)
);


CREATE TABLE IF NOT EXISTS tb_document (
    document_id   CHAR(36)      PRIMARY KEY NOT NULL,
    employee_id   CHAR(36)      NOT NULL,
    file_name     VARCHAR(255)  NOT NULL,
    content_type  VARCHAR(100)  NOT NULL,
    data          LONGBLOB      NOT NULL,
    uploaded_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_employee
        FOREIGN KEY (employee_id)
        REFERENCES tb_employee(employee_id)
);

-- Índices importantes
CREATE INDEX idx_company_cnpj ON tb_company(company_cnpj);
CREATE INDEX idx_company_name ON tb_company(name_company);
