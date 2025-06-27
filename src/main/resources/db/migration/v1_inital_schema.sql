-- Criação das tabelas principais

CREATE TABLE IF NOT EXISTS tb_company (
    company_id BINARY(16) PRIMARY KEY,
    name_company VARCHAR(50) NOT NULL,
    company_cnpj VARCHAR(17) NOT NULL,
    street VARCHAR(255),
    company_email VARCHAR(50) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS tb_address (
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20) NOT NULL,
    number VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS tb_employee (
    employee_id BINARY(16) PRIMARY KEY,
    full_name VARCHAR(50) NOT NULL,
    cpf VARCHAR(14) NOT NULL,
    job_position VARCHAR(50) NOT NULL,
    email VARCHAR(50) NOT NULL,
    password VARCHAR(200) NOT NULL,
    role VARCHAR(50) NOT NULL,
    salary DOUBLE PRECISION NOT NULL,
    phone VARCHAR(15),
    company_id BINARY(16) NOT NULL,
    CONSTRAINT fk_company_company FOREIGN KEY (company_id) REFERENCES tb_company(company_id)
);

CREATE TABLE IF NOT EXISTS tb_time_records (
    time_record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    start_work TIMESTAMP,
    end_work TIMESTAMP,
    status_record VARCHAR(10),
    is_edite BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    employee_id BINARY(16) NOT NULL,
    CONSTRAINT fk_time_record_employee FOREIGN KEY (employee_id) REFERENCES tb_employee(employee_id)
);

CREATE TABLE IF NOT EXISTS tb_user (
    user_id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    role VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    employee_id BINARY(16),
    CONSTRAINT fk_user_employee FOREIGN KEY (employee_id) REFERENCES tb_employee(employee_id)
);

-- Índices importantes
CREATE INDEX idx_company_cnpj ON tb_company(company_cnpj);
CREATE INDEX idx_company_name ON tb_company(name_company);
