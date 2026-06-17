-- Aplica os ALTERs de tipo das colunas de hash que V34 deveria ter aplicado
-- mas não aplicou (a versão original de V34 registrada no banco continha conteúdo
-- diferente; flyway repair apenas realinhou o checksum, sem re-executar a migration).
--
-- ALTER COLUMN ... TYPE é seguro de re-executar: se a coluna já estiver como
-- VARCHAR(64), o ALTER vira no-op silencioso. Aqui garantimos que as 4 colunas
-- de hash estão alinhadas com o mapeamento JPA (@Column(length = 64)).

ALTER TABLE tb_service_contract
    ALTER COLUMN document_hash_sha256 TYPE VARCHAR(64);

ALTER TABLE tb_service_contract_signature
    ALTER COLUMN contract_document_hash_sha256 TYPE VARCHAR(64);

ALTER TABLE tb_service_contract_signature
    ALTER COLUMN signed_pdf_hash_sha256 TYPE VARCHAR(64);

ALTER TABLE tb_service_contract_signature
    ALTER COLUMN declaration_hash_sha256 TYPE VARCHAR(64);
