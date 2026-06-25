-- V43 — FAQ seed data
-- Controlled seed: categories + articles per role + screens + tags.

-- -------------------------------------------------------------------------
-- 1. Categories
-- -------------------------------------------------------------------------
INSERT INTO tb_faq_category (id, name, description) VALUES
    ('a1000000-0000-0000-0000-000000000001', 'Primeiros Passos',        'Guia inicial de uso da plataforma'),
    ('a1000000-0000-0000-0000-000000000002', 'Documentos',              'Envio, visualização e gestão de documentos'),
    ('a1000000-0000-0000-0000-000000000003', 'Colaboradores',           'Cadastro e gestão de colaboradores'),
    ('a1000000-0000-0000-0000-000000000004', 'Ponto e Registro',        'Registros de ponto e espelho'),
    ('a1000000-0000-0000-0000-000000000005', 'Férias',                  'Solicitação e aprovação de férias'),
    ('a1000000-0000-0000-0000-000000000006', 'Abono',                   'Solicitação e aprovação de abono de ponto'),
    ('a1000000-0000-0000-0000-000000000007', 'Empresa e Administração', 'Gestão de empresas e configurações globais'),
    ('a1000000-0000-0000-0000-000000000008', 'Permissões',              'Controle de acesso e papéis de usuário')
ON CONFLICT (id) DO NOTHING;

-- -------------------------------------------------------------------------
-- 2. Articles — COMMON (PARTNER + MANAGER + CTO)
-- -------------------------------------------------------------------------

-- FAQ C1: Como usar a busca de ajuda?
INSERT INTO tb_faq_article (id, title, short_answer, full_answer, status, priority, category_id) VALUES (
    'b1000000-0000-0000-0000-000000000001',
    'Como usar a busca de ajuda?',
    'Clique no ícone de busca no cabeçalho ou pressione o atalho de teclado para abrir a busca de ajuda.',
    'A busca de ajuda está disponível em todas as telas autenticadas. '
    || 'No desktop, clique no ícone de interrogação ou lupa no cabeçalho. '
    || 'No mobile, toque no botão flutuante de ajuda. '
    || 'Digite palavras-chave relacionadas à sua dúvida. '
    || 'Os resultados são filtrados automaticamente de acordo com o seu perfil de acesso. '
    || 'Você também pode ver sugestões contextuais relacionadas à tela em que está.',
    'ACTIVE', 1,
    'a1000000-0000-0000-0000-000000000001'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_faq_article_role (faq_id, role) VALUES
    ('b1000000-0000-0000-0000-000000000001', 'PARTNER'),
    ('b1000000-0000-0000-0000-000000000001', 'MANAGER'),
    ('b1000000-0000-0000-0000-000000000001', 'CTO')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_screen (faq_id, screen_key) VALUES
    ('b1000000-0000-0000-0000-000000000001', 'DASHBOARD')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_tag (faq_id, tag) VALUES
    ('b1000000-0000-0000-0000-000000000001', 'busca'),
    ('b1000000-0000-0000-0000-000000000001', 'ajuda'),
    ('b1000000-0000-0000-0000-000000000001', 'faq'),
    ('b1000000-0000-0000-0000-000000000001', 'pesquisa')
ON CONFLICT DO NOTHING;

-- FAQ C2: O que fazer quando uma informação não aparece?
INSERT INTO tb_faq_article (id, title, short_answer, full_answer, status, priority, category_id) VALUES (
    'b1000000-0000-0000-0000-000000000002',
    'O que fazer quando uma informação não aparece?',
    'Verifique sua conexão, atualize a página ou entre em contato com o suporte.',
    'Se uma informação não aparecer na plataforma, siga os passos: '
    || '1. Verifique sua conexão com a internet. '
    || '2. Atualize a página (F5 ou botão de atualizar). '
    || '3. Verifique se o seu perfil tem permissão para acessar o recurso. '
    || '4. Caso o problema persista, entre em contato com o administrador da sua empresa ou com o suporte Kronos.',
    'ACTIVE', 2,
    'a1000000-0000-0000-0000-000000000001'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_faq_article_role (faq_id, role) VALUES
    ('b1000000-0000-0000-0000-000000000002', 'PARTNER'),
    ('b1000000-0000-0000-0000-000000000002', 'MANAGER'),
    ('b1000000-0000-0000-0000-000000000002', 'CTO')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_screen (faq_id, screen_key) VALUES
    ('b1000000-0000-0000-0000-000000000002', 'DASHBOARD')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_tag (faq_id, tag) VALUES
    ('b1000000-0000-0000-0000-000000000002', 'erro'),
    ('b1000000-0000-0000-0000-000000000002', 'informacao'),
    ('b1000000-0000-0000-0000-000000000002', 'suporte'),
    ('b1000000-0000-0000-0000-000000000002', 'problema')
ON CONFLICT DO NOTHING;

-- -------------------------------------------------------------------------
-- 3. Articles — PARTNER only
-- -------------------------------------------------------------------------

-- FAQ P1: Como visualizar meus documentos?
INSERT INTO tb_faq_article (id, title, short_answer, full_answer, status, priority, category_id) VALUES (
    'b2000000-0000-0000-0000-000000000001',
    'Como visualizar meus documentos?',
    'Acesse o menu "Documentos" para ver todos os seus documentos disponíveis.',
    'Para visualizar seus documentos: '
    || '1. Acesse o menu lateral e clique em "Documentos". '
    || '2. Utilize os filtros de tipo e data para localizar o documento desejado. '
    || '3. Clique no documento para visualizá-lo ou baixá-lo. '
    || 'Apenas documentos enviados pelo seu gestor estarão disponíveis aqui.',
    'ACTIVE', 1,
    'a1000000-0000-0000-0000-000000000002'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_faq_article_role (faq_id, role) VALUES
    ('b2000000-0000-0000-0000-000000000001', 'PARTNER')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_screen (faq_id, screen_key) VALUES
    ('b2000000-0000-0000-0000-000000000001', 'DOCUMENTS')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_tag (faq_id, tag) VALUES
    ('b2000000-0000-0000-0000-000000000001', 'documentos'),
    ('b2000000-0000-0000-0000-000000000001', 'visualizar'),
    ('b2000000-0000-0000-0000-000000000001', 'holerite'),
    ('b2000000-0000-0000-0000-000000000001', 'contrato')
ON CONFLICT DO NOTHING;

-- FAQ P2: Como solicitar férias?
INSERT INTO tb_faq_article (id, title, short_answer, full_answer, status, priority, category_id) VALUES (
    'b2000000-0000-0000-0000-000000000002',
    'Como solicitar férias?',
    'Acesse "Férias", selecione o período desejado e envie a solicitação para aprovação.',
    'Para solicitar férias: '
    || '1. Acesse o menu "Férias" na navegação principal. '
    || '2. Clique em "Nova solicitação". '
    || '3. Selecione a data de início e a data de fim das férias. '
    || '4. Escolha o gestor responsável pela aprovação. '
    || '5. Confirme o envio. '
    || 'Sua solicitação será analisada pelo gestor e você receberá uma notificação com a decisão.',
    'ACTIVE', 1,
    'a1000000-0000-0000-0000-000000000005'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_faq_article_role (faq_id, role) VALUES
    ('b2000000-0000-0000-0000-000000000002', 'PARTNER')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_screen (faq_id, screen_key) VALUES
    ('b2000000-0000-0000-0000-000000000002', 'VACATION')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_tag (faq_id, tag) VALUES
    ('b2000000-0000-0000-0000-000000000002', 'ferias'),
    ('b2000000-0000-0000-0000-000000000002', 'solicitacao'),
    ('b2000000-0000-0000-0000-000000000002', 'periodo'),
    ('b2000000-0000-0000-0000-000000000002', 'descanso')
ON CONFLICT DO NOTHING;

-- FAQ P3: Como solicitar abono de ponto?
INSERT INTO tb_faq_article (id, title, short_answer, full_answer, status, priority, category_id) VALUES (
    'b2000000-0000-0000-0000-000000000003',
    'Como solicitar abono de ponto?',
    'Acesse "Abono/Solicitações", informe o motivo e anexe o comprovante quando necessário.',
    'Para solicitar abono de ponto: '
    || '1. Acesse o menu "Solicitações" ou "Abono". '
    || '2. Selecione a data e o registro de ponto a ser abonado. '
    || '3. Informe o motivo da solicitação. '
    || '4. Anexe um comprovante, se disponível (ex.: atestado médico). '
    || '5. Envie a solicitação. '
    || 'O gestor responsável irá analisar e aprovar ou rejeitar a solicitação.',
    'ACTIVE', 2,
    'a1000000-0000-0000-0000-000000000006'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_faq_article_role (faq_id, role) VALUES
    ('b2000000-0000-0000-0000-000000000003', 'PARTNER')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_screen (faq_id, screen_key) VALUES
    ('b2000000-0000-0000-0000-000000000003', 'TIME_OFF')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_tag (faq_id, tag) VALUES
    ('b2000000-0000-0000-0000-000000000003', 'abono'),
    ('b2000000-0000-0000-0000-000000000003', 'ponto'),
    ('b2000000-0000-0000-0000-000000000003', 'justificativa'),
    ('b2000000-0000-0000-0000-000000000003', 'comprovante')
ON CONFLICT DO NOTHING;

-- FAQ P4: Como consultar meu espelho de ponto?
INSERT INTO tb_faq_article (id, title, short_answer, full_answer, status, priority, category_id) VALUES (
    'b2000000-0000-0000-0000-000000000004',
    'Como consultar meu espelho de ponto?',
    'Acesse "Registros" ou "Espelho de Ponto" para ver seu histórico de registros.',
    'Para consultar o espelho de ponto: '
    || '1. Acesse o menu "Registros" ou "Espelho de Ponto". '
    || '2. Selecione o período desejado (mês e ano). '
    || '3. Visualize os registros de entrada, saída e pausas. '
    || '4. Se precisar de uma versão em PDF, clique em "Exportar" ou "Baixar espelho". '
    || 'Em caso de divergência, entre em contato com seu gestor.',
    'ACTIVE', 1,
    'a1000000-0000-0000-0000-000000000004'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_faq_article_role (faq_id, role) VALUES
    ('b2000000-0000-0000-0000-000000000004', 'PARTNER')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_screen (faq_id, screen_key) VALUES
    ('b2000000-0000-0000-0000-000000000004', 'TIME_RECORDS')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_tag (faq_id, tag) VALUES
    ('b2000000-0000-0000-0000-000000000004', 'espelho'),
    ('b2000000-0000-0000-0000-000000000004', 'ponto'),
    ('b2000000-0000-0000-0000-000000000004', 'registro'),
    ('b2000000-0000-0000-0000-000000000004', 'historico')
ON CONFLICT DO NOTHING;

-- -------------------------------------------------------------------------
-- 4. Articles — MANAGER only
-- -------------------------------------------------------------------------

-- FAQ M1: Como cadastrar colaborador?
INSERT INTO tb_faq_article (id, title, short_answer, full_answer, status, priority, category_id) VALUES (
    'b3000000-0000-0000-0000-000000000001',
    'Como cadastrar colaborador?',
    'Acesse "Colaboradores", clique em "Novo Colaborador" e preencha os dados necessários.',
    'Para cadastrar um novo colaborador: '
    || '1. Acesse o menu "Colaboradores". '
    || '2. Clique no botão "Novo Colaborador". '
    || '3. Preencha os dados pessoais: nome completo, CPF, cargo, salário, e-mail e telefone. '
    || '4. Informe o endereço e configure os horários de jornada. '
    || '5. Se necessário, cadastre a biometria facial. '
    || '6. Confirme o cadastro. '
    || 'Após o cadastro, um usuário de acesso será criado automaticamente ou pode ser criado manualmente em "Usuários".',
    'ACTIVE', 1,
    'a1000000-0000-0000-0000-000000000003'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_faq_article_role (faq_id, role) VALUES
    ('b3000000-0000-0000-0000-000000000001', 'MANAGER')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_screen (faq_id, screen_key) VALUES
    ('b3000000-0000-0000-0000-000000000001', 'EMPLOYEES')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_tag (faq_id, tag) VALUES
    ('b3000000-0000-0000-0000-000000000001', 'colaborador'),
    ('b3000000-0000-0000-0000-000000000001', 'cadastro'),
    ('b3000000-0000-0000-0000-000000000001', 'funcionario'),
    ('b3000000-0000-0000-0000-000000000001', 'admissao')
ON CONFLICT DO NOTHING;

-- FAQ M2: Como enviar documento para colaborador?
INSERT INTO tb_faq_article (id, title, short_answer, full_answer, status, priority, category_id) VALUES (
    'b3000000-0000-0000-0000-000000000002',
    'Como enviar documento para colaborador?',
    'Acesse "Documentos", selecione o colaborador e faça o upload do arquivo.',
    'Para enviar um documento para um colaborador: '
    || '1. Acesse o menu "Documentos". '
    || '2. Selecione o tipo de documento (ex.: holerite, contrato). '
    || '3. Filtre pelo nome ou ID do colaborador destinatário. '
    || '4. Clique em "Enviar documento" e selecione o arquivo (PDF, JPG ou PNG, máx. 5MB). '
    || '5. Confirme o envio. '
    || 'O colaborador poderá visualizar e baixar o documento na área de documentos dele.',
    'ACTIVE', 2,
    'a1000000-0000-0000-0000-000000000002'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_faq_article_role (faq_id, role) VALUES
    ('b3000000-0000-0000-0000-000000000002', 'MANAGER')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_screen (faq_id, screen_key) VALUES
    ('b3000000-0000-0000-0000-000000000002', 'DOCUMENTS')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_tag (faq_id, tag) VALUES
    ('b3000000-0000-0000-0000-000000000002', 'documento'),
    ('b3000000-0000-0000-0000-000000000002', 'enviar'),
    ('b3000000-0000-0000-0000-000000000002', 'upload'),
    ('b3000000-0000-0000-0000-000000000002', 'holerite')
ON CONFLICT DO NOTHING;

-- FAQ M3: Como aprovar férias?
INSERT INTO tb_faq_article (id, title, short_answer, full_answer, status, priority, category_id) VALUES (
    'b3000000-0000-0000-0000-000000000003',
    'Como aprovar férias?',
    'Acesse "Férias", veja as solicitações pendentes e aprove ou rejeite.',
    'Para aprovar uma solicitação de férias: '
    || '1. Acesse o menu "Férias". '
    || '2. Visualize as solicitações com status "Pendente". '
    || '3. Clique na solicitação para ver os detalhes (período, colaborador). '
    || '4. Clique em "Aprovar" para confirmar ou "Rejeitar" para negar. '
    || '5. O colaborador será notificado sobre a decisão. '
    || 'Somente gestores e CTO podem aprovar ou rejeitar solicitações de férias.',
    'ACTIVE', 1,
    'a1000000-0000-0000-0000-000000000005'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_faq_article_role (faq_id, role) VALUES
    ('b3000000-0000-0000-0000-000000000003', 'MANAGER')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_screen (faq_id, screen_key) VALUES
    ('b3000000-0000-0000-0000-000000000003', 'VACATION')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_tag (faq_id, tag) VALUES
    ('b3000000-0000-0000-0000-000000000003', 'ferias'),
    ('b3000000-0000-0000-0000-000000000003', 'aprovacao'),
    ('b3000000-0000-0000-0000-000000000003', 'pendente'),
    ('b3000000-0000-0000-0000-000000000003', 'gestor')
ON CONFLICT DO NOTHING;

-- FAQ M4: Como aprovar abono?
INSERT INTO tb_faq_article (id, title, short_answer, full_answer, status, priority, category_id) VALUES (
    'b3000000-0000-0000-0000-000000000004',
    'Como aprovar abono?',
    'Acesse "Solicitações de Abono", veja os pedidos pendentes e aprove ou rejeite.',
    'Para aprovar uma solicitação de abono: '
    || '1. Acesse o menu "Solicitações" ou "Abono". '
    || '2. Filtre pelo status "Pendente". '
    || '3. Clique na solicitação para ver o motivo e o comprovante (se houver). '
    || '4. Clique em "Aprovar" para confirmar ou "Rejeitar" para negar. '
    || 'O colaborador será notificado da decisão automaticamente.',
    'ACTIVE', 2,
    'a1000000-0000-0000-0000-000000000006'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_faq_article_role (faq_id, role) VALUES
    ('b3000000-0000-0000-0000-000000000004', 'MANAGER')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_screen (faq_id, screen_key) VALUES
    ('b3000000-0000-0000-0000-000000000004', 'TIME_OFF')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_tag (faq_id, tag) VALUES
    ('b3000000-0000-0000-0000-000000000004', 'abono'),
    ('b3000000-0000-0000-0000-000000000004', 'aprovacao'),
    ('b3000000-0000-0000-0000-000000000004', 'solicitacao'),
    ('b3000000-0000-0000-0000-000000000004', 'ponto')
ON CONFLICT DO NOTHING;

-- FAQ M5: Como consultar auditoria fiscal?
INSERT INTO tb_faq_article (id, title, short_answer, full_answer, status, priority, category_id) VALUES (
    'b3000000-0000-0000-0000-000000000005',
    'Como consultar auditoria fiscal?',
    'Acesse "Auditoria" para exportar relatórios fiscais como AFD, AEJ e espelho de ponto.',
    'Para consultar a auditoria fiscal: '
    || '1. Acesse o menu "Auditoria". '
    || '2. Selecione o tipo de relatório: AFD (Arquivo Fonte de Dados), AEJ (Arquivo Eletrônico de Jornada) ou Espelho de Ponto. '
    || '3. Informe o período desejado. '
    || '4. Clique em "Exportar" para baixar o arquivo. '
    || 'Os arquivos gerados são assinados digitalmente e atendem às exigências da legislação trabalhista.',
    'ACTIVE', 3,
    'a1000000-0000-0000-0000-000000000007'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_faq_article_role (faq_id, role) VALUES
    ('b3000000-0000-0000-0000-000000000005', 'MANAGER')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_screen (faq_id, screen_key) VALUES
    ('b3000000-0000-0000-0000-000000000005', 'AUDIT')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_tag (faq_id, tag) VALUES
    ('b3000000-0000-0000-0000-000000000005', 'auditoria'),
    ('b3000000-0000-0000-0000-000000000005', 'fiscal'),
    ('b3000000-0000-0000-0000-000000000005', 'afd'),
    ('b3000000-0000-0000-0000-000000000005', 'espelho')
ON CONFLICT DO NOTHING;

-- -------------------------------------------------------------------------
-- 5. Articles — CTO only
-- -------------------------------------------------------------------------

-- FAQ A1: Como criar empresa?
INSERT INTO tb_faq_article (id, title, short_answer, full_answer, status, priority, category_id) VALUES (
    'b4000000-0000-0000-0000-000000000001',
    'Como criar empresa?',
    'Acesse "Empresas", clique em "Nova Empresa" e preencha os dados do CNPJ e endereço.',
    'Para criar uma nova empresa na plataforma: '
    || '1. Acesse o menu "Empresas" (disponível apenas para CTO). '
    || '2. Clique em "Nova Empresa". '
    || '3. Informe o CNPJ e os dados da empresa (nome, endereço, geolocalização). '
    || '4. Configure os dados de funcionamento (horário, tipo de jornada). '
    || '5. Confirme o cadastro. '
    || 'Após criar a empresa, você poderá criar o primeiro gestor vinculado a ela.',
    'ACTIVE', 1,
    'a1000000-0000-0000-0000-000000000007'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_faq_article_role (faq_id, role) VALUES
    ('b4000000-0000-0000-0000-000000000001', 'CTO')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_screen (faq_id, screen_key) VALUES
    ('b4000000-0000-0000-0000-000000000001', 'COMPANIES')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_tag (faq_id, tag) VALUES
    ('b4000000-0000-0000-0000-000000000001', 'empresa'),
    ('b4000000-0000-0000-0000-000000000001', 'criar'),
    ('b4000000-0000-0000-0000-000000000001', 'cnpj'),
    ('b4000000-0000-0000-0000-000000000001', 'onboarding')
ON CONFLICT DO NOTHING;

-- FAQ A2: Como criar Manager?
INSERT INTO tb_faq_article (id, title, short_answer, full_answer, status, priority, category_id) VALUES (
    'b4000000-0000-0000-0000-000000000002',
    'Como criar Manager?',
    'Acesse "Usuários", crie o colaborador e associe o papel de MANAGER.',
    'Para criar um usuário com papel de Manager: '
    || '1. Acesse o menu "Usuários". '
    || '2. Clique em "Criar Administrador" ou "Novo Manager". '
    || '3. Preencha os dados do colaborador (nome, CPF, e-mail, cargo). '
    || '4. Selecione a empresa à qual o Manager será vinculado. '
    || '5. Confirme o cadastro. '
    || 'Um username e credenciais de acesso serão criados automaticamente. '
    || 'O Manager poderá acessar a plataforma e gerenciar os colaboradores da empresa.',
    'ACTIVE', 2,
    'a1000000-0000-0000-0000-000000000008'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_faq_article_role (faq_id, role) VALUES
    ('b4000000-0000-0000-0000-000000000002', 'CTO')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_screen (faq_id, screen_key) VALUES
    ('b4000000-0000-0000-0000-000000000002', 'USERS')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_tag (faq_id, tag) VALUES
    ('b4000000-0000-0000-0000-000000000002', 'manager'),
    ('b4000000-0000-0000-0000-000000000002', 'criar'),
    ('b4000000-0000-0000-0000-000000000002', 'usuario'),
    ('b4000000-0000-0000-0000-000000000002', 'permissao')
ON CONFLICT DO NOTHING;

-- FAQ A3: Como consultar permissões administrativas?
INSERT INTO tb_faq_article (id, title, short_answer, full_answer, status, priority, category_id) VALUES (
    'b4000000-0000-0000-0000-000000000003',
    'Como consultar permissões administrativas?',
    'Acesse "Usuários" para visualizar os papéis e permissões de cada usuário cadastrado.',
    'Para consultar permissões administrativas: '
    || '1. Acesse o menu "Usuários". '
    || '2. Use a busca para localizar um usuário específico. '
    || '3. Visualize o papel (role) e o status ativo/inativo de cada usuário. '
    || '4. Para alterar permissões, utilize as ações de edição disponíveis. '
    || 'Papéis disponíveis: PARTNER (colaborador), MANAGER (gestor), CTO (administrador global).',
    'ACTIVE', 3,
    'a1000000-0000-0000-0000-000000000008'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_faq_article_role (faq_id, role) VALUES
    ('b4000000-0000-0000-0000-000000000003', 'CTO')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_screen (faq_id, screen_key) VALUES
    ('b4000000-0000-0000-0000-000000000003', 'USERS')
ON CONFLICT DO NOTHING;

INSERT INTO tb_faq_article_tag (faq_id, tag) VALUES
    ('b4000000-0000-0000-0000-000000000003', 'permissoes'),
    ('b4000000-0000-0000-0000-000000000003', 'administrativo'),
    ('b4000000-0000-0000-0000-000000000003', 'roles'),
    ('b4000000-0000-0000-0000-000000000003', 'acesso')
ON CONFLICT DO NOTHING;
