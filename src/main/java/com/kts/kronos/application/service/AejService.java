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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AejService implements AejUseCase {

    private final CompanyProvider companyProvider;
    private final EmployeeProvider employeeProvider;
    private final TimeRecordProvider recordRepository;

    private static final String SOFTWARE_VERSION = "1.0";
    private static final String DEV_NAME = "KRONOS TECH SOLUTIONS";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmm");

    @Override
    @Transactional(readOnly = true)
    public void generateAej(UUID companyId, LocalDate startDate, LocalDate endDate, OutputStream outputStream) {
        Company company = companyProvider.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));

        try (PrintWriter writer = new PrintWriter(outputStream, true, StandardCharsets.ISO_8859_1)) {

            // 1. REGISTRO 01: CABEÇALHO
            writeLine(writer, generateType01(company, startDate, endDate));

            // 2. REGISTRO 02: REPs
            writeLine(writer, generateType02(company));

            // 3. LOOP DE FUNCIONÁRIOS
            List<Employee> employees = employeeProvider.findByCompanyId(company.companyId());
            int sequenceId = 1;

            for (Employee employee : employees) {
                // ID de Vínculo Sequencial no arquivo
                String bondId = String.format("%09d", sequenceId++);

                // REGISTRO 03: VÍNCULO
                writeLine(writer, generateType03(bondId, employee));

                // REGISTRO 04: HORÁRIO CONTRATUAL (Dinâmico por Funcionário)
                // Gera um ID de horário único baseado no vínculo para simplificar (1 para 1)
                String scheduleId = "H" + bondId;
                writeLine(writer, generateType04(scheduleId, employee));

                // Busca registros do período
                List<TimeRecord> records = recordRepository.findByEmployeeId(employee.employeeId()).stream()
                        .filter(r -> !r.startWork().toLocalDate().isBefore(startDate) && !r.startWork().toLocalDate().isAfter(endDate))
                        .sorted(Comparator.comparing(TimeRecord::startWork))
                        .toList();

                // REGISTRO 05: MARCAÇÕES
                for (TimeRecord record : records) {
                    // Passamos o scheduleId dinâmico
                    generateType05Lines(bondId, scheduleId, record).forEach(line -> writeLine(writer, line));
                }

                // REGISTRO 07: AUSÊNCIAS
                for (TimeRecord record : records) {
                    if (isAbsence(record)) {
                        writeLine(writer, generateType07(bondId, record));
                    }
                }
            }

            // 4. REGISTRO 08: PTRP
            writeLine(writer, generateType08(company));

            // 5. REGISTRO 99: TRAILER
            writeLine(writer, "99|");

            writer.flush();
        } catch (Exception e) {
            log.error("Erro na geração do AEJ", e);
            throw new RuntimeException("Falha ao gerar arquivo AEJ: " + e.getMessage());
        }
    }

    private void writeLine(PrintWriter writer, String line) {
        writer.print(line + "\r\n");
    }

    // --- GERADORES DE LINHA ---

    private String generateType01(Company c, LocalDate start, LocalDate end) {
        return String.join("|", "01", "1", formatOnlyNumbers(c.cnpj()), "", formatText(c.name(), 150),
                start.format(DATE_FMT), end.format(DATE_FMT),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")), "001") + "|";
    }

    private String generateType02(Company c) {
        return String.join("|", "02", "001", "4", "999999999") + "|";
    }

    private String generateType03(String bondId, Employee e) {
        return String.join("|", "03", bondId, formatOnlyNumbers(e.cpf()), formatText(e.fullName(), 150),
                e.phone() != null ? formatOnlyNumbers(e.phone()) : "") + "|";
    }

    // *** ATUALIZADO PARA PRODUÇÃO ***
    private String generateType04(String scheduleId, Employee e) {
        // Formato: 04|CodHorario|DuracaoMin|Entrada|SaidaAlmoco|VoltaAlmoco|Saida

        LocalTime start = e.workStartTime() != null ? e.workStartTime() : LocalTime.of(8, 0);
        LocalTime end = e.workEndTime() != null ? e.workEndTime() : LocalTime.of(17, 0);
        LocalTime breakStart = e.breakStartTime() != null ? e.breakStartTime() : LocalTime.of(12, 0);
        LocalTime breakEnd = e.breakEndTime() != null ? e.breakEndTime() : LocalTime.of(13, 0);

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

        if (r.startWork() != null) {
            String source = "O";
            // Lógica de Detecção de Edição (Fonte "I" se editado e diferente do original)
            if (r.edited() && r.originalStartWork() != null && !r.startWork().equals(r.originalStartWork())) {
                source = "I";
            }
            // Se não tiver original (inserido manualmente via sistema), também é "I"
            if (r.originalStartWork() == null) {
                source = "I";
            }

            lines.add(String.join("|", "05", bondId, formatDateTimeIso(r.startWork()),
                    "001", "E", "", source, scheduleId, "") + "|");
        }

        if (r.endWork() != null) {
            String source = "O";
            if (r.edited() && r.originalEndWork() != null && !r.endWork().equals(r.originalEndWork())) {
                source = "I";
            }
            if (r.originalEndWork() == null) {
                source = "I";
            }

            lines.add(String.join("|", "05", bondId, formatDateTimeIso(r.endWork()),
                    "001", "S", "", source, scheduleId, "") + "|");
        }
        return lines;
    }

    private String generateType07(String bondId, TimeRecord r) {
        String type = "05";
        if (r.statusRecord() == StatusRecord.VACATION) type = "04";

        long minutes = 480; // Padrão caso falte dados
        if (r.startWork() != null && r.endWork() != null) {
            minutes = java.time.Duration.between(r.startWork(), r.endWork()).toMinutes();
        }

        return String.join("|", "07", bondId, type,
                r.startWork().toLocalDate().format(DATE_FMT), String.valueOf(minutes), "") + "|";
    }

    private String generateType08(Company c) {
        return String.join("|", "08", "KRONOS SYSTEM", SOFTWARE_VERSION, "1", "00000000000000", DEV_NAME, "suporte@kronos.com.br") + "|";
    }

    private boolean isAbsence(TimeRecord r) {
        return r.statusRecord() == StatusRecord.VACATION || r.statusRecord() == StatusRecord.TIME_OFF || r.statusRecord() == StatusRecord.ABSENCE;
    }

    private String formatOnlyNumbers(String s) { return s == null ? "" : s.replaceAll("\\D", ""); }
    private String formatText(String s, int max) { if (s == null) return ""; return s.length() > max ? s.substring(0, max) : s; }
    private String formatDateTimeIso(LocalDateTime dt) { return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + "-0300"; }
}