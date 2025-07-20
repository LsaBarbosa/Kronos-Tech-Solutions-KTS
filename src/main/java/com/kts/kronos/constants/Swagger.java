package com.kts.kronos.constants;

public class Swagger {
    private Swagger() {
    }

    // Default Description
    public static final String GET_ALL = "Lista todos objetos.";
    public static final String CREATE = "Cria um Objeto.";
    public static final String DELETE = "Deleta um Objeto.";
    public static final String UPDATE = "Altera um Objeto.";
    public static final String TOGGLE = "Alterna um valor do Objeto.";
    public static final String GET_BY_PARAMETER = "Retorna um objeto com base no parametro.";
    public static final String CREATE_DESCRIPTION = "Ao passar os dados necessários o objeto é Criado e persiste no banco de dados.";
    public static final String UPDATE_DESCRIPTION = "Ao passar os dados necessários o objeto é Alterado e persiste no banco de dados.";
    public static final String TOGGLE_DESCRIPTION = "Ao chamar o endpoint Alterna o estado do Objeto entre ativo/inativo e persiste no banco de dados.";
    public static final String ALL_OBJECTS_DESCRIPTION = "Retorna todos os objetos e seus dados, baseado no valor do parametro 'active'.";

    // Company Description
    public static final String COMPANY_API = "APi Empresa.";
    public static final String COMPANY_DESCRIPTION_API = "End-points para gestão da empresa.";
    public static final String DELETE_COMPANY_DESCRIPTION = "Ao passar o CNPJ como parametro, Deleta os dados de uma empresa e persiste no banco de dados.";
    public static final String GET_COMPANY_DESCRIPTION = "Ao passar o CNPJ como parametro, Retorna os dados de uma empresa.";

    // User Description
    public static final String USER_API = "APi Usiários";
    public static final String USER_DESCRIPTION_API = "End-points para gestão dos Usuários.";
    public static final String DELETE_USER_DESCRIPTION = "Ao passar o Id do usuário como parametro, Deleta os dados de um usuário e persiste no banco de dados.";
    public static final String USER_DESCRIPTION = "Ao passar o nome do usuário como parametro, Retorna os dados de um Usuário.";
    public static final String USER_DESCRIPTION_ID = "Ao passar o nome do usuário como parametro, Retorna os dados de um Usuário.";

    // Document Description
    public static final String DOCUMENT_API = "APi Documentos";
    public static final String DOCUMENT_DESCRIPTION_API = "End-points para gestão dos Documentos.";
    public static final String DELETE_DOCUMENT_DESCRIPTION = "Ao passar o Id do documento como parametro, Deleta os dados de um documento e persiste no banco de dados.";
    public static final String DOCUMENT_DESCRIPTION = "Ao passar os Ids do colaborador e do documento, Retorna o arquivo.";

    // Employee Description
    public static final String EMPLOYEE_API = "APi Colaboradores";
    public static final String EMPLOYEE_DESCRIPTION_API = "End-points para gestão dos Colaboradores.";
    public static final String DELETEE_EMPLOYEE_DESCRIPTION = "Ao passar o Id do colaborador como parametro, Deleta os dados de um colaborador e persiste no banco de dados.";
    public static final String EMPLOYEE_DESCRIPTION = "Ao passar os Id do colaborador, Retorna o colaborador.";

    // TimeRecord Description
    public static final String TIMERECORD_API = "APi Registro de Horas";
    public static final String TIMERECORD_DESCRIPTION_API = "End-points para gestão dos registros de horas dos  colaboradores.";
    public static final String CHECKIN_DESCRIPTION = "Ao passar o id do Employee, o sistema registra a data e hora de entrada no momento atual da chamada.";
    public static final String CHECKOUT_DESCRIPTION = "Ao passar o id do Employee, o sistema registra a data e hora de saída no momento atual da chamada.";
    public static final String UPDATE_RECORD_DESCRIPTION = "Ao passar o id do Employee,id do registro e data e hora de entrada (e/ou) saída, e o sistema persiste no banco de dados.";
    public static final String UPDATE_STATUS_DESCRIPTION = "Ao passar o id do Employee, e o Status , e o sistema persiste no banco de dados.";
    public static final String DELETEE_TIMERECORD_DESCRIPTION = "Ao passar o id do Employee,id do registro como parametro, Deleta os dados de um registro e persiste no banco de dados.";
    public static final String TIMERECORD_DOWNLOAD_DESCRIPTION = "Ao passar o Id do employee e/ou parametros como data,status e referencias das horas trabalhadas, o sistema gera um relatório em pdf.";

    // Schema
        // Address Request
    public static final String ADDRESS_REQUEST = "AddressRequest";
    public static final String UPDATE_ADDRESS_REQUEST = "UpdateAddressRequest";
    public static final String DTO_ADDRESS_REQUEST = "DTO para requisição de endereço";
    public static final String DTO_UPDATE_ADDRESS_REQUEST = "DTO para requisição de alteração do endereço";
    public static final String POSTAL_CODE = "CEP (8 dígitos)";
    public static final String POSTAL_CODE_EXEMPLE = "01234567";
    public static final String ADDRESS_NUMBER = "Número do endereço";
    public static final String ADDRESS_NUMBER_EXEMPLE = "123";

    public static final String CREATE_COMPANY_REQUEST = "CreateCompanyRequest";
    public static final String UPDATE_COMPANY_REQUEST = "UpdateCompanyRequest";
    public static final String DTO_CREATE_COMPANY_REQUEST = "DTO para criação de empresa";
    public static final String DTO_UPDATE_COMPANY_REQUEST = "DTO para alteração dos dados da empresa";
    public static final String NAME_COMPANY_EXEMPLE = "Empresa XYZ Ltda.";
    public static final String NAME_COMPANY = "Nome da empresa";
    public static final String CNPJ = "CNPJ (14 dígitos)";
    public static final String CNPJ_EXEMPLE = "12345678000199";
    public static final String EMAIL = "E-mail de contato";
    public static final String EMAIL_EXEMPLE = "contato@domino.com";
    public static final String STATUS = "Status de ativo ou inativo";

    public static final String CREATE_EMPLOYEE_REQUEST = "CreateEmployeeRequest";
    public static final String UPDATE_EMPLOYEE_REQUEST = "UpdateEmployeeRequest";
    public static final String RECOVER_PASSWORD_EMPLOYEE_REQUEST = "RecoverPasswordRequest";
    public static final String DTO_RECOVER_PASSWORD_EMPLOYEE_REQUEST = "DTO para recuperação de senha";
    public static final String DTO_CREATE_EMPLOYEE_REQUEST = "DTO para criação de colaborador";
    public static final String DTO_UPDATE_EMPLOYEE_REQUEST = "DTO para alteração dos dados do colaborador";
    public static final String NAME_EMPLOYEE_EXEMPLE = "João da Silva";
    public static final String NAME_EMPLOYEE = "Nome completo";
    public static final String CPF = "CPF (11 dígitos)";
    public static final String CPF_EXEMPLE = "12345678901";
    public static final String JOB_POSITION = "Cargo";
    public static final String JOB_POSITION_EXEMPLE = "Status de ativo";
    public static final String SALARY = "Salário";
    public static final String SALARY_EXEMPLE = "3500.00";
    public static final String PHONE = "Telefone";
    public static final String PHONE_EXEMPLE = "21964032585";

    public static final String LIST_RECORD_REQUEST = "ListReportRequest";
    public static final String UPDATE_RECORD_REQUEST = "UpdateTimeRecordRequest";
    public static final String UPDATE_RECORD_STATUS_REQUEST = "UpdateTimeRecordStatusRequest";
    public static final String DTO_UPDATE_RECORD_REQUEST = "DTO para alteração dos dados doregistro";
    public static final String DTO_UPDATE_RECORD_STATUS_REQUEST = "DTO para alterar o status de um registro";
    public static final String SIMPLE_RECORD_REQUEST = "SimpleReportRequest";
    public static final String DTO_SIMPLE_RECORD_REQUEST = "DTO para relatório simples de horas";
    public static final String REFERENCE =  "Referência de horas (HH:mm)";
    public static final String DTO_LIST_RECORD_REQUEST = "DTO para filtros de listagem de registros";
    public static final String REFERENCE_EXEMPLE = "08:00:17:00";
    public static final String STATUS_EXEMPLE = "true";
    public static final String STATUS_RECORD = "Status do registro";
    public static final String STATUS_RECORD_EXEMPLE = "CREATED";
    public static final String DATE = "Datas para filtro (dd-MM-yyyy)";
    public static final String DATE_EXEMPLE = "17-01-2025";
    public static final String DATE_EXEMPLE_ARRAY = "[\"07-01-2025\", \"17-01-2025\"]";
    public static final String HOUR_EXEMPLE = "17:05";
    public static final String HOUR = "Horas no formato HH:mm";

    public static final String CREATE_USER_REQUEST = "CreateUserRequest";
    public static final String UPDATE_USER_REQUEST = "UpdateUserRequest";
    public static final String DTO_UPDATE_USER_REQUEST = "DTO para alteração dos dados do usuário";
    public static final String DTO_CREATE_USER_REQUEST = "DTO para criação do usuário";
    public static final String NAME_USER = "Nome de usuário";
    public static final String NAME_USER_EXEMPLE = "joao.silva";
    public static final String PASSWORD = "Senha do usuário";
    public static final String PASSWORD_EXEMPLE = "senha_do_usuario";
    public static final String ROLE = "Permissão do usuário";
    public static final String ROLE_EXEMPLE = "MANAGER";
    public static final String ID_USER = "Identificador do usuário";
    public static final String ID_USER_EXEMPLE = "Identificador do usuário";

}
