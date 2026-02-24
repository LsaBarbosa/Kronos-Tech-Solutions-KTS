package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.out.provider.AfdEntryProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.domain.model.AfdEntry;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.springframework.transaction.annotation.Transactional;

import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AfdService implements AdfUseCase {


    private final AfdEntryProvider afdProvider;
    private final CompanyProvider companyProvider;

    // Removemos dados hardcoded e usamos configuração
    @Value("${kronos.legal.inpi-number:999999999}")
    private String inpiNumber;

    @Override
    @Transactional // Mantém transação para garantir integridade do HASH
    public void logMarking(Company company, Employee employee, LocalDateTime date, Long nsr) {
        // 1. Busca o Hash do registro anterior para garantir o encadeamento (Blockchain style)
        var previousHash = afdProvider.findLastHashByCompanyId(company.companyId())
                .orElse(null); // Se for null, é o primeiro registro da empresa

        // 2. Monta a linha crua conforme layout para cálculo do hash
        // Layout simplificado: NSR + Tipo + DataHora + CPF + HashAnterior
        var rawData = String.format("%09d", nsr) +
                "7" + // Tipo 7 = Marcação de Ponto
                date.format(AFD_DATE_FMT) +
                formatCpf(employee.cpf()) +
                (previousHash != null ? previousHash : ""); // Hash anterior faz parte do novo hash

        var currentHash = calculateSha256(rawData);

        // 3. Persiste o registro de auditoria
        var entry = new AfdEntry(
                nsr, "7", date, employee.cpf(), employee.phone(),
                company.companyId(), employee.employeeId(), previousHash, currentHash
        );

        afdProvider.save(entry);
        log.debug("AFD registrado. NSR: {}, Hash: {}", nsr, currentHash);
    }

    @Override
    @Transactional(readOnly = true) // ReadOnly true é vital para performance do Stream no Postgres
    public void writeAfdToStream(UUID companyId, OutputStream outputStream) {
        var company = companyProvider.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

        try (var writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8)) {

            // 1. Cabeçalho (Registro Tipo 1)
            // Identifica a empresa e o REP-P
            var header = String.format("0000000011%s%s%s",
                    "1", // 1=CNPJ
                    formatString(company.cnpj(), 14),
                    formatString(company.name(), 150)
            );
            writer.print(header + "\r\n"); // \r\n é o padrão Windows exigido por muitos validadores fiscais

            // 2. Registros (Stream do banco)
            // Usamos AtomicLong para contar os registros dentro do Stream (lambda)
            var recordCounter = new AtomicLong(0);

            try (Stream<AfdEntry> stream = afdProvider.streamByCompanyIdOrderByNsr(companyId)) {
                stream.forEach(entry -> {
                    var line = formatType7(entry);
                    writer.print(line + "\r\n");
                    recordCounter.incrementAndGet(); // Contabiliza +1
                });
            }

            // 3. Trailer (Registro Tipo 9)
            // Deve conter o total de registros do arquivo (Cabeçalho + Registros + Trailer não conta)
            // O padrão geralmente pede a quantidade de registros de dados (Tipo 7).
            long totalRegistros = recordCounter.get();

            // Formato: "999999999" + Quantidade (9 dígitos)
            var trailer = String.format("999999999%09d", totalRegistros);
            writer.print(trailer);
            // Trailer é a última linha, alguns validadores não exigem \r\n no final, mas é bom garantir flush.

            writer.flush();

        } catch (Exception e) {
            log.error("Erro ao gerar arquivo AFD", e);
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
        } catch (Exception e) {
            throw new RuntimeException(ERROR_TO_GENERATE_HASH, e);
        }
    }
}