package com.kts.kronos.application.service.demo;

import com.kts.kronos.adapter.out.persistence.*;
import com.kts.kronos.adapter.out.persistence.entity.*;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import com.kts.kronos.config.demo.DemoSandboxProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoSandboxPurgeService {

    private final CompanyRepository              companyRepo;
    private final UserRepository                 userRepo;
    private final EmployeeRepository             employeeRepo;
    private final UserCompanyAccessRepository    accessRepo;
    private final TimeRecordRepository           timeRecordRepo;
    private final TimeRecordApprovalRepository   approvalRepo;
    private final DocumentRepository             documentRepo;
    private final LegalConsentRepository         consentRepo;
    private final DemoSandboxSessionInvalidationService sessionInvalidation;
    private final DemoSandboxProperties          props;
    private final FaceRecognitionProvider        faceRecognitionProvider;
    private final FaceStorageProvider            faceStorageProvider;
    private final BucketStorageProvider          bucketStorageProvider;

    public record PurgeCounters(
            int companies, int users, int employees,
            int pointRecords, int approvals, int documents,
            int consents, int accesses, int files, int sessions
    ) {}

    /**
     * Idempotent purge: cleans everything related to the sandbox.
     * Safe to run even if sandbox is partially created or already purged.
     */
    @Transactional
    public PurgeCounters purgeAll() {
        int sessions  = sessionInvalidation.invalidateSandboxUserSession(props.getUsername());
        int companies = 0, users = 0, employees = 0;
        int pointRecords = 0, approvals = 0, documents = 0, consents = 0, accesses = 0;

        var companyOpt = companyRepo.findBySandboxKey(props.getSandboxKey());
        UUID companyId = null;

        if (companyOpt.isPresent()) {
            companyId = companyOpt.get().getId();
            List<EmployeeEntity> empList = employeeRepo.findByCompanyId(companyId);

            for (EmployeeEntity emp : empList) {
                UUID empId = emp.getEmployeeId();
                consents     += consentRepo.deleteByEmployeeId(empId);
                documents    += deleteDocumentsForEmployee(empId);
                approvals    += deleteApprovalsForEmployee(empId);
                pointRecords += deletePointRecordsForEmployee(empId);
                purgeSandboxArtifacts(emp);
            }

            accesses += accessRepo.deleteByCompanyId(companyId);
            employees += empList.size();
            employeeRepo.deleteAll(empList);
        }

        var userOpt = userRepo.findByUsernameIgnoreCase(props.getUsername());
        if (userOpt.isPresent()) {
            userRepo.delete(userOpt.get());
            users = 1;
        }

        if (companyOpt.isPresent()) {
            companyRepo.delete(companyOpt.get());
            companies = 1;
        }

        int files = deleteSandboxFiles();

        log.info("[DemoSandbox] Purge complete: companies={} users={} employees={} " +
                        "pointRecords={} docs={} consents={} accesses={} files={} sessions={}",
                companies, users, employees, pointRecords, documents, consents, accesses, files, sessions);

        return new PurgeCounters(companies, users, employees, pointRecords,
                approvals, documents, consents, accesses, files, sessions);
    }

    private int deleteDocumentsForEmployee(UUID employeeId) {
        var docs = documentRepo.findByEmployeeIdOrderByUploadedAtDesc(employeeId);
        for (var doc : docs) {
            String path = doc.getStoragePath();
            if (path != null && !path.startsWith("/")) {
                try {
                    bucketStorageProvider.deleteFile(doc.getType(), path);
                } catch (Exception e) {
                    log.warn("[DemoSandbox] Could not delete S3 document: path={} reason={}", path, e.getMessage());
                }
            }
        }
        documentRepo.deleteAll(docs);
        return docs.size();
    }

    private void purgeSandboxArtifacts(EmployeeEntity emp) {
        UUID empId = emp.getEmployeeId();
        try {
            faceRecognitionProvider.deleteFacesByExternalImageId(empId);
        } catch (Exception e) {
            log.warn("[DemoSandbox] Could not delete Rekognition face: employeeId={} reason={}", empId, e.getMessage());
        }
        String faceKey = emp.getFaceS3ObjectKey();
        if (faceKey != null && !faceKey.isBlank()) {
            try {
                faceStorageProvider.deleteFaceImage(faceKey);
            } catch (Exception e) {
                log.warn("[DemoSandbox] Could not delete face S3 image: key={} reason={}", faceKey, e.getMessage());
            }
        }
    }

    private int deleteApprovalsForEmployee(UUID employeeId) {
        var records = timeRecordRepo.findByEmployeeId(employeeId);
        int count = 0;
        for (TimeRecordEntity rec : records) {
            if (approvalRepo.existsById(rec.getTimeRecordId())) {
                approvalRepo.deleteById(rec.getTimeRecordId());
                count++;
            }
        }
        return count;
    }

    private int deletePointRecordsForEmployee(UUID employeeId) {
        var records = timeRecordRepo.findByEmployeeId(employeeId);
        timeRecordRepo.deleteAll(records);
        return records.size();
    }

    private int deleteSandboxFiles() {
        Path root = Path.of(props.getLocalStorageRoot());
        if (!Files.exists(root)) return 0;

        AtomicInteger fileCount = new AtomicInteger(0);
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    fileCount.incrementAndGet();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.warn("[DemoSandbox] Error deleting sandbox files at {}: {}", root, e.getMessage());
        }
        return fileCount.get();
    }
}
