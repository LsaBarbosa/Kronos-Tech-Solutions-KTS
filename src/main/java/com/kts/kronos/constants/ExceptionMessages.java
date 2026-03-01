package com.kts.kronos.constants;

public class ExceptionMessages {
    private ExceptionMessages() {
    }

    public static final String FORBIDDEN_RESOURCE_ACCESS = "Sem permissão para utilizar esse recurso";
    public static final String COMPANY_NOT_FOUND = "Empresa não encontrada";
    public static final String EMPLOYEE_NOT_FOUND = "Colaborador não encontrado.";
    public static final String LOCAL_FILE_NOT_FOUND = "Arquivo não encontrado no disco: ";
    public static final String LOCAL_FILE_SAVE_ERROR = "Falha ao salvar o arquivo no disco.";
    public static final String LOCAL_FILE_READ_ERROR = "Falha ao ler o arquivo do disco.";
    public static final String LOCAL_FILE_DELETE_ERROR = "Falha ao excluir o arquivo do disco.";
    public static final String REKOGNITION_INIT_ERROR = "Falha na inicialização do serviço Rekognition.";
    public static final String REKOGNITION_REGISTER_FACE_ERROR = "Falha ao registrar face no Rekognition.";
    public static final String REKOGNITION_IMAGE_READ_ERROR = "Falha ao ler a imagem para reconhecimento.";
    public static final String FACIAL_AUTH_UNAVAILABLE = "Serviço de autenticação facial indisponível. Tente novamente.";
    public static final String FACIAL_RECOGNITION_SERVICE_ERROR = "Falha no serviço de reconhecimento facial.";
    public static final String EMAIL_CONFIGURATION_ERROR = "Falha na configuração do e-mail de recuperação.";
    public static final String EMAIL_SEND_ERROR = "Falha no envio do e-mail de recuperação.";
    public static final String EMAIL_REQUIRED_DATA_MISSING = "Dados obrigatórios para envio de e-mail não informados.";
    public static final String EMAIL_SENDER_CONFIG_MISSING = "Configuração de remetente de e-mail (mail.username) está ausente.";
    public static final String S3_FACE_PREPARE_UPLOAD_ERROR = "Falha ao preparar a imagem para upload no S3.";
    public static final String S3_FACE_SAVE_ERROR = "Falha ao salvar a imagem no S3.";
    public static final String S3_COMMUNICATION_ERROR = "Erro de comunicação com Storage S3";
    public static final String S3_READ_ERROR = "Falha de leitura do arquivo no S3";
    public static final String S3_NOT_FOUND_OR_ERROR = "Arquivo não encontrado ou erro S3";
}
