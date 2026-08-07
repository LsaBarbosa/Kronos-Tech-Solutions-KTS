package com.kts.kronos.application.service.afd;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AfdParser {

    private static final DateTimeFormatter AFD_DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    public record AfdHeader(String nsr, String cnpj, String cei, String razaoSocial) {}

    public record AfdEmployee(String nsr, OffsetDateTime datetime, char operacao, String pis, String nome, String cnpj) {}

    public record AfdMark(String nsr, OffsetDateTime markDatetime, String pis, OffsetDateTime recordDatetime, String tipoOp, String hash) {}

    public record AfdParseResult(AfdHeader header, List<AfdEmployee> employees, List<AfdMark> marks, int skippedLines) {}

    public AfdParseResult parse(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readAllBytes();
        String content;
        try {
            content = new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            content = new String(bytes, Charset.forName("ISO-8859-1"));
        }

        AfdHeader header = null;
        List<AfdEmployee> employees = new ArrayList<>();
        List<AfdMark> marks = new ArrayList<>();
        int skipped = 0;

        String[] lines = content.split("\r?\n");
        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum];
            if (line.isBlank()) continue;
            if (line.startsWith("ASSINATURA_DIGITAL_EM_ARQUIVO_P7S")) continue;
            if (line.length() < 10) {
                skipped++;
                continue;
            }

            char tipo = line.charAt(9);

            try {
                switch (tipo) {
                    case '1' -> {
                        if (line.length() < 186) {
                            log.warn("AFD parse: tipo 1 linha {} muito curta ({})", lineNum + 1, line.length());
                            skipped++;
                            continue;
                        }
                        header = new AfdHeader(
                                line.substring(0, 9),
                                line.substring(10, 24).trim(),
                                line.substring(24, 36).trim(),
                                line.substring(36, 186).trim()
                        );
                    }
                    case '5' -> {
                        if (line.length() < 113) {
                            log.warn("AFD parse: tipo 5 linha {} muito curta ({})", lineNum + 1, line.length());
                            skipped++;
                            continue;
                        }
                        employees.add(new AfdEmployee(
                                line.substring(0, 9),
                                parseDateTime(line.substring(10, 34), lineNum + 1),
                                line.charAt(34),
                                line.substring(35, 46).trim(),
                                line.substring(47, Math.min(99, line.length())).trim(),
                                line.length() >= 113 ? line.substring(99, 113).trim() : ""
                        ));
                    }
                    case '7' -> {
                        if (line.length() < 72) {
                            log.warn("AFD parse: tipo 7 linha {} muito curta ({})", lineNum + 1, line.length());
                            skipped++;
                            continue;
                        }
                        String hashVal = line.length() >= 136 ? line.substring(72, 136) : "";
                        marks.add(new AfdMark(
                                line.substring(0, 9),
                                parseDateTime(line.substring(10, 34), lineNum + 1),
                                line.substring(34, 45).trim(),
                                parseDateTime(line.substring(46, 70), lineNum + 1),
                                line.substring(70, 72),
                                hashVal
                        ));
                    }
                    case '9' -> { /* trailer — ignorar */ }
                    default -> {
                        log.warn("AFD parse: tipo desconhecido '{}' na linha {}", tipo, lineNum + 1);
                        skipped++;
                    }
                }
            } catch (Exception e) {
                log.warn("AFD parse: erro na linha {} — {} — pulando", lineNum + 1, e.getMessage());
                skipped++;
            }
        }

        if (header == null) {
            throw new IllegalArgumentException("Arquivo AFD inválido: cabeçalho tipo 1 não encontrado.");
        }

        return new AfdParseResult(header, employees, marks, skipped);
    }

    private OffsetDateTime parseDateTime(String raw, int lineNum) {
        try {
            return OffsetDateTime.parse(raw.trim(), AFD_DT_FMT);
        } catch (DateTimeParseException e) {
            log.warn("AFD parse: datetime inválido '{}' na linha {} — usando agora", raw, lineNum);
            return OffsetDateTime.now();
        }
    }
}
