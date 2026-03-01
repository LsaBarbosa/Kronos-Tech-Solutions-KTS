package com.kts.kronos.constants;

public class PathValues {
    private PathValues() {
    }

    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String HEADER_USER_AGENT = "User-Agent";
    public static final String COMMA_SEPARATOR = ",";
    public static final String DOWNLOAD_ATTACHMENT_PREFIX = "attachment; filename=\"";
    public static final String DOWNLOAD_ATTACHMENT_SUFFIX = "\"";
    public static final String MEDIA_TYPE_PKCS7 = "application/pkcs7-signature";
    public static final String FILE_NAME_TECHNICAL_CERTIFICATE_PATTERN = "Atestado_Tecnico_Kronos_%s.p7s";
    public static final String FILE_NAME_AFD_PATTERN = "AFD_%s.txt";
    public static final String FILE_NAME_AEJ_PATTERN = "AEJ_%s_%s.p7s";
    public static final String FILE_NAME_MIRROR_PATTERN = "Espelho_%s_%s.pdf";
    public static final String ROLE_MANAGER = "MANAGER";
}
