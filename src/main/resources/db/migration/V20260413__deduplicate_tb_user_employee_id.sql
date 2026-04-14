-- Sprint 3 - Integridade de conta, reset e vínculo usuário-colaborador
-- Banco alvo: PostgreSQL

-- Feature 3.3 - Endurecer persistência de token reset
ALTER TABLE tb_password_reset_token
    ALTER COLUMN token TYPE VARCHAR(64);

-- Feature 3.4 - Garantir integridade entre usuário e colaborador
-- Falha explicitamente se existir duplicidade antes de criar a constraint.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM tb_user
        WHERE employee_id IS NOT NULL
        GROUP BY employee_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION
            'Existem registros duplicados em tb_user.employee_id. Corrija os dados antes de aplicar a constraint UNIQUE.';
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conrelid = 'tb_user'::regclass
          AND contype = 'u'
          AND conname = 'uk_tb_user_employee_id'
    ) THEN
        ALTER TABLE tb_user
            ADD CONSTRAINT uk_tb_user_employee_id UNIQUE (employee_id);
    END IF;
END $$;