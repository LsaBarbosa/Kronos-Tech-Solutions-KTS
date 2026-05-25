package com.kts.kronos.constants;

public class ApiPaths {
    private ApiPaths() {
    }

    // Companies
    public static final String COMPANIES = "/companies";
    public static final String BY_CNPJ = "/{cnpj}";
    public static final String TOGGLE_ACTIVATE = "/{cnpj}/toggle-activate";
    public static final String CHECK_CNPJ = "/check-cnpj";


    // Documents
    public static final String DOCUMENTS = "/documents";
    public static final String DOCUMENT_ID = "/{documentId}";

    // Employee
    public static final String EMPLOYEE = "/employee";
    public static final String EMPLOYEE_ID = "/{employeeId}";
    public static final String UPDATE_EMPLOYEE = "/manager/update-employee/{employeeId}";
    public static final String UPDATE_OWN_PROFILE = "/update-own-profile";
    public static final String OWN_PROFILE = "/own-profile";
    public static final String CHECK_CPF = "/check-cpf";
    public static final String TOGGLE_ACTIVATE_EMPLOYEE = "/toggle-activate/{employeeId}";

    // Recods
    public static final String RECORDS = "/records";
    public static final String CHECKIN = "/checkin";
    public static final String UPDATE_TIME_RECORD = "/update/time-record/{timeRecordId}";
    public static final String UPDATE_STATUS = "/update/status/{employeeId}/{timeRecordId}";
    public static final String TOGGLE_ACTIVATE_RECORD = "/toggle-activate/{employeeId}/{timeRecordId}";
    public static final String DELETE_RECORD = "/{employeeId}/{timeRecordId}";
    public static final String REPORT = "/report";
    public static final String REJECT_UPDATE = "/reject/{timeRecordId}";
    public static final String APPROVE_UPDATE = "/approve/{timeRecordId}";
    public static final String PENDING_APPROVALS = "/pending-approvals";
    public static final String VACATION_REQUEST = "/vacation-request";
    public static final String VACATION_APPROVE = "/vacation-request/approve";
    public static final String VACATION_REJECT = "/vacation-request/reject";
    public static final String TIME_OFF_REQUEST = "/time-off/request";
    public static final String TIME_OFF_REQUESTS = "/time-off/requests";
    public static final String TIME_OFF_APPROVE = "/time-off/approve/{timeRecordId}";
    public static final String TIME_OFF_REJECT = "/time-off/reject/{timeRecordId}";

    //User
    public static final String USER = "/users";
    public static final String USERS = "/search";
    public static final String USER_BY_USERNAME = "/search/username/{userName}";
    public static final String USER_BY_ID = "/search/id/{userId}";
    public static final String UPDATE_USER = "/search/{userId}";
    public static final String TOGGLE_ACTIVATE_USER = "/toggle-activate/{userId}";
    public static final String DELETE_USER = "/{userId}";
    public static final String PASSWORD = "/password";
    public static final String OWN_USER_PROFILE = "/own-profile";
    public static final String CHECK_USERNAME = "/check-username";
    public static final String LOGIN_FACE = "/login-face";



    //Message
    public static final String MESSAGES = "/messages";
    public static final String MESSAGE_ID = "/{messageId}";
    public static final String MESSAGES_SEEN = "/mark-messages-seen";


    //Auth
    public static final String AUTH = "/auth";
    public static final String LOGIN = "/login";
    public static final String RECOVER_PASSWORD = "/recover-password"; // Novo
    public static final String RESET_PASSWORD = "/reset-password";
    public static final String REFRESH = "/refresh";

    //Cep
    public static final String API_VIA_CEP = "https://viacep.com.br/ws";
    public static final String GEOLOCATION = "/geolocation";
    public static final String RESOLVE = "/resolve";

    // LGPD
    public static final String LGPD = "/lgpd";
    public static final String LGPD_REQUESTS = "/requests";
    public static final String LGPD_REQUEST_ID = "/requests/{requestId}";
    public static final String LGPD_REQUEST_STATUS = "/requests/{requestId}/status";
    public static final String LGPD_REQUEST_HISTORY = "/requests/{requestId}/history";
    public static final String LGPD_EMPLOYEE_EXPORT = "/employees/{employeeId}/export";
    public static final String LGPD_EMPLOYEE_ANONYMIZE = "/employees/{employeeId}/anonymize";
    public static final String LGPD_RETENTION_EXECUTIONS = "/retention/executions";
    public static final String LGPD_RETENTION_EXECUTION_ID = "/retention/executions/{executionId}";
    public static final String LGPD_INVENTORY = "/inventory";
    public static final String LGPD_INVENTORY_ACTIVE = "/inventory/active";
    public static final String LGPD_INVENTORY_BY_CODE = "/inventory/{processCode}";
    public static final String LGPD_INVENTORY_ID = "/inventory/{inventoryId}";
    public static final String LGPD_PROCESSING_CATALOG = "/processing-catalog";
    public static final String LGPD_RETENTION_DRY_RUN = "/admin/retention/dry-run";

    // Security Incidents
    public static final String SECURITY_INCIDENTS = "/security-incidents";
    public static final String SECURITY_INCIDENT_ID = "/{incidentId}";

}
