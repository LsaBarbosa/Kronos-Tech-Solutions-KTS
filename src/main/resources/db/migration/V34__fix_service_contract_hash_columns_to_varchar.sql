-- Alinha tipos de coluna de hash com o mapeamento JPA (@Column(length=64) → VARCHAR(64)).
-- A V33 criou essas colunas como CHAR(64), que o PostgreSQL armazena como bpchar e
-- causa SchemaManagementException no schema-validation do Hibernate.

ALTER TABLE tb_service_contract
    ALTER COLUMN document_hash_sha256 TYPE VARCHAR(64);

ALTER TABLE tb_service_contract_signature
    ALTER COLUMN contract_document_hash_sha256 TYPE VARCHAR(64);

ALTER TABLE tb_service_contract_signature
    ALTER COLUMN signed_pdf_hash_sha256 TYPE VARCHAR(64);

ALTER TABLE tb_service_contract_signature
    ALTER COLUMN declaration_hash_sha256 TYPE VARCHAR(64);
