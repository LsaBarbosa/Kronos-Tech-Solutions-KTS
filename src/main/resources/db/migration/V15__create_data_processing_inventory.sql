CREATE TABLE tb_data_processing_inventory (
    inventory_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    process_code VARCHAR(100) NOT NULL UNIQUE,
    process_name VARCHAR(255) NOT NULL,
    data_category VARCHAR(255) NOT NULL,
    data_fields TEXT NOT NULL,
    data_subject_category VARCHAR(255) NOT NULL,
    purpose TEXT NOT NULL,
    legal_basis VARCHAR(255) NOT NULL,
    sensitive_data BOOLEAN NOT NULL DEFAULT false,
    source_system VARCHAR(255) NOT NULL,
    storage_location VARCHAR(255),
    retention_policy_code VARCHAR(100),
    external_sharing VARCHAR(255),
    international_transfer BOOLEAN NOT NULL DEFAULT false,
    security_measures TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_data_processing_inventory_process_code ON tb_data_processing_inventory(process_code);
CREATE INDEX idx_data_processing_inventory_active ON tb_data_processing_inventory(active);
CREATE INDEX idx_data_processing_inventory_created_at ON tb_data_processing_inventory(created_at DESC);

INSERT INTO tb_data_processing_inventory (process_code, process_name, data_category, data_fields, data_subject_category, purpose, legal_basis, sensitive_data, source_system, storage_location, retention_policy_code, external_sharing, international_transfer, security_measures, active, created_at, updated_at)
VALUES
('AUTH_PASSWORD_LOGIN', 'Autenticação por Senha', 'Credenciais', 'email, senha_hash, tentativas_falhas, ultimo_login', 'Funcionário', 'Autenticação de Usuário', 'Execução de Contrato', false, 'Kronos Identity Service', 'Banco de Dados Postgresql', 'RETENTION_AUTHENTICATION', 'Não', false, 'Hash Seguro (BCRYPT), Conexão TLS', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('AUTH_FACE_LOGIN', 'Autenticação Biométrica por Rosto', 'Biometria Facial', 'template_facial_criptografado, qualidade_captura, timestamp_verificacao', 'Funcionário', 'Autenticação Biométrica', 'Consentimento do Titular', true, 'Kronos Biometric Service', 'Banco de Dados Postgresql', 'RETENTION_BIOMETRIC', 'Não', false, 'Criptografia AES-256, Template Não Reconstruível', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('FACE_ENROLLMENT', 'Inscrição Biométrica Facial', 'Biometria Facial', 'imagem_facial, template_facial_criptografado, data_inscricao, qualidade_imagem', 'Funcionário', 'Cadastro para Autenticação Biométrica', 'Consentimento do Titular', true, 'Kronos Biometric Service', 'Banco de Dados Postgresql', 'RETENTION_BIOMETRIC', 'Não', false, 'Armazenamento de Template Criptografado, Imagem Original Deletada', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TIME_RECORD_CHECKIN', 'Registro de Ponto por Apresentação', 'Localização Aproximada, Timestamps', 'employee_id, timestamp_checkin, location_aproximada, device_type, ip_address', 'Funcionário', 'Registro de Jornada de Trabalho', 'Obrigação Legal (CLT)', false, 'Kronos Time & Attendance', 'Banco de Dados Postgresql', 'RETENTION_TIMESHEET', 'Órgãos Governamentais (Conforme Lei)', false, 'Acesso Restrito por RBAC, Auditoria de Acesso', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('TIME_RECORD_GEOLOCATION', 'Registro de Ponto com Geolocalização Precisa', 'Localização Precisa', 'employee_id, latitude, longitude, timestamp, accuracia, tipo_dispositivo', 'Funcionário', 'Rastreamento de Localização em Campo', 'Consentimento do Titular', true, 'Kronos Time & Attendance', 'Banco de Dados Postgresql', 'RETENTION_GEOLOCATION', 'Não', false, 'GPS Encriptado, Consentimento Explícito Necessário', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('EMPLOYEE_MANAGEMENT', 'Gestão de Dados de Funcionários', 'Dados Pessoais, Dados Profissionais', 'employee_id, nome, cpf, email, telefone, endereco, data_nascimento, funcao, departamento, data_admissao, salario', 'Funcionário', 'Gestão de Recursos Humanos', 'Execução de Contrato', false, 'Sistema de RH Kronos', 'Banco de Dados Postgresql', 'RETENTION_EMPLOYEE_RECORD', 'Departamento de Folha de Pagamento', false, 'Criptografia de Dados Sensíveis, Acesso Restrito por Departamento', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('DOCUMENT_MANAGEMENT', 'Gestão de Documentos', 'Documentos Diversos', 'document_id, documento_tipo, proprietario_id, conteudo_hash, data_upload, versao', 'Funcionário', 'Armazenamento e Gestão de Documentos', 'Execução de Contrato', false, 'Document Management System', 'Armazenamento S3', 'RETENTION_DOCUMENT', 'Sob Aprovação', false, 'Criptografia End-to-End, Versionamento com Auditoria', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('LEGAL_REPORT_AFD', 'Relatório Afastamento Doença (AFD)', 'Dados de Saúde, Dados Pessoais', 'employee_id, data_afastamento, data_retorno, tipo_afastamento, dias_afastado', 'Funcionário', 'Gestão de Afastamentos por Doença', 'Obrigação Legal (Previdência Social)', true, 'Sistema de RH', 'Banco de Dados Postgresql', 'RETENTION_LEGAL_SENSITIVE', 'INSS, Médicos Autorizados', false, 'Criptografia de Dados de Saúde, Acesso Restrito', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('LEGAL_REPORT_AEJ', 'Relatório Afastamento por Evento Judicial', 'Dados Pessoais, Dados Legais', 'employee_id, data_afastamento, motivo_judicial, data_retorno, numero_processo', 'Funcionário', 'Gestão de Afastamentos Judiciais', 'Obrigação Legal (Legislação Trabalhista)', false, 'Sistema de RH', 'Banco de Dados Postgresql', 'RETENTION_LEGAL', 'Órgãos Judiciais Envolvidos', false, 'Acesso Restrito por RBAC, Auditoria de Acesso', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('LEGAL_REPORT_POINT_MIRROR', 'Relatório Espelho de Ponto', 'Registro de Jornada', 'employee_id, data, horario_entrada, horario_saida, horas_trabalhadas, banco_horas', 'Funcionário', 'Registro Oficial de Jornada de Trabalho', 'Obrigação Legal (CLT)', false, 'Kronos Time & Attendance', 'Banco de Dados Postgresql', 'RETENTION_TIMESHEET', 'Órgãos Governamentais (Conforme Lei)', false, 'Auditoria de Qualquer Alteração, Assinatura Digital', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('PASSWORD_RECOVERY', 'Recuperação de Senha', 'Email, Token Temporário', 'employee_id, recovery_token_hash, token_expiry, email_destino, ip_request', 'Funcionário', 'Recuperação de Acesso à Conta', 'Interesse Legítimo (Segurança)', false, 'Kronos Identity Service', 'Cache com TTL', 'RETENTION_TEMPORARY', 'Não', false, 'Token com Expiração Curta (15min), Hash Seguro', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('MESSAGE_MANAGEMENT', 'Gestão de Mensagens Internas', 'Conteúdo de Mensagens, Metadados', 'message_id, sender_id, recipient_id, conteudo, timestamp, lido', 'Funcionário', 'Comunicação Interna', 'Interesse Legítimo (Operações Comerciais)', false, 'Sistema de Mensagens Kronos', 'Banco de Dados Postgresql', 'RETENTION_MESSAGE', 'Não', false, 'Acesso Restrito aos Participantes, Criptografia em Trânsito', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('LGPD_REQUEST_MANAGEMENT', 'Gestão de Solicitações LGPD', 'Dados de Solicitação LGPD', 'request_id, employee_id, tipo_solicitacao, data_solicitacao, status, dados_solicitados', 'Titular de Dados', 'Cumprimento de Direitos LGPD', 'Obrigação Legal (LGPD)', false, 'LGPD Management System', 'Banco de Dados Postgresql', 'RETENTION_LGPD_REQUEST', 'Não', false, 'Auditoria Completa, Criptografia de Sensíveis', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('SECURITY_INCIDENT_MANAGEMENT', 'Gestão de Incidentes de Segurança', 'Logs de Segurança, Incidentes', 'incident_id, tipo_incidente, data_incidente, descricao, usuario_afetado, acao_tomada', 'Funcionário', 'Resposta a Incidentes de Segurança', 'Obrigação Legal (Segurança da Informação)', false, 'Security Incident System', 'Banco de Dados Postgresql', 'RETENTION_SECURITY', 'Time de Segurança Autorizado', false, 'Auditoria de Acesso, Criptografia de Dados Sensíveis', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('AUDIT_LOGGING', 'Auditoria de Acesso e Ações', 'Logs de Sistema, Ações de Usuários', 'audit_id, usuario_id, acao, recurso, timestamp, resultado, ip_address', 'Funcionário', 'Conformidade e Auditoria Interna', 'Obrigação Legal (Conformidade)', false, 'Auditoria Centralizada', 'Banco de Dados Postgresql', 'RETENTION_AUDIT', 'Órgãos Reguladores (Conforme Lei)', false, 'Imutabilidade de Logs, Criptografia de Acesso', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
