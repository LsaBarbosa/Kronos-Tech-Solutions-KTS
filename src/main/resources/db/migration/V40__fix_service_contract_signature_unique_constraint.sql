-- O constraint UNIQUE incondicional em assignment_id impede re-assinatura após void
-- e não protege concorrência de forma semântica correta (TOCTOU).
-- Substituímos por um partial unique index que garante no máximo uma assinatura
-- ACTIVE por atribuição, permitindo nova assinatura se a anterior foi anulada.

ALTER TABLE tb_service_contract_signature
    DROP CONSTRAINT IF EXISTS tb_service_contract_signature_assignment_id_key;

CREATE UNIQUE INDEX IF NOT EXISTS uk_service_contract_signature_active_assignment
    ON tb_service_contract_signature(assignment_id)
    WHERE status = 'ACTIVE';
