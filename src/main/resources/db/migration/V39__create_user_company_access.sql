-- Tabela de acesso usuário-empresa (relação N:N explícita)
CREATE TABLE tb_user_company_access (
    access_id    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL,
    company_id   UUID         NOT NULL,
    employee_id  UUID,
    role         VARCHAR(50)  NOT NULL,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    is_default   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT fk_uca_user
        FOREIGN KEY (user_id) REFERENCES tb_user(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_uca_company
        FOREIGN KEY (company_id) REFERENCES tb_company(company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_uca_employee
        FOREIGN KEY (employee_id) REFERENCES tb_employee(employee_id) ON DELETE SET NULL,
    CONSTRAINT uk_user_company
        UNIQUE (user_id, company_id),
    CONSTRAINT chk_uca_role
        CHECK (role IN ('CTO', 'MANAGER', 'PARTNER'))
);

CREATE INDEX idx_user_company_access_user    ON tb_user_company_access(user_id);
CREATE INDEX idx_user_company_access_company ON tb_user_company_access(company_id);
CREATE INDEX idx_user_company_access_active  ON tb_user_company_access(user_id, is_active);

-- Popula com acessos existentes (migração de dados)
-- Cada usuário existente recebe um registro de acesso padrão para sua empresa atual
INSERT INTO tb_user_company_access
    (access_id, user_id, company_id, employee_id, role, is_active, is_default, created_at)
SELECT
    gen_random_uuid(),
    u.user_id,
    e.company_id,
    u.employee_id,
    u.role,
    u.is_active,
    TRUE,
    CURRENT_TIMESTAMP
FROM tb_user u
JOIN tb_employee e ON u.employee_id = e.employee_id
WHERE u.employee_id IS NOT NULL
  AND u.deleted_at IS NULL;
