-- Remove a constraint global de CPF (que impede o mesmo CPF em empresas diferentes)
ALTER TABLE tb_employee DROP CONSTRAINT IF EXISTS uk_tb_employee_cpf;

-- Remove o index composto que inclui cpf como único
DROP INDEX IF EXISTS idx_emp_cpf_email_lower;

-- Verifica se já existem duplicatas de (company_id, cpf) excluindo deletados
-- Se existirem, a migration falha aqui com erro descritivo
DO $$
DECLARE
    dup_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO dup_count
    FROM (
        SELECT company_id, cpf, COUNT(*) as cnt
        FROM tb_employee
        WHERE deleted_at IS NULL
        GROUP BY company_id, cpf
        HAVING COUNT(*) > 1
    ) dups;

    IF dup_count > 0 THEN
        RAISE EXCEPTION 'Existem % par(es) company_id+cpf duplicados em tb_employee. Resolva antes de aplicar esta migration.', dup_count;
    END IF;
END $$;

-- Cria unique index composto: dentro da mesma empresa, CPF deve ser único (exceto deletados)
CREATE UNIQUE INDEX uk_employee_company_cpf
    ON tb_employee(company_id, cpf)
    WHERE deleted_at IS NULL;

-- Recria o índice de cpf+email (sem unicidade) para buscas por CPF
CREATE INDEX idx_emp_cpf_email_lower ON tb_employee (cpf, LOWER(email));
