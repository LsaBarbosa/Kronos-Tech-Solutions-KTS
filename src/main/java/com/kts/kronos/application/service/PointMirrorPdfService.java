package com.kts.kronos.application.service;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.PointMirrorPdfUseCase;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import com.kts.kronos.observability.support.ObservabilityDefaults;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.Messages.DATE_FMT_BR;
import static com.kts.kronos.constants.Messages.TIME_FORMATTER;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointMirrorPdfService implements PointMirrorPdfUseCase {

    private final CompanyProvider companyProvider;
    private final TimeRecordProvider recordRepository;
    private final DomainAuthorizationService domainAuthorizationService;
    private final KronosMetrics kronosMetrics;
    private final KronosTracing kronosTracing;



    @Override
    @Transactional(readOnly = true)
    public byte[] generateMirror(UUID employeeId, LocalDate startDate, LocalDate endDate) {
        return generateMirrorInternal(employeeId, startDate, endDate, null);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateMirrorWithSignatureStamp(
            UUID employeeId,
            LocalDate startDate,
            LocalDate endDate,
            SignatureStamp stamp
    ) {
        return generateMirrorInternal(employeeId, startDate, endDate, stamp);
    }

    private byte[] generateMirrorInternal(UUID employeeId, LocalDate startDate, LocalDate endDate, SignatureStamp stamp) {
        long startedAt = System.nanoTime();
        long totalDays = LegalExportRangeGuard.validate(startDate, endDate);
        var employee = domainAuthorizationService.authorizeEmployeeAccess(employeeId);
        var company = companyProvider.findById(employee.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));

        try {
            byte[] pdfBytes = tracing().observe("kronos.legal.point_mirror", () -> {
                try (var baos = new ByteArrayOutputStream()) {
                    var writer = new PdfWriter(baos);
                    var pdf = new PdfDocument(writer);
                    var document = new Document(pdf);
                    document.setMargins(20, 20, 20, 20);

                    document.add(new Paragraph("ESPELHO DE PONTO ELETRÔNICO")
                            .setBold().setFontSize(16).setTextAlignment(TextAlignment.CENTER));
                    document.add(new Paragraph("Período: " + startDate.format(DATE_FMT_BR) + " a " + endDate.format(DATE_FMT_BR))
                            .setFontSize(10).setTextAlignment(TextAlignment.CENTER));

                    addEmployeeHeader(document, company, employee);

                    var table = new Table(UnitValue.createPercentArray(new float[]{3, 3, 5, 5, 3, 3}));
                    table.setWidth(UnitValue.createPercentValue(100));

                    addCellHeader(table, "DATA");
                    addCellHeader(table, "JORNADA");
                    addCellHeader(table, "MARC. ORIGINAIS");
                    addCellHeader(table, "MARC. TRATADAS");
                    addCellHeader(table, "TRABALHADO");
                    addCellHeader(table, "SALDO");

                    var totalBalance = Duration.ZERO;
                    var totalWorked = Duration.ZERO;

                    var recordsByDay = recordRepository.findByRange(
                                    employee.employeeId(),
                                    startDate.atStartOfDay(),
                                    endDate.atTime(23, 59, 59)
                            ).stream()
                            .filter(r -> r.startWork() != null)
                            .sorted(Comparator.comparing(TimeRecord::startWork))
                            .collect(Collectors.groupingBy(
                                    r -> r.startWork().toLocalDate(),
                                    TreeMap::new,
                                    Collectors.toList()
                            ));

                    for (var date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                        List<TimeRecord> dailyRecords = recordsByDay.getOrDefault(date, List.of());
                        var dayData = processDay(date, dailyRecords, employee);

                        totalWorked = totalWorked.plus(dayData.worked);
                        totalBalance = totalBalance.plus(dayData.balance);

                        addCell(table, date.format(DateTimeFormatter.ofPattern("dd/MM (EEE)")));
                        addCell(table, dayData.jornadaDisplay);
                        addCell(table, dayData.originalMarks);
                        addCell(table, dayData.treatedMarks);
                        addCell(table, formatDuration(dayData.worked));
                        addCell(table, formatBalance(dayData.balance));
                    }

                    document.add(table);
                    document.add(new Paragraph("\nRESUMO DO PERÍODO").setBold());
                    document.add(new Paragraph("Total Trabalhado: " + formatDuration(totalWorked)));
                    document.add(new Paragraph("Saldo do Período: " + formatBalance(totalBalance)));
                    addSignatures(document, employee.fullName());

                    if (stamp != null) {
                        addElectronicSignatureStamp(document, stamp);
                    }

                    document.close();
                    return baos.toByteArray();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });

            metrics().legalSuccess("point_mirror");
            metrics().recordLegalDuration("point_mirror", Duration.ofNanos(System.nanoTime() - startedAt), "success");
            log.info("event=legal_point_mirror_generation result=success");
            return pdfBytes;
        } catch (RuntimeException e) {
            metrics().legalFailure("point_mirror", "generation");
            metrics().recordLegalDuration("point_mirror", Duration.ofNanos(System.nanoTime() - startedAt), "failure");
            log.error("event=legal_point_mirror_generation result=failure reason=generation exception_type={}",
                    e.getClass().getSimpleName());
            throw new RuntimeException("Erro na geração do PDF", e);
        }
    }

    // --- MÉTODOS AUXILIARES ---

    private void addEmployeeHeader(Document doc, Company c, Employee e) {
        var header = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        header.setWidth(UnitValue.createPercentValue(100));
        header.setMarginBottom(10);

        header.addCell(new Cell().add(new Paragraph("EMPREGADOR: " + c.name() + "\nCNPJ: " + c.cnpj())).setBorder(null));
        header.addCell(new Cell().add(new Paragraph("FUNCIONÁRIO: " + e.fullName() + "\nCPF: " + e.cpf())).setBorder(null).setTextAlignment(TextAlignment.RIGHT));

        doc.add(header);
    }

    private void addElectronicSignatureStamp(Document doc, SignatureStamp stamp) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss z", new Locale("pt", "BR"));
        String when = stamp.signedAt().atZone(ZoneId.of("America/Sao_Paulo")).format(fmt);
        String recordsHashShort = stamp.recordsSnapshotHashSha256() != null && stamp.recordsSnapshotHashSha256().length() >= 16
                ? stamp.recordsSnapshotHashSha256().substring(0, 16) + "…"
                : "—";

        var stampTable = new Table(UnitValue.createPercentArray(new float[]{1}));
        stampTable.setWidth(UnitValue.createPercentValue(100));
        stampTable.setMarginTop(20);

        var content = new Paragraph()
                .add(new com.itextpdf.layout.element.Text("ASSINATURA ELETRÔNICA REGISTRADA\n").setBold().setFontSize(11))
                .add(new com.itextpdf.layout.element.Text("Tipo: Eletrônica avançada interna (Lei 14.063/2020) — Não-ICP-Brasil.\n").setFontSize(8))
                .add(new com.itextpdf.layout.element.Text("Signatário (ciência): " + stamp.signerFullName() + "\n").setFontSize(9))
                .add(new com.itextpdf.layout.element.Text("Data/Hora da ciência: " + when + "\n").setFontSize(9))
                .add(new com.itextpdf.layout.element.Text("Declaração: versão " + stamp.declarationVersion() + "\n").setFontSize(9))
                .add(new com.itextpdf.layout.element.Text("Hash canônico dos registros (SHA-256, prefixo): " + recordsHashShort + "\n").setFontSize(8))
                .add(new com.itextpdf.layout.element.Text("Este documento também é assinado digitalmente pelo certificado da empresa (PAdES/PKCS#7) — verifique no Adobe Reader.").setFontSize(8).setItalic());

        var cell = new Cell()
                .add(content)
                .setBackgroundColor(new DeviceRgb(245, 240, 255))
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(new DeviceRgb(124, 58, 237), 1.0f))
                .setPadding(8);
        stampTable.addCell(cell);
        doc.add(stampTable);
    }

    private void addSignatures(Document doc, String empName) {
        Paragraph p = new Paragraph("\n\n\n");
        p.add("____________________________________            ____________________________________\n");
        p.add("        " + empName + "                                    Gestor Responsável\n");
        p.add("        (Funcionário)                                      (Empresa)");
        p.setTextAlignment(TextAlignment.CENTER);
        doc.add(p);
    }

    private void addCellHeader(Table table, String text) {
        table.addHeaderCell(new Cell().add(new Paragraph(text).setBold().setFontSize(9))
                .setBackgroundColor(new DeviceRgb(220, 220, 220)).setTextAlignment(TextAlignment.CENTER));
    }

    private void addCell(Table table, String text) {
        table.addCell(new Cell().add(new Paragraph(text).setFontSize(9)).setTextAlignment(TextAlignment.CENTER));
    }

    // DTO Interno para organizar os dados processados do dia
    private record ProcessedDay(
            String jornadaDisplay,
            String originalMarks,
            String treatedMarks,
            Duration worked,
            Duration balance
    ) {}

    /**
     * Lógica "Real" de Processamento Diário
     */
    private ProcessedDay processDay(LocalDate date, List<TimeRecord> records, Employee employee) {
        var originalSb = new StringBuilder();
        var treatedSb = new StringBuilder();
        var worked = Duration.ZERO;

        // 1. Determina a expectativa de trabalho para este dia específico
        long expectedMinutes = employee.getDailyWorkMinutes(); // Método real do Employee
        boolean isWeekend = (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY);

        // Em produção, se for FDS, a expectativa padrão é zero (hora extra 100% se trabalhar)
        if (isWeekend) {
            expectedMinutes = 0;
        }

        var expected = Duration.ofMinutes(expectedMinutes);

        // 2. Processa as marcações
        for (var r : records) {
            // Formata Original
            if (r.originalStartWork() != null) originalSb.append(r.originalStartWork().format(TIME_FORMATTER)).append("E ");
            if (r.originalEndWork() != null) originalSb.append(r.originalEndWork().format(TIME_FORMATTER)).append("S ");

            // Formata Tratado
            if (r.startWork() != null) treatedSb.append(r.startWork().format(TIME_FORMATTER)).append("E ");
            if (r.endWork() != null) {
                treatedSb.append(r.endWork().format(TIME_FORMATTER)).append("S ");

                // Soma horas trabalhadas (ignora pausas implícitas no cálculo de 'trabalhado')
                if (r.statusRecord() != StatusRecord.IMPLICIT_BREAK) {
                    worked = worked.plus(Duration.between(r.startWork(), r.endWork()));
                }
            }
        }

        // 3. Calcula Saldo
        var balance = worked.minus(expected);

        // 4. Define texto de exibição da Jornada
        String jornadaDisplay;
        if (expectedMinutes > 0) {
            // Exibe horário contratual (Ex: 08:00 - 17:00)
            var start = employee.workStartTime() != null ? employee.workStartTime() : LocalTime.of(8,0);
            var end = employee.workEndTime() != null ? employee.workEndTime() : LocalTime.of(17,0);
            jornadaDisplay = start.format(TIME_FORMATTER) + " - " + end.format(TIME_FORMATTER);
        } else {
            jornadaDisplay = "FOLGA / DSR";
        }

        // 5. Ajustes Visuais para dias sem marcação
        if (records.isEmpty()) {
            if (expectedMinutes > 0) {
                // Dia útil sem marcação = FALTA (Saldo Negativo)
                treatedSb.append("FALTA");
            } else {
                // Fim de semana sem marcação = FOLGA (Saldo Zero)
                treatedSb.append("-");
                balance = Duration.ZERO;
            }
        }

        // 6. Tratamento para Abonos/Férias
        boolean isAbono = records.stream().anyMatch(r -> r.statusRecord() == StatusRecord.TIME_OFF);
        boolean isFerias = records.stream().anyMatch(r -> r.statusRecord() == StatusRecord.VACATION);

        if (isAbono) {
            treatedSb = new StringBuilder("TIME_OFF_REQUEST");
            balance = Duration.ZERO; // Abono zera o débito
        } else if (isFerias) {
            treatedSb = new StringBuilder("FÉRIAS");
            balance = Duration.ZERO;
        }

        return new ProcessedDay(jornadaDisplay, originalSb.toString(), treatedSb.toString(), worked, balance);
    }

    private String formatDuration(Duration d) {
        long hours = d.toHours();
        long minutes = d.toMinutesPart();
        return String.format("%02d:%02d", hours, Math.abs(minutes));
    }

    private String formatBalance(Duration d) {
        var sign = d.isNegative() ? "-" : "+";
        return sign + formatDuration(d.abs());
    }

    private KronosMetrics metrics() {
        return kronosMetrics != null ? kronosMetrics : ObservabilityDefaults.metrics();
    }

    private KronosTracing tracing() {
        return kronosTracing != null ? kronosTracing : ObservabilityDefaults.tracing();
    }
}
