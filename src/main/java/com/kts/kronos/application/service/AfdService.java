package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.out.provider.AfdEntryProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.domain.model.AfdEntry;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AfdService implements AdfUseCase {


    private final AfdEntryProvider afdProvider;
    private final CompanyProvider companyProvider;
    @Autowired
    private KronosMetrics kronosMetrics = new KronosMetrics();
    @Autowired
    private KronosTracing kronosTracing = new KronosTracing();

    // Removemos dados hardcoded e usamos configuração
    @Value("${kronos.legal.inpi-number:999999999}")
    private String inpiNumber;

    @Override
    @Transactional
    public void logMarking(Company company, Employee employee, LocalDateTime date, Long nsr) {
        try {
            var previousHash = afdProvider.findLastHashByCompanyId(company.companyId())
                    .orElse(null);

            var rawData = String.format("%09d", nsr) +
                    "7" +
                    date.format(AFD_DATE_FMT) +
                    formatCpf(employee.cpf()) +
                    (previousHash != null ? previousHash : "");

            var currentHash = calculateSha256(rawData);

            var entry = new AfdEntry(
                    nsr, "7", date, employee.cpf(), employee.phone(),
                    company.companyId(), employee.employeeId(), previousHash, currentHash
            );

            afdProvider.save(entry);

            log.info("event=legal_afd_marking result=success");
        } catch (RuntimeException e) {
            log.error("event=legal_afd_marking result=failure reason=persistence exception_type={}",
                    e.getClass().getSimpleName());
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true) // ReadOnly true é vital para performance do Stream no Postgres
    public void writeAfdToStream(UUID companyId, OutputStream outputStream) {
        long startedAt = System.nanoTime();
        var company = companyProvider.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));
        try {
            kronosTracing.observe("kronos.legal.afd.generate", () -> {
                try (var writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8)) {
                    var header = String.format("0000000011%s%s%s",
                            "1",
                            formatString(company.cnpj(), 14),
                            formatString(company.name(), 150)
                    );
                    writer.print(header + "\r\n");

                    var recordCounter = new AtomicLong(0);

                    try (Stream<AfdEntry> stream = afdProvider.streamByCompanyIdOrderByNsr(companyId)) {
                        stream.forEach(entry -> {
                            var line = formatType7(entry);
                            writer.print(line + "\r\n");
                            recordCounter.incrementAndGet();
                        });
                    }

                    long totalRegistros = recordCounter.get();
                    var trailer = String.format("999999999%09d", totalRegistros);
                    writer.print(trailer);
                    writer.flush();
                }
            });

            kronosMetrics.legalSuccess("afd");
            kronosMetrics.recordLegalDuration("afd", Duration.ofNanos(System.nanoTime() - startedAt), "success");
            log.info("event=legal_afd_generation result=success");
        } catch (RuntimeException e) {
            kronosMetrics.legalFailure("afd", "generation");
            kronosMetrics.recordLegalDuration("afd", Duration.ofNanos(System.nanoTime() - startedAt), "failure");
            log.error("event=legal_afd_generation result=failure reason=generation exception_type={}",
                    e.getClass().getSimpleName());
            throw new RuntimeException(FAILURE_TO_GENERATE_AFD, e);
        }
    }

    private String formatType7(AfdEntry e) {
        // Layout Tipo 7 (Marcação):
        // NSR(9) | Tipo(1) | DataHora(12) | CPF(12) | CRC/Hash(Variavel)
        return String.format("%09d%s%s%s",
                e.nsr(),
                e.recordType(),
                e.recordDate().format(AFD_DATE_FMT),
                formatNumeric(formatCpf(e.employeeCpf()), 12)
        );
    }

    private String formatNumeric(String s, int size) {
        if (s == null) s = "";
        // Preenchimento com ZEROS à esquerda para números (CPF, PIS)
        if (s.length() > size) return s.substring(0, size);
        return String.format("%" + size + "s", s).replace(' ', '0');
    }

    private String formatString(String s, int size) {
        if (s == null) s = "";
        // Preenchimento com ESPAÇOS à direita para texto (Nome, Razão Social)
        if (s.length() > size) return s.substring(0, size);
        return String.format("%-" + size + "s", s);
    }

    private String formatCpf(String cpf) {
        return cpf != null ? cpf.replaceAll("\\D", "") : "00000000000";
    }

    private String calculateSha256(String data) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            var hexString = new StringBuilder();
            for (byte b : hash) {
                var hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(ERROR_TO_GENERATE_HASH, e);
        }
    }
}
