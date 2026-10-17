package com.kts.kronos.application.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.kts.kronos.adapter.in.web.dto.timerecord.ManagerMonthlyAlertItemResponse;
import com.kts.kronos.adapter.in.web.dto.timerecord.ManagerMonthlyAlertResponse;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ManagerMonthlyAlertsPdfService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generate(ManagerMonthlyAlertResponse response) {
        try (var output = new ByteArrayOutputStream()) {
            var pdf = new PdfDocument(new PdfWriter(output));
            var document = new Document(pdf);

            document.add(new Paragraph("Alertas mensais de ponto").setBold().setFontSize(16));
            document.add(new Paragraph("Mês: " + response.month() + " | Filtro: " + filterLabel(response.filter())));
            document.add(new Paragraph("Total de ocorrências: " + response.items().size()).setFontSize(9));

            var table = new Table(UnitValue.createPercentArray(new float[]{45, 25, 30}))
                    .useAllAvailableWidth();
            table.addHeaderCell(new Cell().add(new Paragraph("Colaborador").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Data").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Ocorrência").setBold()));

            for (var item : response.items()) {
                table.addCell(item.employeeName());
                table.addCell(formatDate(item.date()));
                table.addCell(item.value());
            }
            document.add(table);

            if (response.items().isEmpty()) {
                document.add(new Paragraph("Nenhuma ocorrência encontrada para o filtro selecionado."));
            }

            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar o PDF de alertas mensais.", exception);
        }
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : DATE_FORMAT.format(date);
    }

    private String filterLabel(String filter) {
        return switch (filter) {
            case "PENDING_EXIT" -> "Saída pendente";
            case "OVERTIME" -> "Hora extra acima de 5 minutos";
            case "NEGATIVE_BALANCE" -> "Saldo de horas negativo";
            case "LATE" -> "Atraso na entrada";
            case "SHORT_BREAK" -> "Pausa inferior a 1 hora";
            case "ABSENCE" -> "Faltas";
            default -> filter;
        };
    }
}
