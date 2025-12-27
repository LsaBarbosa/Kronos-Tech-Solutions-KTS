package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AejUseCase;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.infrastructure.DigitalSignatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AejService implements AejUseCase {

    private final CompanyProvider companyProvider;
    private final EmployeeProvider employeeProvider;
    private final TimeRecordProvider recordRepository;
    private final DigitalSignatureService signatureService;

    @Value("${kronos.legal.inpi-number:999999999}")
    private String inpiNumber;

    @Value("${kronos.legal.dev-name:KRONOS TECH SOLUTIONS}")
    private String developerName;

    @Value("${kronos.legal.software-version:1.0}")
    private String softwareVersion;

    @Override
    @Transactional(readOnly = true)
    public void generateAej(UUID companyId, LocalDate startDate, LocalDate endDate, OutputStream outputStream) {
        var company = companyProvider.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

        log.info("Iniciando geração de AEJ para empresa {} de {} a {}", companyId, startDate, endDate);

        // Buffer em memória para montar o texto antes de assinar
        try (var textBuffer = new ByteArrayOutputStream();
             var writer = new PrintWriter(textBuffer, true, StandardCharsets.ISO_8859_1)) {

            // --- GERAÇÃO DO CONTEÚDO TEXTUAL (LAYOUT PORTARIA 671) ---

            // 1. REGISTRO 01: CABEÇALHO
            writeLine(writer, generateType01(company, startDate, endDate));

            // 2. REGISTRO 02: IDENTIFICAÇÃO DO REP-P
            writeLine(writer, generateType02());

            // 3. LOOP DE FUNCIONÁRIOS
            List<Employee> employees = employeeProvider.findByCompanyId(company.companyId());
            int sequenceId = 1;

            for (var employee : employees) {
                // ID de Vínculo Sequencial no arquivo
                var bondId = String.format("%09d", sequenceId++);

                // REGISTRO 03: VÍNCULO
                writeLine(writer, generateType03(bondId, employee));

                // REGISTRO 04: HORÁRIO CONTRATUAL
                var scheduleId = "H" + bondId;
                writeLine(writer, generateType04(scheduleId, employee));

                // Busca registros do período
                List<TimeRecord> records = recordRepository.findByEmployeeId(employee.employeeId()).stream()
                        .filter(r -> r.startWork() != null)
                        .filter(r -> !r.startWork().toLocalDate().isBefore(startDate) && !r.startWork().toLocalDate().isAfter(endDate))
                        .sorted(Comparator.comparing(TimeRecord::startWork))
                        .toList();

                // REGISTRO 05: MARCAÇÕES
                for (var record : records) {
                    generateType05Lines(bondId, scheduleId, record).forEach(line -> writeLine(writer, line));
                }

                // REGISTRO 07: AUSÊNCIAS E FÉRIAS
                for (var record : records) {
                    if (isAbsence(record)) {
                        writeLine(writer, generateType07(bondId, record));
                    }
                }
            }

            // 4. REGISTRO 08: IDENTIFICAÇÃO DO DESENVOLVEDOR (PTRP)
            writeLine(writer, generateType08());

            // 5. REGISTRO 99: TRAILER
            writeLine(writer, "99|");

            // Força a escrita no buffer
            writer.flush();

            // --- PROCESSO DE ASSINATURA DIGITAL ---

            byte[] originalContent = textBuffer.toByteArray();
            log.info("Layout AEJ gerado com sucesso. Tamanho original: {} bytes. Iniciando assinatura...", originalContent.length);

            // Assina o conteúdo (Gera o .p7s)
            byte[] signedContent = signatureService.signData(originalContent);

            // Escreve o conteúdo assinado na saída (Download)
            outputStream.write(signedContent);

            log.info("AEJ assinado digitalmente e enviado para output. Tamanho final: {} bytes.", signedContent.length);

        } catch (Exception e) {
            log.error("Erro crítico na geração/assinatura do AEJ", e);
            throw new RuntimeException(FAILURE_TO_GENERAT_AEJ + e.getMessage());
        }
    }

    private void writeLine(PrintWriter writer, String line) {
        // Padrão Windows (CRLF) é exigido por muitos validadores legados, embora Portaria 671 aceite LF.
        // Usamos \r\n para compatibilidade máxima.
        writer.print(line + "\r\n");
    }

    // --- GERADORES DE LINHA (LAYOUTS) ---

    private String generateType01(Company c, LocalDate start, LocalDate end) {
        return String.join("|", "01", "1", formatOnlyNumbers(c.cnpj()), "", formatText(c.name(), 150),
                start.format(DATE_FMT), end.format(DATE_FMT),
                LocalDateTime.now().format(GENERATION_DATE_FMT), "001") + "|";
    }

    private String generateType02() {
        return String.join("|", "02", "001", "4", this.inpiNumber) + "|";
    }

    private String generateType03(String bondId, Employee e) {
        return String.join("|", "03", bondId, formatOnlyNumbers(e.cpf()), formatText(e.fullName(), 150),
                e.phone() != null ? formatOnlyNumbers(e.phone()) : "") + "|";
    }

    private String generateType04(String scheduleId, Employee e) {
        // Horários Padrão (Fallback se não tiver configurado no funcionário)
        var start = e.workStartTime() != null ? e.workStartTime() : LocalTime.of(8, 0);
        var end = e.workEndTime() != null ? e.workEndTime() : LocalTime.of(17, 0);
        var breakStart = e.breakStartTime() != null ? e.breakStartTime() : LocalTime.of(12, 0);
        var breakEnd = e.breakEndTime() != null ? e.breakEndTime() : LocalTime.of(13, 0);

        long dailyMinutes = e.getDailyWorkMinutes();

        return String.join("|",
                "04",
                scheduleId,
                String.valueOf(dailyMinutes),
                start.format(TIME_FMT),     // Entrada
                breakStart.format(TIME_FMT),// Saída Almoço
                breakEnd.format(TIME_FMT),  // Volta Almoço
                end.format(TIME_FMT)        // Saída
        ) + "|";
    }

    private List<String> generateType05Lines(String bondId, String scheduleId, TimeRecord r) {
        List<String> lines = new ArrayList<>();
        if (isAbsence(r)) return Collections.emptyList();

        // Linha de Entrada (Check-in)
        if (r.startWork() != null) {
            var source = determineSource(r.edited(), r.startWork(), r.originalStartWork());
            lines.add(String.join("|", "05", bondId, formatDateTimeIso(r.startWork()),
                    "001", "E", "", source, scheduleId, "") + "|");
        }

        // Linha de Saída (Check-out)
        if (r.endWork() != null) {
            var source = determineSource(r.edited(), r.endWork(), r.originalEndWork());
            lines.add(String.join("|", "05", bondId, formatDateTimeIso(r.endWork()),
                    "001", "S", "", source, scheduleId, "") + "|");
        }
        return lines;
    }

    // Lógica para determinar a Fonte da Marcação (Original 'O' ou Editada/Inserida 'I')
    private String determineSource(boolean isEditedRecord, LocalDateTime current, LocalDateTime original) {
        if (!isEditedRecord && current.equals(original)) {
            return "O"; // Original
        }
        // Se foi editado OU se não tem original (inserção manual posterior), é 'I'
        return "I";
    }

    private String generateType07(String bondId, TimeRecord r) {
        var type = "05"; // Default: Outras Ausências
        if (r.statusRecord() == StatusRecord.VACATION) type = "04"; // Férias

        long minutes = 0;
        // Se tem início e fim definidos (ex: meio período de folga), calcula.
        // Se é o dia todo, geralmente assume-se jornada diária padrão (ex: 480 min).
        if (r.startWork() != null && r.endWork() != null) {
            minutes = Duration.between(r.startWork(), r.endWork()).toMinutes();
        } else {
            minutes = 480; // Fallback para dia cheio (ajustar conforme regra de negócio)
        }

        return String.join("|", "07", bondId, type,
                r.startWork().toLocalDate().format(DATE_FMT), String.valueOf(minutes), "") + "|";
    }

    private String generateType08() {
        return String.join("|", "08",
                "KRONOS SYSTEM",
                this.softwareVersion,
                "1", // Tipo do sistema (1 = REP-P)
                "00000000000000", // CNPJ Desenvolvedor (Se tiver, coloque aqui ou crie @Value)
                formatText(this.developerName, 150),
                "suporte@kronos.com.br") + "|";
    }

    private boolean isAbsence(TimeRecord r) {
        return r.statusRecord() == StatusRecord.VACATION
                || r.statusRecord() == StatusRecord.TIME_OFF
                || r.statusRecord() == StatusRecord.ABSENCE;
    }

    // --- UTILITÁRIOS DE FORMATAÇÃO ---

    private String formatOnlyNumbers(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    private String formatText(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) : s;
    }

    private String formatDateTimeIso(LocalDateTime dt) {
        // Formato ISO extendido exigido no layout: yyyy-MM-ddThh:mm:ss-Offset
        // Aqui fixamos -0300 (Brasília), mas o ideal é pegar do ZoneId se multi-região.
        return dt.format(GENERATION_DATE_FMT) + "-0300";
    }
}