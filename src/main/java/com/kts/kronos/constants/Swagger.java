package com.kts.kronos.constants;

public class Swagger {
    private Swagger() {
    }

    public static final String LIST_SUCCESS = "Lista retornada com sucesso";
    public static final String DTO_SCHEMA_DESCRIPTION = "Estrutura de dados exposta pela API. Evita a exposição de informações sensíveis e detalhes internos de implementação.";
    public static final String OPENAPI_TITLE = "Kronos API";
    public static final String OPENAPI_DESCRIPTION = "API para gestão de autenticação, empresas, colaboradores, jornada e documentos, com foco em rastreabilidade e segurança da informação.";
    public static final String OPENAPI_VERSION = "v1";
    public static final String OPENAPI_SECURITY_SCHEME = "cookieAuth";
    public static final String OPENAPI_SECURITY_SCHEME_BEARER = "bearerAuth";
    public static final String OPENAPI_SECURITY_DESCRIPTION = "Fluxo principal web: autenticação via cookie HttpOnly 'APIKEY' (enviado automaticamente pelo navegador). Não dependa de Authorization: Bearer no frontend web.";
    public static final String OPENAPI_SECURITY_BEARER_DESCRIPTION = "Fluxo opcional para clientes não-browser (integrações server-to-server): informe o JWT no header Authorization no formato Bearer {token}.";

    // --- SWAGGER: Auth Controller ---
    public static final String SWAGGER_AUTH_TAG = "Autenticação";
    public static final String SWAGGER_AUTH_DESC = "Endpoints para login, recuperação de senha e login facial";

    public static final String LOGIN_SUMMARY = "Login com Credenciais";
    public static final String LOGIN_DESC = "Autentica um usuário via username e senha e estabelece a sessão via cookie HttpOnly enviado no header Set-Cookie. O campo token no corpo é legado e permanece nulo.";
    public static final String LOGIN_SUCCESS = "Login realizado com sucesso";
    public static final String CREDENTIALS_INVALID = "Credenciais inválidas";

    public static final String RECOVER_PASS_SUMMARY = "Solicitar Recuperação de Senha";
    public static final String RECOVER_PASS_DESC = "Envia um e-mail com instruções para redefinição de senha.";
    public static final String RECOVER_PASS_204 = "Solicitação processada com sucesso";
    public static final String RECOVER_PASS_400 = "Dados inválidos para recuperação de senha";
    public static final String RECOVER_PASS_404 = "Usuário não encontrado para os dados informados";

    public static final String LOGIN_FACE_SUMMARY = "Login Facial";
    public static final String LOGIN_FACE_DESC = "Autentica o usuário através da imagem facial (Base64) e estabelece a sessão via cookie HttpOnly enviado no header Set-Cookie. O campo token no corpo é legado e permanece nulo.";
    public static final String LOGIN_FACE_400 = "Usuário inativo | Imagem inválida | Erro na autenticação facial";
    public static final String LOGIN_FACE_403 = "Facial inválida";
    public static final String LOGIN_FACE_404 = "Nenhum usuário vinculado ao colaborador";

    public static final String LOGOUT_SUMMARY = "Logout";
    public static final String LOGOUT_DESC = "Encerra a sessão web removendo o cookie HttpOnly de autenticação no header Set-Cookie.";
    public static final String LOGOUT_204 = "Logout processado com sucesso (cookie de sessão removido)";

    public static final String RESET_PASS_SUMMARY = "Redefinir Senha";
    public static final String RESET_PASS_DESC = "Conclui o processo de troca de senha utilizando o token recebido por e-mail.";
    public static final String RESET_PASS_204 = "Senha redefinida com sucesso";
    public static final String RESET_PASS_400 = "Confirmação de senha não confere";
    public static final String RESET_PASS_404 = "Token de recuperação inválido ou expirado | Usuário não encontrado | Erro na autenticação facial";

    // --- SWAGGER: COMPANIES ---

    public static final String SWAGGER_COMPANY_TAG = "Empresas";
    public static final String SWAGGER_COMPANY_DESC = "Gestão de empresas (Requer permissão KRONOS/Admin)";

    public static final String REG_COMPANY_SUMMARY = "Registrar Nova Empresa";
    public static final String REG_COMPANY_DESC = "Cria uma nova empresa no sistema.";
    public static final String REG_COMPANY_SUCCESS = "Empresa registrada com sucesso";
    public static final String REG_COMPANY_400 = "CNPJ já cadastrado no sistema ou dados inválidos";
    public static final String REG_COMPANY_403 = "Acesso negado (Requer perfil KRONOS)";

    public static final String GET_COMPANY_SUMMARY = "Buscar Empresa por CNPJ";
    public static final String GET_COMPANY_DESC = "Retorna os detalhes da empresa e contagem de funcionários.";
    public static final String GET_COMPANY_SUCCESS = "Empresa encontrada";
    public static final String GET_COMPANY_404 = "Empresa não encontrada com o CNPJ informado";

    public static final String LIST_COMPANY_SUMMARY = "Listar Todas as Empresas";
    public static final String LIST_COMPANY_DESC = "Lista empresas cadastradas com filtro opcional de status.";
    public static final String ACCESS_DENIED = "Acesso negado"; // Genérico para 403

    public static final String UPDATE_COMPANY_SUMMARY = "Atualizar Empresa";
    public static final String UPDATE_COMPANY_DESC = "Atualiza dados cadastrais e endereço da empresa.";
    public static final String UPDATE_COMPANY_SUCCESS = "Dados atualizados com sucesso";
    public static final String UPDATE_COMPANY_400 = "Erro de validação: Geolocalização é obrigatória ao alterar endereço";
    public static final String UPDATE_COMPANY_404 = "Empresa não encontrada"; // Genérico para 404

    public static final String TOGGLE_COMPANY_SUMMARY = "Ativar/Desativar Empresa";
    public static final String TOGGLE_COMPANY_DESC = "Alterna o status da empresa e reflete a alteração nos usuários vinculados.";
    public static final String TOGGLE_COMPANY_SUCCESS = "Status alterado com sucesso";

    public static final String DEL_COMPANY_SUMMARY = "Excluir Empresa";
    public static final String DEL_COMPANY_DESC = "Remove a empresa do banco de dados.";
    public static final String DEL_COMPANY_SUCCESS = "Empresa excluída com sucesso";

    public static final String CHECK_CNPJ_SUMMARY = "Verificar Disponibilidade de CNPJ";
    public static final String CHECK_CNPJ_DESC = "Verifica se um CNPJ já está cadastrado.";
    public static final String CHECK_CNPJ_200 = "CNPJ Indisponível (Já existe no banco)";
    public static final String CHECK_CNPJ_404 = "CNPJ Disponível (Não encontrado no banco)";

    // --- SWAGGER: DOCUMENTS ---
    public static final String SWAGGER_DOC_TAG = "Documentos";
    public static final String SWAGGER_DOC_DESC = "Gestão de arquivos e documentos dos colaboradores (Upload, Download e Listagem)";

    public static final String UPLOAD_DOC_SUMMARY = "Upload de Documento";
    public static final String UPLOAD_DOC_DESC = "Envia um arquivo para o armazenamento e vincula ao funcionário.";
    public static final String UPLOAD_DOC_SUCCESS = "Upload realizado com sucesso";
    public static final String UPLOAD_DOC_400 = "Arquivo inválido, tipo não permitido ou erro de leitura";
    public static final String UPLOAD_DOC_404 = "Funcionário não encontrado"; // Pode reutilizar EMPLOYEE_NOT_FOUND se preferir, mas aqui é Swagger desc

    public static final String LIST_DOC_SUMMARY = "Listar Documentos";
    public static final String LIST_DOC_DESC = "Lista metadados dos documentos com filtros opcionais.";

    public static final String DOWN_DOC_SUMMARY = "Download de Documento";
    public static final String DOWN_DOC_DESC = "Recupera o binário do arquivo armazenado.";
    public static final String DOWN_DOC_SUCCESS = "Arquivo recuperado com sucesso";
    public static final String DOWN_DOC_400 = "Erro ao ler arquivo do storage";
    public static final String DOWN_DOC_404 = "Documento ou funcionário não encontrado";

    public static final String DEL_DOC_SUMMARY = "Excluir Documento";
    public static final String DEL_DOC_DESC = "Realiza exclusão lógica ou física do documento, dependendo das regras de negócio.";
    public static final String DEL_DOC_SUCCESS = "Documento excluído ou marcado para exclusão";
    public static final String DEL_DOC_403 = "Operação proibida (Ex: Tentar apagar documento de outro usuário sem ser Gestor)";
    public static final String DEL_DOC_404 = "Documento não encontrado";

    // --- SWAGGER: EMPLOYEES ---
    public static final String SWAGGER_EMPLOYEE_TAG = "Colaboradores";
    public static final String SWAGGER_EMPLOYEE_DESC = "Gestão de cadastro, perfil e ações de funcionários";

    public static final String REG_EMPLOYEE_SUMMARY = "Registrar Funcionário";
    public static final String REG_EMPLOYEE_DESC = "Cria um novo funcionário. Se enviado uma foto facial, realiza o cadastro biométrico.";
    public static final String REG_EMPLOYEE_201 = "Funcionário criado com sucesso";
    public static final String REG_EMPLOYEE_400 = "Erro de validação: CPF duplicado, Face não detectada ou imagem inválida";
    public static final String REG_EMPLOYEE_403 = "Acesso negado (Permissão insuficiente)";

    public static final String LIST_EMPLOYEE_SUMMARY = "Listar Funcionários";
    public static final String LIST_EMPLOYEE_DESC = "Retorna lista de funcionários vinculados à empresa do gestor logado.";

    public static final String GET_EMPLOYEE_SUMMARY = "Buscar Funcionário por ID";
    public static final String GET_EMPLOYEE_DESC = "Obtém detalhes de um funcionário específico.";
    public static final String GET_EMPLOYEE_SUCCESS = "Funcionário encontrado";
    public static final String GET_EMPLOYEE_404 = "Funcionário não encontrado (ou pertence a outra empresa)";


    public static final String UPDATE_EMPLOYEE_SUMMARY = "Atualizar Funcionário (Gestor)";
    public static final String UPDATE_EMPLOYEE_DESC = "Atualiza dados contratuais e biometria facial do funcionário.";
    public static final String UPDATE_EMPLOYEE_400 = "Erro na atualização da biometria facial (Imagem inválida/sem face)";
    public static final String SWAGGER_EMP_NOT_FOUND = "Funcionário não encontrado"; // Específico para Swagger

    public static final String OWN_PROFILE_SUMMARY = "Meu Perfil";
    public static final String OWN_PROFILE_DESC = "Retorna os dados do funcionário atualmente logado.";
    public static final String OWN_PROFILE_SUCCESS = "Perfil recuperado com sucesso";
    public static final String OWN_PROFILE_404 = "Usuário/Funcionário não encontrado";

    public static final String UPDATE_OWN_PROFILE_SUMMARY = "Atualizar Meu Perfil";
    public static final String UPDATE_OWN_PROFILE_DESC = "Funcionário atualiza seus próprios dados de contato (Email, Telefone, Endereço).";
    public static final String UPDATE_OWN_PROFILE_SUCCESS = "Perfil atualizado com sucesso";
    public static final String UPDATE_EMPLOYEE_SUCCESS = "Dados atualizados com sucesso";


    public static final String DEL_EMPLOYEE_SUMMARY = "Remover Funcionário";
    public static final String DEL_EMPLOYEE_DESC = "Remove o funcionário da base de dados.";
    public static final String DEL_EMPLOYEE_SUCCESS = "Funcionário removido com sucesso";

    public static final String MARK_MSG_SEEN_SUMMARY = "Marcar Mensagens como Lidas";
    public static final String MARK_MSG_SEEN_DESC = "Atualiza o timestamp de visualização de mensagens do mural.";
    public static final String MARK_MSG_SEEN_SUCCESS = "Marcado como lido";

    public static final String CHECK_CPF_SUMMARY = "Verificar Disponibilidade de CPF";
    public static final String CHECK_CPF_DESC = "Verifica se um CPF já está cadastrado no sistema.";
    public static final String CHECK_CPF_200 = "CPF Indisponível (Já existe)";
    public static final String CHECK_CPF_404 = "CPF Disponível (Não encontrado)";



    // --- SWAGGER: LEGAL (FISCAL) ---
    public static final String SWAGGER_LEGAL_TAG = "Fiscal - Arquivos Legais";
    public static final String SWAGGER_LEGAL_DESC = "Geração de arquivos para fiscalização e espelhos de ponto (Portaria 671)";

    public static final String TECH_CERT_SUMMARY = "Baixar Atestado Técnico (Portaria 671)";
    public static final String TECH_CERT_DESC = "Gera o atestado de conformidade técnica assinado digitalmente (.p7s).";
    public static final String FILE_GENERATED_SUCCESS = "Arquivo gerado com sucesso";
    public static final String COMPANY_OR_EMPLOYEE_NOT_FOUND = "Empresa ou Colaborador não encontrado";
    public static final String INTERNAL_SIGNATURE_ERROR = "Erro interno na assinatura digital ou geração do PDF";
    public static final String ACCESS_DENIED_MANAGER_CTO = "Acesso negado (Requer MANAGER ou CTO)";

    public static final String AFD_SUMMARY = "Baixar Arquivo Fonte de Dados (AFD)";
    public static final String AFD_DESC = "Gera o arquivo TXT contendo todos os registros brutos de ponto (Portaria 671).";
    public static final String AFD_SUCCESS = "Arquivo AFD gerado com sucesso";
    public static final String STREAM_ERROR = "Erro ao processar stream de dados";

    public static final String AEJ_SUMMARY = "Baixar Arquivo Eletrônico de Jornada (AEJ)";
    public static final String AEJ_DESC = "Gera arquivo assinado digitalmente (.p7s) contendo apuração de ponto processada.";
    public static final String AEJ_SUCCESS = "Arquivo AEJ gerado com sucesso";
    public static final String CRITICAL_SIGNATURE_ERROR = "Erro crítico na assinatura digital";

    public static final String MIRROR_SUMMARY = "Baixar Espelho de Ponto (PDF)";
    public static final String MIRROR_DESC = "Relatório mensal detalhado para conferência do funcionário.";
    public static final String PDF_SUCCESS = "PDF gerado com sucesso";
    public static final String PDF_GENERATION_ERROR = "Erro na geração do PDF";


    // --- SWAGGER: MESSAGES (MURAL) ---
    public static final String SWAGGER_MSG_TAG = "Mensagens e Avisos";
    public static final String SWAGGER_MSG_DESC = "Mural de comunicação interna (Envio e Leitura de mensagens)";

    public static final String POST_MSG_SUMMARY = "Publicar Mensagem";
    public static final String POST_MSG_DESC = "Envia uma nova mensagem/aviso para colaboradores específicos da empresa.";
    public static final String POST_MSG_SUCCESS = "Mensagem enviada com sucesso";
    public static final String POST_MSG_400 = "Erro de validação: Lista de destinatários vazia ou inválida";
    public static final String POST_MSG_404 = "Remetente não encontrado";
    public static final String POST_MSG_403 = "Acesso negado (Requer perfil MANAGER)";

    public static final String LIST_MSG_SUMMARY = "Listar Minhas Mensagens";
    public static final String LIST_MSG_DESC = "Retorna as mensagens direcionadas ao colaborador logado ou globais da empresa.";
    public static final String LIST_MSG_SUCCESS = "Lista recuperada com sucesso";

    public static final String DEL_MSG_SUMMARY = "Excluir Mensagem";
    public static final String DEL_MSG_DESC = "Remove uma mensagem do mural. Apenas o autor da mensagem pode excluí-la.";
    public static final String DEL_MSG_SUCCESS = "Mensagem excluída com sucesso";
    public static final String DEL_MSG_400 = "Operação não permitida: Tentativa de excluir mensagem de outro autor";

    // --- SWAGGER: TERMS (LGPD/BIOMETRIA) ---
    public static final String SWAGGER_TERMS_TAG = "Legal - Termos de Uso";
    public static final String SWAGGER_TERMS_DESC = "Gestão de aceites e termos legais (LGPD/Biometria)";

    public static final String ACCEPT_BIO_SUMMARY = "Registrar Aceite do Termo de Biometria";
    public static final String ACCEPT_BIO_DESC = "Captura IP e User-Agent da requisição, gera um PDF de consentimento assinado eletronicamente e o armazena nos documentos do colaborador.";
    public static final String ACCEPT_BIO_SUCCESS = "Aceite registrado com sucesso (ou termo já aceito anteriormente)";
    public static final String ACCEPT_BIO_404 = "Colaborador ou Empresa não encontrados";
    public static final String ACCEPT_BIO_500 = "Erro interno ao gerar PDF ou realizar upload para o Storage";

    public static final String CHECK_TERMS_SUMMARY = "Verificar Status do Aceite";
    public static final String CHECK_TERMS_DESC = "Verifica se o colaborador logado já possui o documento de termo de biometria registrado no sistema.";
    public static final String CHECK_TERMS_SUCCESS = "Status retornado com sucesso (true/false)";
    public static final String ACCESS_DENIED_TOKEN = "Acesso negado (Token inválido)";

    // --- SWAGGER: TIME RECORD (PONTO/FÉRIAS/ABONOS) ---
    public static final String SWAGGER_TR_TAG = "Ponto Eletrônico & Férias";
    public static final String SWAGGER_TR_DESC = "Registro de jornada (Check-in/out), gestão de espelho, férias e abonos (Portaria 671)";

    // Register Time
    public static final String REG_TIME_SUMMARY = "Registrar Ponto (Check-in/Check-out)";
    public static final String REG_TIME_DESC = "Realiza a marcação de ponto. Executa validação de relógio (NTP), reconhecimento facial (Rekognition) e geolocalização. Gera comprovante PDF e registro fiscal (AFD) automaticamente.";
    public static final String REG_TIME_200 = "Marcação realizada com sucesso (ActionResponse contém NSR e Tipo)";
    public static final String REG_TIME_400 = "Erro de validação: Face não reconhecida, Geolocalização fora da cerca, Checkout em data diferente ou Imagem inválida";
    public static final String REG_TIME_500 = "Erro Crítico: Relógio do servidor dessincronizado (Anti-fraude NTP) ou falha na geração do comprovante";

    // Update Time
    public static final String UPDATE_TR_SUMMARY = "Solicitar Ajuste de Ponto";
    public static final String UPDATE_TR_DESC = "Colaborador ou Gestor solicita a correção de um registro existente. Se for PARTNER, cria uma solicitação de aprovação. Se for MANAGER, aplica a alteração (com validação de sobreposição).";
    public static final String UPDATE_TR_200 = "Solicitação enviada ou ajuste aplicado com sucesso";
    public static final String UPDATE_TR_400 = "Erro: Horário inválido (Início > Fim), Sobreposição com outro registro ou Manager inválido";

    // Update Status
    public static final String UPDATE_STATUS_SUMMARY = "Atualizar Status do Ponto (Gestor)";
    public static final String UPDATE_STATUS_DESC = "Permite ao gestor alterar manualmente o status de um registro (ex: de PENDING para CREATED).";
    public static final String UPDATE_STATUS_200 = "Status atualizado";
    public static final String UPDATE_STATUS_400 = "Operação inválida: Registro aguardando aprovação ou já atualizado";

    // Actions
    public static final String TOGGLE_TR_SUMMARY = "Ativar/Desativar Registro";
    public static final String TOGGLE_TR_DESC = "Realiza exclusão lógica (Soft Delete) ou reativação de um registro.";

    public static final String DEL_TR_SUMMARY = "Excluir Registro Definitivamente";
    public static final String DEL_TR_DESC = "Remove o registro do banco de dados (Apenas Gestores).";

    // Reports
    public static final String REPORT_SUMMARY = "Gerar Relatório Detalhado (Espelho)";
    public static final String REPORT_DESC = "Retorna lista de registros detalhados, incluindo links para documentos comprobatórios e saldos diários.";

    public static final String SIMPLE_REPORT_SUMMARY = "Gerar Relatório Simplificado (Dashboard)";
    public static final String SIMPLE_REPORT_DESC = "Retorna dados sumarizados por dia (Trabalhado, Pausas, Saldo) para exibição em gráficos ou dashboards.";

    // Approvals (Time Record)
    public static final String APPROVE_CHANGE_SUMMARY = "Aprovar Ajuste de Ponto";
    public static final String APPROVE_CHANGE_DESC = "Gestor acata a correção solicitada e aplica as mudanças no registro original.";
    public static final String APPROVE_CHANGE_200 = "Ajuste aprovado e aplicado";
    public static final String APPROVE_CHANGE_400 = "Registro não está aguardando aprovação";
    public static final String APPROVE_CHANGE_404 = "Solicitação não encontrada";

    public static final String REJECT_CHANGE_SUMMARY = "Rejeitar Ajuste de Ponto";
    public static final String REJECT_CHANGE_DESC = "Gestor nega a correção e o registro volta ao estado anterior (marcado como rejeitado).";

    public static final String LIST_PENDING_SUMMARY = "Listar Aprovações Pendentes";
    public static final String LIST_PENDING_DESC = "Lista solicitações de ajuste de ponto pendentes para a empresa do gestor.";

    // Vacation
    public static final String REQ_VACATION_SUMMARY = "Solicitar Férias";
    public static final String REQ_VACATION_DESC = "Cria registros de solicitação de férias para um período. Requer aprovação posterior.";
    public static final String REQ_VACATION_201 = "Solicitação criada com sucesso";
    public static final String REQ_VACATION_400 = "Datas inválidas ou conflito com registros existentes";
    public static final String REQ_VACATION_403 = "Manager indicado não pertence à mesma empresa";

    public static final String APPROVE_VACATION_SUMMARY = "Aprovar Férias";
    public static final String APPROVE_VACATION_DESC = "Converte os registros de 'Solicitação' para 'Férias' (Apenas Manager/CTO).";
    public static final String APPROVE_VACATION_204 = "Férias aprovadas";
    public static final String APPROVE_VACATION_403 = "Permissão insuficiente (Apenas Manager/CTO)";

    public static final String REJECT_VACATION_SUMMARY = "Rejeitar Férias";

    public static final String LIST_VACATION_SUMMARY = "Listar Solicitações de Férias";
    public static final String LIST_VACATION_DESC = "Lista solicitações filtradas por status e nome do colaborador.";

    // Time Off (Abono/Atestado)
    public static final String REQ_TIMEOFF_SUMMARY = "Enviar Atestado/Abono";
    public static final String REQ_TIMEOFF_DESC = "Cria solicitação de abono (ou correção de esquecimento) com upload obrigatório de documento (ex: Atestado Médico).";
    public static final String REQ_TIMEOFF_201 = "Solicitação criada";
    public static final String REQ_TIMEOFF_400 = "Erro no upload do arquivo ou dados inválidos";
    public static final String TIME_OFF_APPROVED = "Abonos/Esquecimentos aprovados com sucesso.";
    public static final String TIME_OFF_REJECTED = "Abonos/Esquecimentos rejeitados com sucesso.";
    public static final String APPROVE_TIMEOFF_SUMMARY = "Aprovar Abono";
    public static final String APPROVE_TIMEOFF_DESC = "Aprova a solicitação de abono ou correção de esquecimento.";
    public static final String REJECT_TIMEOFF_SUMMARY = "Rejeitar Abono";
    public static final String LIST_TIMEOFF_SUMMARY = "Listar Solicitações de Abono";
    public static final String SWAGGER_USER_TAG = "Usuários";
    public static final String SWAGGER_USER_DESC = "Gestão de credenciais de acesso, perfis e senhas";
    public static final String CREATE_USER_SUMMARY = "Criar Usuário";
    public static final String CREATE_USER_DESC = "Cria credenciais de acesso para um funcionário existente. A senha inicial é gerada automaticamente pelo sistema.";
    public static final String CREATE_USER_SUCCESS = "Usuário criado com sucesso";
    public static final String CREATE_USER_400 = "Nome de usuário já existe no sistema";
    public static final String CREATE_USER_404 = "Funcionário vinculado não encontrado";

    public static final String GET_USER_NAME_SUMMARY = "Buscar Usuário por Username";
    public static final String GET_USER_NAME_DESC = "Recupera dados de acesso buscando pelo login (username).";
    public static final String GET_USER_SUCCESS = "Usuário encontrado";
    public static final String GET_USER_404 = "Usuário não encontrado (ou pertence a outra empresa - isolamento de tenant)";

    public static final String GET_USER_ID_SUMMARY = "Buscar Usuário por ID";
    public static final String GET_USER_ID_DESC = "Recupera dados de acesso pelo ID interno do usuário.";

    public static final String LIST_USERS_SUMMARY = "Listar Usuários";
    public static final String LIST_USERS_DESC = "Lista todos os usuários da empresa do solicitante.";
    public static final String LIST_USERS_404 = "Funcionário do usuário logado não encontrado";

    public static final String UPDATE_USER_SUMMARY = "Atualizar Usuário";
    public static final String UPDATE_USER_DESC = "Gestor atualiza senha, permissões ou status de um usuário.";
    public static final String UPDATE_USER_SUCCESS = "Usuário atualizado com sucesso";
    public static final String UPDATE_USER_400 = "Senha não atende à política de segurança (Min 8 chars, Maiúscula, Minúscula, Número)";

    public static final String ACTIVATE_USER_SUMMARY = "Ativar/Desativar Usuário";
    public static final String ACTIVATE_USER_DESC = "Alterna o status de acesso do usuário (Login permitido/bloqueado).";

    public static final String DELETE_USER_SUMMARY = "Excluir Usuário";
    public static final String DELETE_USER_DESC = "Remove credenciais e dados vinculados.";
    public static final String DELETE_USER_SUCCESS = "Usuário excluído com sucesso";

    public static final String OWN_USER_PROFILE_SUMMARY = "Perfil do Usuário Logado";
    public static final String OWN_USER_PROFILE_DESC = "Retorna os dados de acesso do usuário atual.";
    public static final String OWN_USER_PROFILE_SUCCESS = "Perfil recuperado com sucesso";
    public static final String OWN_USER_PROFILE_404 = "Usuário não encontrado para o token informado";

    public static final String CHANGE_PASS_SUMMARY = "Alterar Minha Senha";
    public static final String CHANGE_PASS_DESC = "Permite que o usuário altere sua própria senha.";
    public static final String CHANGE_PASS_SUCCESS = "Senha alterada com sucesso";
    public static final String CHANGE_PASS_400 = "Erro de validação: Senha atual incorreta, Nova senha não confere ou Política de senha fraca";

    public static final String CHECK_USER_SUMMARY = "Verificar Username";
    public static final String CHECK_USER_DESC = "Verifica disponibilidade de nome de usuário.";
    public static final String CHECK_USER_200 = "Username Indisponível (Já existe)";
    public static final String CHECK_USER_404 = "Username Disponível (Não encontrado)";
}
