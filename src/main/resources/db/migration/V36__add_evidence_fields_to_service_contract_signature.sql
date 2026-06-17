-- Acrescenta campos de evidência exigidos pela política de assinatura:
--  - document_type: tipo do documento assinado (constante SERVICE_CONTRACT por enquanto)
--  - document_version: versão do documento/declaração aceita
--  - canonical_evidence_hash_sha256: hash SHA-256 do JSON canônico de todas as
--      evidências (rastreabilidade independente do PDF)
--  - audit_log_id: referência à linha em tb_audit_logs criada no ato de assinar
--  - pades_signature_status: status da assinatura PAdES aplicada
--      (SUCCESS / FAILED / NOT_APPLIED)

ALTER TABLE tb_service_contract_signature
    ADD COLUMN document_type VARCHAR(40);

ALTER TABLE tb_service_contract_signature
    ADD COLUMN document_version VARCHAR(40);

ALTER TABLE tb_service_contract_signature
    ADD COLUMN canonical_evidence_hash_sha256 VARCHAR(64);

ALTER TABLE tb_service_contract_signature
    ADD COLUMN audit_log_id UUID;

ALTER TABLE tb_service_contract_signature
    ADD COLUMN pades_signature_status VARCHAR(40);

-- Backfill para linhas pré-existentes (assinaturas criadas antes desta migration)
UPDATE tb_service_contract_signature
SET document_type = 'SERVICE_CONTRACT'
WHERE document_type IS NULL;

UPDATE tb_service_contract_signature
SET document_version = declaration_version
WHERE document_version IS NULL;

UPDATE tb_service_contract_signature
SET canonical_evidence_hash_sha256 = signed_pdf_hash_sha256
WHERE canonical_evidence_hash_sha256 IS NULL;

UPDATE tb_service_contract_signature
SET pades_signature_status = 'UNKNOWN'
WHERE pades_signature_status IS NULL;

-- Tornar NOT NULL os campos obrigatórios após backfill
ALTER TABLE tb_service_contract_signature
    ALTER COLUMN document_type SET NOT NULL;

ALTER TABLE tb_service_contract_signature
    ALTER COLUMN document_version SET NOT NULL;

ALTER TABLE tb_service_contract_signature
    ALTER COLUMN canonical_evidence_hash_sha256 SET NOT NULL;

ALTER TABLE tb_service_contract_signature
    ALTER COLUMN pades_signature_status SET NOT NULL;
