package com.kts.kronos.constants;

public class LegalTexts {
    private LegalTexts() {}

    // --- TERMO BIOMETRIA ---
    public static final String BIOMETRIC_TERM_TITLE = "TERMO DE CONSENTIMENTO PARA\nTRATAMENTO DE DADOS BIOMÉTRICOS";
    
    public static final String BIOMETRIC_TERM_BODY = 
        "O TITULAR autoriza, de forma livre, informada e inequívoca, o tratamento de seus dados pessoais sensíveis, " +
        "especificamente sua IMAGEM FACIAL (Biometria), para a finalidade exclusiva de REGISTRO E CONTROLE DE JORNADA DE TRABALHO, " +
        "em conformidade com a Lei Geral de Proteção de Dados (Lei nº 13.709/2018) e a Portaria 671/2021 do Ministério do Trabalho e Previdência.";

    public static final String BIOMETRIC_ITEM_1 = "FINALIDADE: Autenticação segura da identidade no momento do registro de ponto eletrônico, prevenindo fraudes.";
    public static final String BIOMETRIC_ITEM_2 = "ARMAZENAMENTO: Os dados serão armazenados em ambiente seguro de computação em nuvem (SaaS) provido pela KRONOS TECH SOLUTIONS.";
    public static final String BIOMETRIC_ITEM_3 = "REVOGAÇÃO: Este consentimento poderá ser revogado a qualquer momento pelo Titular, mediante solicitação expressa ao departamento de Recursos Humanos.";
    public static final String BIOMETRIC_ITEM_4 = "RETENÇÃO: A imagem facial, os templates biométricos e o termo de consentimento serão mantidos apenas enquanto houver consentimento biométrico ativo e necessidade operacional vinculada ao registro de jornada. A revogação do consentimento, a exclusão do usuário ou a exclusão do colaborador disparam a purga dos artefatos biométricos e a remoção dos documentos de consentimento.";

    public static final String BIOMETRIC_FOOTER_BOX = "Este documento foi assinado digitalmente através da plataforma KRONOS, garantindo autenticidade e integridade conforme MP 2.200-2/2001.";

    // --- ATESTADO TÉCNICO ---
    public static final String TECH_CERT_TITLE = "ATESTADO TÉCNICO E TERMO DE RESPONSABILIDADE\n(PORTARIA MTP 671/2021)";
    
    public static final String TECH_CERT_DECLARATION = 
        "Declaramos para os devidos fins que o software mencionado atende integralmente aos requisitos do REGISTRADOR ELETRÔNICO DE PONTO VIA PROGRAMA (REP-P), conforme Art. 76 a 80 da Portaria 671/2021.\n\n" +
        "O sistema garante:\n" +
        "I - Registro fiel das marcações, sem restrições de horário;\n" +
        "II - Geração do Arquivo Fonte de Dados (AFD) e Arquivo Eletrônico de Jornada (AEJ);\n" +
        "III - Assinatura digital padrão ICP-Brasil nos documentos fiscais;\n" +
        "IV - Sincronismo de relógio com NTP.br e auditoria de alterações.";
    
    public static final String TECH_CERT_FOOTER = "Este documento digital deve ser validado via assinatura eletrônica P7S anexa.";
}
