package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.DigitalSignatureException;
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
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.Messages.*;
@Slf4j
@Service
@RequiredArgsConstructor
public class AejService implements AejUseCase {

    private final CompanyProvider companyProvider;
    private final EmployeeProvider employeeProvider;
    private final TimeRecordProvider recordRepository;
    private final DigitalSignatureService signatureService;
    private final KronosMetrics kronosMetrics;
    private final KronosTracing kronosTracing;

    @Value("${kronos.legal.inpi-number:999999999}")
    private String inpiNumber;

    @Value("${kronos.legal.dev-name:KRONOS TECH SOLUTIONS}")
    private String developerName;

    @Value("${kronos.legal.software-version:1.0}")
    private String softwareVersion;

    @Override
    @Transactional(readOnly = true)
    public void generateAej(UUID companyId, LocalDate startDate, LocalDate endDate, OutputStream outputStream) {
        long startedAt = System.nanoTime();
        final boolean[] signatureFailure = {false};
        long totalDays = LegalExportRangeGuard.validate(startDate, endDate);

        var company = companyProvider.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

        // Buffer em memória para montar o texto antes de assinar
        try {
            kronosTracing.observe("kronos.legal.aej", () -> {
                try (var textBuffer = new ByteArrayOutputStream();
                     var writer = new PrintWriter(textBuffer, true, StandardCharsets.ISO_8859_1)) {

                    writeLine(writer, generateType01(company, startDate, endDate));
                    writeLine(writer, generateType02());

                    List<Employee> employees = employeeProvider.findByCompanyId(company.companyId());

                    var employeeIds = employees.stream()
                            .map(Employee::employeeId)
                            .toList();

                    var recordsByEmployeeId = recordRepository.findByEmployeeIdsAndRange(
                                    employeeIds,
                                    startDate.atStartOfDay(),
                                    endDate.atTime(23, 59, 59)
                            ).stream()
                            .collect(Collectors.groupingBy(
                                    TimeRecord::employeeId,
                                    LinkedHashMap::new,
                                    Collectors.toList()
                            ));

                    int sequenceId = 1;

                    for (var employee : employees) {
                        var bondId = String.format("%09d", sequenceId++);
                        writeLine(writer, generateType03(bondId, employee));

                        var scheduleId = "H" + bondId;
                        writeLine(writer, generateType04(scheduleId, employee));

                        List<TimeRecord> records = recordsByEmployeeId.getOrDefault(employee.employeeId(), List.of());

                        for (var record : records) {
                            generateType05Lines(bondId, scheduleId, record)
                                    .forEach(line -> writeLine(writer, line));
                        }

                        for (var record : records) {
                            if (isAbsence(record)) {
                                writeLine(writer, generateType07(bondId, record));
                            }
                        }
                    }

                    writeLine(writer, generateType08());
                    writeLine(writer, "99|");
                    writer.flush();

                    byte[] originalContent = textBuffer.toByteArray();

                    byte[] signedContent;
                    try {
                        signedContent = signatureService.signData(originalContent);
                    } catch (DigitalSignatureException ex) {
                        signatureFailure[0] = true;
                        kronosMetrics.legalFailure("aej", "digital_signature");
                        kronosMetrics.recordLegalDuration("aej", Duration.ofNanos(System.nanoTime() - startedAt), "failure");
                        log.error("event=legal_aej_generation result=failure reason=digital_signature exception_type={}",
                                ex.getClass().getSimpleName());
                        throw ex;
                    } catch (RuntimeException ex) {
                        signatureFailure[0] = true;
                        kronosMetrics.legalFailure("aej", "digital_signature");
                        kronosMetrics.recordLegalDuration("aej", Duration.ofNanos(System.nanoTime() - startedAt), "failure");
                        log.error("event=legal_aej_generation result=failure reason=digital_signature_unexpected exception_type={}",
                                ex.getClass().getSimpleName());
                        throw new DigitalSignatureException("Falha ao assinar documento digitalmente.", ex);
                    }

                    outputStream.write(signedContent);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });

            kronosMetrics.legalSuccess("aej");
            kronosMetrics.recordLegalDuration("aej", Duration.ofNanos(System.nanoTime() - startedAt), "success");
            log.info("event=legal_aej_generation result=success");
        } catch (DigitalSignatureException e) {
            // Já foi medido/logado no catch interno; propaga preservando o tipo
            // para o RestExceptionHandler retornar 503/DIGITAL_SIGNATURE_UNAVAILABLE.
            throw e;
        } catch (RuntimeException e) {
            if (!signatureFailure[0]) {
                kronosMetrics.legalFailure("aej", "generation");
                kronosMetrics.recordLegalDuration("aej", Duration.ofNanos(System.nanoTime() - startedAt), "failure");
                log.error("event=legal_aej_generation result=failure reason=generation exception_type={}",
                        e.getClass().getSimpleName());
            }
            throw new RuntimeException(FAILURE_TO_GENERAT_AEJ, e);
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

        long minutes;
        // Se tem início e fim definidos (ex: meio período de folga), calcula.
        // Se é o dia todo, geralmente assume-se jornada diária padrão (ex: 480 min).
        if (r.startWork() != null && r.endWork() != null) {
            minutes = Duration.between(r.startWork(), r.endWork()).toMinutes();
        } else {
            minutes = 480; // Fallback para dia cheio (ajustar conforme regra de negócio)
        }

        // Resolve a data do evento sem NPE: prefere startWork, depois endWork,
        // depois recorre à data atual (mantém o arquivo válido, evita silenciar registro).
        LocalDate eventDate;
        if (r.startWork() != null) {
            eventDate = r.startWork().toLocalDate();
        } else if (r.endWork() != null) {
            eventDate = r.endWork().toLocalDate();
        } else {
            log.warn("event=legal_aej_generation reason=absence_without_timestamps status={} fallback=today",
                    r.statusRecord());
            eventDate = LocalDate.now();
        }

        return String.join("|", "07", bondId, type,
                eventDate.format(DATE_FMT), String.valueOf(minutes), "") + "|";
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
