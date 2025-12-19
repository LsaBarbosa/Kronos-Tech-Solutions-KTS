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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class AfdService implements AdfUseCase {

    private final AfdEntryProvider afdProvider;
    private final CompanyProvider companyProvider; // Necessário para cabeçalho

    // TODO: Mover para arquivo de configuração (application.yml)
    private static final String INPI_NUMBER = "999999999";
    private static final DateTimeFormatter AFD_DATE_FMT = DateTimeFormatter.ofPattern("ddMMyyyyHHmm");

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void logMarking(Company company, Employee employee, LocalDateTime date, Long nsr) {
        String previousHash = afdProvider.findLastHashByCompanyId(company.companyId())
                .orElse(null);

        // Formatação simples para hash (ajustar conforme rigor técnico da Portaria 671 Anexo V)
        String rawData = String.format("%09d", nsr) +
                "7" +
                date.format(AFD_DATE_FMT) +
                formatCpf(employee.cpf()) +
                (previousHash != null ? previousHash : "");

        String currentHash = calculateSha256(rawData);

        AfdEntry entry = new AfdEntry(
                nsr, "7", date, employee.cpf(), employee.phone(), // phone placeholder p/ pis
                company.companyId(), employee.employeeId(), previousHash, currentHash
        );

        afdProvider.save(entry);
    }

    /**
     * Gera o arquivo AFD escrevendo diretamente no OutputStream para evitar OutOfMemory.
     * @param companyId ID da empresa
     * @param outputStream Stream de saída (ex: ServletResponse)
     */

    @Override
    @Transactional(readOnly = true) // Obrigatório para manter o Stream do banco aberto
    public void writeAfdToStream(UUID companyId, OutputStream outputStream) {
        Company company = companyProvider.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));

        try (PrintWriter writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8)) {

            // 1. Cabeçalho (Registro Tipo 000000001)
            // Nota: O layout padrão exige Cabeçalho + Registros + Trailer.
            String header = String.format("0000000011%s%s%s",
                    "1", // 1=CNPJ, 2=CPF
                    formatString(company.cnpj(), 14),
                    formatString(company.name(), 150)
            );
            writer.print(header + "\r\n");

            // --- CONTADOR ATÔMICO PARA O TRAILER ---
            AtomicLong recordCounter = new AtomicLong(0);

            // 2. Registros (Stream do banco)
            try (Stream<AfdEntry> stream = afdProvider.streamByCompanyIdOrderByNsr(companyId)) {
                stream.forEach(entry -> {
                    String line = formatType7(entry);
                    writer.print(line + "\r\n");

                    // Incrementa o contador a cada linha escrita
                    recordCounter.incrementAndGet();
                });
            }

            // 3. Trailer (Registro Tipo 999999999)
            // Formato: "999999999" + Quantidade de Registros Tipo 7 (9 dígitos com zeros à esquerda)
            long totalRegistros = recordCounter.get();
            String trailer = String.format("999999999%09d", totalRegistros);

            writer.print(trailer); // Trailer geralmente é a última linha, sem \r\n final obrigatório

            writer.flush();
        } catch (Exception e) {
            log.error("Erro ao gerar AFD", e);
            throw new RuntimeException("Falha na geração do arquivo AFD", e);
        }
    }

    private String formatType7(AfdEntry e) {
        // Layout Tipo 7: NSR(9) | Tipo(1) | DataHora(12) | CPF(12) | ...
        return String.format("%09d%s%s%s",
                e.nsr(),
                e.recordType(),
                e.recordDate().format(AFD_DATE_FMT),
                formatNumeric(formatCpf(e.employeeCpf()), 12) // CORREÇÃO: Zero Padding
        );
    }

    private String formatNumeric(String s, int size) {
        if (s == null) s = "";
        // Se a string for maior que o tamanho, corta. Se menor, preenche com 0 à esquerda.
        if (s.length() > size) return s.substring(0, size);
        return String.format("%" + size + "s", s).replace(' ', '0');
    }

    private String calculateSha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String formatString(String s, int size) {
        if (s == null) s = "";
        if (s.length() > size) return s.substring(0, size);
        return String.format("%-" + size + "s", s);
    }

    private String formatCpf(String cpf) {
        return cpf != null ? cpf.replaceAll("\\D", "") : "00000000000";
    }
}