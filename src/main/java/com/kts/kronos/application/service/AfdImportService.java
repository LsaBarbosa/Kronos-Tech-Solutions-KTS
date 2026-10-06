package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.afd.AfdImportConfirmResponse;
import com.kts.kronos.adapter.in.web.dto.afd.AfdImportPreviewResponse;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AfdImportUseCase;
import com.kts.kronos.application.port.out.provider.AfdEntryProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.service.afd.AfdParser;
import com.kts.kronos.adapter.out.persistence.AfdEntryRepository;
import com.kts.kronos.adapter.out.persistence.TimeRecordRepository;
import com.kts.kronos.domain.model.AfdEntry;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.COMPANY_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class AfdImportService implements AfdImportUseCase {

    private static final ZoneId ZONE_BR = ZoneId.of("America/Sao_Paulo");
    private static final String IMPORT_HASH_MARKER = "IMPORTED";

    private final AfdParser afdParser;
    private final CompanyProvider companyProvider;
    private final EmployeeProvider employeeProvider;
    private final TimeRecordProvider timeRecordProvider;
    private final AfdEntryProvider afdEntryProvider;
    private final AfdEntryRepository afdEntryRepository;
    private final TimeRecordRepository timeRecordRepository;
    private final DomainAuthorizationService domainAuthorizationService;

    @Override
    @Transactional(readOnly = true)
    public AfdImportPreviewResponse preview(InputStream inputStream) throws IOException {
        var parseResult = afdParser.parse(inputStream);
        var header = parseResult.header();

        var company = companyProvider.findByCnpj(header.cnpj())
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND + header.cnpj()));

        UUID companyId = domainAuthorizationService.authorizeCompanyAccess(company.companyId());

        var marks = parseResult.marks();
        int totalMarks = marks.size();
        int alreadyImported = 0;
        int toImport = 0;

        for (var mark : marks) {
            Long nsr = parseNsr(mark.nsr());
            if (nsr != null && afdEntryRepository.existsByCompanyIdAndNsr(companyId, nsr)) {
                alreadyImported++;
            } else {
                toImport++;
            }
        }

        // Resolve employees by PIS
        var uniquePis = marks.stream().map(AfdParser.AfdMark::pis).distinct().toList();
        List<String> notFoundPisList = new ArrayList<>();
        int employeesFound = 0;
        int employeesNotFound = 0;

        for (String pis : uniquePis) {
            Optional<Employee> emp = findEmployeeByPisOrCpf(companyId, pis);
            if (emp.isPresent()) {
                employeesFound++;
            } else {
                employeesNotFound++;
                notFoundPisList.add(pis);
            }
        }

        // Count open records (unpaired marks per PIS)
        int openRecords = countOpenRecords(marks);

        // Period
        String periodoInicio = marks.stream()
                .map(AfdParser.AfdMark::markDatetime)
                .min(Comparator.naturalOrder())
                .map(dt -> dt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .orElse(null);
        String periodoFim = marks.stream()
                .map(AfdParser.AfdMark::markDatetime)
                .max(Comparator.naturalOrder())
                .map(dt -> dt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                .orElse(null);

        return new AfdImportPreviewResponse(
                company.name(),
                header.cnpj(),
                header.razaoSocial(),
                periodoInicio,
                periodoFim,
                totalMarks,
                alreadyImported,
                toImport,
                employeesFound,
                employeesNotFound,
                notFoundPisList,
                openRecords,
                parseResult.skippedLines()
        );
    }

    @Override
    @Transactional
    public AfdImportConfirmResponse confirm(InputStream inputStream) throws IOException {
        var parseResult = afdParser.parse(inputStream);
        var header = parseResult.header();

        var company = companyProvider.findByCnpj(header.cnpj())
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND + header.cnpj()));

        UUID companyId = domainAuthorizationService.authorizeCompanyAccess(company.companyId());

        var marks = parseResult.marks();

        // Group marks by PIS, sorted by datetime
        Map<String, List<AfdParser.AfdMark>> marksByPis = new LinkedHashMap<>();
        for (var mark : marks) {
            marksByPis.computeIfAbsent(mark.pis(), k -> new ArrayList<>()).add(mark);
        }

        int timeRecordsCreated = 0;
        int timeRecordsDuplicated = 0;
        int afdEntriesSaved = 0;
        int afdEntriesDuplicated = 0;
        List<String> notLinkedPisList = new ArrayList<>();

        for (Map.Entry<String, List<AfdParser.AfdMark>> entry : marksByPis.entrySet()) {
            String pis = entry.getKey();
            List<AfdParser.AfdMark> pisMarks = new ArrayList<>(entry.getValue());
            pisMarks.sort(Comparator.comparing(AfdParser.AfdMark::markDatetime));

            Optional<Employee> empOpt = findEmployeeByPisOrCpf(companyId, pis);

            if (empOpt.isEmpty()) {
                notLinkedPisList.add(pis);
                // Cannot save AfdEntry without a valid employee_id (DB constraint)
                log.warn("AFD import: PIS {} não encontrado na empresa {}, marcações ignoradas", pis, companyId);
                continue;
            }

            Employee emp = empOpt.get();
            UUID empId = emp.employeeId();

            // Pair marks: checkin + optional checkout
            int i = 0;
            while (i < pisMarks.size()) {
                AfdParser.AfdMark checkinMark = pisMarks.get(i);
                AfdParser.AfdMark checkoutMark = (i + 1 < pisMarks.size()) ? pisMarks.get(i + 1) : null;

                LocalDateTime startWork = toLocalDateTime(checkinMark.markDatetime());
                LocalDateTime endWork = checkoutMark != null ? toLocalDateTime(checkoutMark.markDatetime()) : null;

                // Idempotency check for TimeRecord
                if (!timeRecordRepository.existsByEmployeeIdAndStartWork(empId, startWork)) {
                    TimeRecord tr = new TimeRecord(
                            null,
                            startWork,
                            endWork,
                            StatusRecord.IMPORTED,
                            false,
                            true,
                            empId,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null
                    );
                    timeRecordProvider.save(tr);
                    timeRecordsCreated++;
                } else {
                    timeRecordsDuplicated++;
                }

                // Save AfdEntries for linked employees
                int r1 = saveAfdEntry(checkinMark, companyId, emp);
                if (r1 > 0) afdEntriesSaved++;
                else afdEntriesDuplicated++;

                if (checkoutMark != null) {
                    int r2 = saveAfdEntry(checkoutMark, companyId, emp);
                    if (r2 > 0) afdEntriesSaved++;
                    else afdEntriesDuplicated++;
                    i += 2;
                } else {
                    i += 1;
                }
            }
        }

        int employeesNotLinked = notLinkedPisList.size();

        return new AfdImportConfirmResponse(
                timeRecordsCreated,
                timeRecordsDuplicated,
                afdEntriesSaved,
                afdEntriesDuplicated,
                employeesNotLinked,
                notLinkedPisList
        );
    }

    private int saveAfdEntry(AfdParser.AfdMark mark, UUID companyId, Employee emp) {
        Long nsr = parseNsr(mark.nsr());
        if (nsr == null) {
            return 0;
        }
        if (afdEntryRepository.existsByCompanyIdAndNsr(companyId, nsr)) {
            return 0;
        }
        try {
            AfdEntry entry = new AfdEntry(
                    nsr,
                    "7",
                    toLocalDateTime(mark.markDatetime()),
                    emp.cpf() != null ? emp.cpf() : "",
                    emp.pis() != null ? emp.pis() : "",
                    companyId,
                    emp.employeeId(),
                    null,
                    IMPORT_HASH_MARKER
            );
            afdEntryProvider.save(entry);
            return 1;
        } catch (DataIntegrityViolationException e) {
            log.debug("AFD entry (companyId={}, nsr={}) já existe — ignorando duplicata", companyId, nsr);
            return 0;
        }
    }

    private Optional<Employee> findEmployeeByPisOrCpf(UUID companyId, String pis) {
        if (pis == null || pis.isBlank()) {
            return Optional.empty();
        }
        Optional<Employee> emp = employeeProvider.findByCompanyIdAndPis(companyId, pis);
        if (emp.isPresent()) {
            return emp;
        }
        // Fallback: try PIS value as CPF
        return employeeProvider.findByCompanyIdAndCpf(companyId, pis);
    }

    private int countOpenRecords(List<AfdParser.AfdMark> marks) {
        Map<String, List<AfdParser.AfdMark>> byPis = new LinkedHashMap<>();
        for (var mark : marks) {
            byPis.computeIfAbsent(mark.pis(), k -> new ArrayList<>()).add(mark);
        }
        int open = 0;
        for (var pisMarks : byPis.values()) {
            pisMarks.sort(Comparator.comparing(AfdParser.AfdMark::markDatetime));
            int i = 0;
            while (i < pisMarks.size()) {
                if (i + 1 < pisMarks.size()) {
                    i += 2;
                } else {
                    open++;
                    i += 1;
                }
            }
        }
        return open;
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime odt) {
        return odt.atZoneSameInstant(ZONE_BR).toLocalDateTime();
    }

    private Long parseNsr(String nsr) {
        if (nsr == null || nsr.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(nsr.trim());
        } catch (NumberFormatException e) {
            log.warn("NSR inválido: '{}'", nsr);
            return null;
        }
    }
}
