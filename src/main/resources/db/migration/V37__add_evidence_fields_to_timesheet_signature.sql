-- Iguala tb_timesheet_signature ao padrão usado em tb_service_contract_signature
-- (ver V36): mesma planilha de campos de evidência exigida pela política de
-- assinatura eletrônica avançada interna.

ALTER TABLE tb_timesheet_signature
    ADD COLUMN document_type VARCHAR(40);

ALTER TABLE tb_timesheet_signature
    ADD COLUMN document_version VARCHAR(40);

ALTER TABLE tb_timesheet_signature
    ADD COLUMN canonical_evidence_hash_sha256 VARCHAR(64);

ALTER TABLE tb_timesheet_signature
    ADD COLUMN audit_log_id UUID;

ALTER TABLE tb_timesheet_signature
    ADD COLUMN pades_signature_status VARCHAR(40);

-- Backfill para linhas pré-existentes
UPDATE tb_timesheet_signature
SET document_type = 'POINT_MIRROR'
WHERE document_type IS NULL;

UPDATE tb_timesheet_signature
SET document_version = declaration_version
WHERE document_version IS NULL;

UPDATE tb_timesheet_signature
SET canonical_evidence_hash_sha256 = point_mirror_hash_sha256
WHERE canonical_evidence_hash_sha256 IS NULL;

UPDATE tb_timesheet_signature
SET pades_signature_status = 'UNKNOWN'
WHERE pades_signature_status IS NULL;

ALTER TABLE tb_timesheet_signature
    ALTER COLUMN document_type SET NOT NULL;

ALTER TABLE tb_timesheet_signature
    ALTER COLUMN document_version SET NOT NULL;

ALTER TABLE tb_timesheet_signature
    ALTER COLUMN canonical_evidence_hash_sha256 SET NOT NULL;

ALTER TABLE tb_timesheet_signature
    ALTER COLUMN pades_signature_status SET NOT NULL;
