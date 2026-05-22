CREATE TABLE tb_legal_text (
    legal_text_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_type VARCHAR(80) NOT NULL,
    version VARCHAR(30) NOT NULL,
    title VARCHAR(180) NOT NULL,
    content TEXT NOT NULL,
    content_hash_sha256 VARCHAR(128) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ NULL
);

CREATE UNIQUE INDEX uix_legal_text_type_version
    ON tb_legal_text(document_type, version);

CREATE UNIQUE INDEX uix_legal_text_active_by_type
    ON tb_legal_text(document_type)
    WHERE active = true;

INSERT INTO tb_legal_text (
    legal_text_id,
    document_type,
    version,
    title,
    content,
    content_hash_sha256,
    active,
    created_at,
    published_at
)
VALUES (
    gen_random_uuid(),
    'BIOMETRIC_CONSENT_TERM',
    '2026.05.21',
    'Termo de Consentimento Biométrico',
    $$O TITULAR autoriza, de forma livre, informada e inequívoca, o tratamento de seus dados pessoais sensíveis, especificamente sua imagem facial, para as finalidades de autenticação biométrica, prevenção a fraudes, comprovação de identidade e registro e controle de jornada de trabalho, em conformidade com a Lei Geral de Proteção de Dados (Lei nº 13.709/2018) e a Portaria MTE nº 671/2021.

- FINALIDADE: autenticação segura da identidade em fluxos autorizados da plataforma Kronos, inclusive para registro de ponto e validações antifraude.
- ARMAZENAMENTO: a imagem facial e os templates biométricos poderão ser tratados por provedores de armazenamento e reconhecimento biométrico configurados pela Kronos em ambiente com controle de acesso e registro de auditoria.
- REVOGAÇÃO: este consentimento poderá ser revogado a qualquer momento pelo titular, e a revogação interromperá o uso de login facial e demais validações biométricas dependentes deste consentimento.
- RETENÇÃO: a imagem facial e os templates biométricos serão removidos após a revogação do consentimento ou quando deixarem de ser necessários para a finalidade operacional. O histórico do aceite e o documento de evidência poderão ser preservados conforme política de retenção aplicável e obrigações legais.$$,
    encode(digest($$O TITULAR autoriza, de forma livre, informada e inequívoca, o tratamento de seus dados pessoais sensíveis, especificamente sua imagem facial, para as finalidades de autenticação biométrica, prevenção a fraudes, comprovação de identidade e registro e controle de jornada de trabalho, em conformidade com a Lei Geral de Proteção de Dados (Lei nº 13.709/2018) e a Portaria MTE nº 671/2021.

- FINALIDADE: autenticação segura da identidade em fluxos autorizados da plataforma Kronos, inclusive para registro de ponto e validações antifraude.
- ARMAZENAMENTO: a imagem facial e os templates biométricos poderão ser tratados por provedores de armazenamento e reconhecimento biométrico configurados pela Kronos em ambiente com controle de acesso e registro de auditoria.
- REVOGAÇÃO: este consentimento poderá ser revogado a qualquer momento pelo titular, e a revogação interromperá o uso de login facial e demais validações biométricas dependentes deste consentimento.
- RETENÇÃO: a imagem facial e os templates biométricos serão removidos após a revogação do consentimento ou quando deixarem de ser necessários para a finalidade operacional. O histórico do aceite e o documento de evidência poderão ser preservados conforme política de retenção aplicável e obrigações legais.$$,'sha256'),'hex'),
    TRUE,
    now(),
    now()
);
