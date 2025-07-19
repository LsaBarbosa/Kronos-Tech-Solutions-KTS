package com.kts.kronos.constants;

public class ApiPaths {
    private ApiPaths() {}

    // Companies
    public static final String COMPANIES = "/companies";
    public static final String BY_CNPJ = "/{cnpj}";
    public static final String TOGGLE_ACTIVATE_EMPLOYEE = "/{cnpj}/toggle-activate";

    // Documents
    public static final String DOCUMENTS = "/documents/{employeeId}";
    public static final String DOCUMENT_ID = "/{documentId}";

    // Employee
    public static final String EMPLOYEE = "/employee";
    public static final String EMPLOYEE_ID = "/{employeeId}";
    public static final String UPDATE_EMPLOYEE = "/manager/update-employee/{employeeId}";
    public static final String UPDATE_OWN_PROFILE = "/update-own-profile/{employeeId}";

    // Recods
    public static final String RECORDS = "/records";
    public static final String CHECKIN = "/checkin/{employeeId}";
    public static final String CHECKOUT = "/checkout/{employeeId}";
    public static final String UPDATE_TIME_RECORD = "/update/time-record/{employeeId}/{timeRecordId}";
    public static final String UPDATE_STATUS = "/update/status/{employeeId}/{timeRecordId}";
    public static final String TOGGLE_ACTIVATE_RECORD = "/toggle-activate/{employeeId}/{timeRecordId}";
    public static final String DELETE_RECORD = "records/{employeeId}/{timeRecordId}";
    public static final String REPORT = "/report/{employeeId}";
    public static final String SIMPLE_REPORT = "/report/simple/{employeeId}";
    public static final String REPORT_PDF = "report/{employeeId}/pdf";
    public static final String REPORT_SIMPLE_PDF = "report/simple/{employeeId}/pdf";


    //User
    public static final String USER = "/users";
    public static final String USERS = "/search";
    public static final String USER_BY_USERNAME = "/search/{userName}";
    public static final String USER_BY_ID = "/search/{userId}";
    public static final String UPDATE_USER = "/search/{userId}";
    public static final String TOGGLE_ACTIVATE_USER = "/toggle-activate/{userId}";
    public static final String DELETE_USER = "/{userId}";

    //Cep
    public static final String API_VIA_CEP = "https://viacep.com.br/ws";

}
