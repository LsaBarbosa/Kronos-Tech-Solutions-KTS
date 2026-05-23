package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.AuditLog;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.LegalConsent;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.application.util.SensitiveDataMasker;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.LegalBasis;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record LgpdEmployeeExportResponse(
        ExportManifest manifest,
        ExportedEmployee employee,
        ExportedUser user,
        ExportedCompany company,
        List<ExportedDocumentMetadata> documents,
        List<ExportedTimeRecord> timeRecords,
        List<ExportedMessage> messages,
        List<ExportedAuditLog> auditLogs,
        List<ExportedLegalConsent> legalConsents,
        ExportedBiometricStatus biometricStatus,
        Instant exportedAt
) {
    public static LgpdEmployeeExportResponse from(
            Employee employee,
            User user,
            Company company,
            List<Document> documents,
            List<TimeRecord> timeRecords,
            List<Message> messages,
            List<AuditLog> auditLogs,
            List<LegalConsent> legalConsents,
            boolean includePreciseGeolocation,
            UUID requestedByUserId
    ) {
        boolean hasFaceImage = employee.faceS3ObjectKey() != null && !employee.faceS3ObjectKey().isBlank();
        boolean hasActiveBiometricConsent = legalConsents.stream()
                .anyMatch(consent -> consent.consentType() == ConsentType.BIOMETRIC_AUTHENTICATION && consent.isActive());

        Instant exportedAt = Instant.now();
        ExportManifest manifest = new ExportManifest(
                UUID.randomUUID(),
                exportedAt,
                requestedByUserId,
                employee.employeeId(),
                includePreciseGeolocation,
                java.util.Arrays.asList("employee", "user", "company", "documents", "timeRecords", "messages", "auditLogs", "legalConsents", "biometricStatus"),
                java.util.Arrays.asList("Este arquivo contém dados pessoais e pode conter dados sensíveis. Mantenha-o em local seguro.")
        );

        return new LgpdEmployeeExportResponse(
                manifest,
                ExportedEmployee.from(employee),
                ExportedUser.from(user),
                ExportedCompany.from(company),
                documents.stream().map(ExportedDocumentMetadata::from).toList(),
                timeRecords.stream()
                        .map(record -> ExportedTimeRecord.from(record, includePreciseGeolocation))
                        .toList(),
                messages.stream().map(ExportedMessage::from).toList(),
                auditLogs.stream().map(ExportedAuditLog::from).toList(),
                legalConsents.stream().map(ExportedLegalConsent::from).toList(),
                new ExportedBiometricStatus(
                        hasFaceImage,
                        hasActiveBiometricConsent,
                        hasFaceImage && hasActiveBiometricConsent,
                        documents.stream().filter(document -> document.type() == DocumentType.BIOMETRIC_CONSENT_TERM).count()
                ),
                exportedAt
        );
    }

    public record ExportedEmployee(
            UUID employeeId,
            String fullName,
            String cpf,
            String pis,
            String jobPosition,
            String email,
            double salary,
            String phone,
            boolean active,
            ExportedAddress address,
            UUID companyId,
            LocalDateTime lastSeenMessageTimestamp,
            boolean homeOffice,
            LocalTime workStartTime,
            LocalTime workEndTime,
            LocalTime breakStartTime,
            LocalTime breakEndTime,
            WorkScheduleType scheduleType,
            LocalDate scaleStartDate,
            java.time.DayOfWeek preferredDayOff,
            Integer weekendOffIndex,
            Set<java.time.DayOfWeek> fixedWorkDays,
            LocalDateTime deletedAt,
            UUID deletedBy,
            String deactivationReason
    ) {
        static ExportedEmployee from(Employee employee) {
            return new ExportedEmployee(
                    employee.employeeId(),
                    employee.fullName(),
                    employee.cpf(),
                    employee.pis(),
                    employee.jobPosition(),
                    employee.email(),
                    employee.salary(),
                    employee.phone(),
                    employee.active(),
                    ExportedAddress.from(employee.address()),
                    employee.companyId(),
                    employee.lastSeenMessageTimestamp(),
                    employee.homeOffice(),
                    employee.workStartTime(),
                    employee.workEndTime(),
                    employee.breakStartTime(),
                    employee.breakEndTime(),
                    employee.scheduleType(),
                    employee.scaleStartDate(),
                    employee.preferredDayOff(),
                    employee.weekendOffIndex(),
                    employee.fixedWorkDays(),
                    employee.deletedAt(),
                    employee.deletedBy(),
                    employee.deactivationReason()
            );
        }
    }

    public record ExportedUser(
            UUID userId,
            String username,
            Role role,
            boolean active,
            UUID employeeId,
            LocalDateTime deletedAt,
            UUID deletedBy,
            String deactivationReason
    ) {
        static ExportedUser from(User user) {
            if (user == null) {
                return null;
            }
            return new ExportedUser(
                    user.userId(),
                    user.username(),
                    user.role(),
                    user.active(),
                    user.employeeId(),
                    user.deletedAt(),
                    user.deletedBy(),
                    user.deactivationReason()
            );
        }
    }

    public record ExportedCompany(
            UUID companyId,
            String name,
            String cnpj,
            String email,
            boolean active,
            ExportedAddress address,
            Location location,
            long activeEmployees,
            long inactiveEmployees,
            LocalDateTime deletedAt,
            UUID deletedBy,
            String deactivationReason
    ) {
        static ExportedCompany from(Company company) {
            return new ExportedCompany(
                    company.companyId(),
                    company.name(),
                    company.cnpj(),
                    company.email(),
                    company.active(),
                    ExportedAddress.from(company.address()),
                    company.location(),
                    company.activeEmployees(),
                    company.inactiveEmployees(),
                    company.deletedAt(),
                    company.deletedBy(),
                    company.deactivationReason()
            );
        }
    }

    public record ExportedAddress(
            String street,
            String number,
            String postalCode,
            String city,
            String state
    ) {
        static ExportedAddress from(Address address) {
            if (address == null) {
                return null;
            }
            return new ExportedAddress(
                    address.street(),
                    address.number(),
                    address.postalCode(),
                    address.city(),
                    address.state()
            );
        }
    }

    public record ExportedDocumentMetadata(
            UUID documentId,
            UUID employeeId,
            DocumentType type,
            String fileName,
            String contentType,
            LocalDateTime uploadedAt,
            Long timeRecordId,
            boolean deletedByEmployee,
            boolean deletedByManager,
            String checksumSha256
    ) {
        static ExportedDocumentMetadata from(Document document) {
            return new ExportedDocumentMetadata(
                    document.documentId(),
                    document.employeeId(),
                    document.type(),
                    document.fileName(),
                    document.contentType(),
                    document.uploadeAt(),
                    document.timeRecordId(),
                    document.deletedByEmployee(),
                    document.deletedByManager(),
                    document.checksumSha256()
            );
        }
    }

    public record ExportedTimeRecord(
            Long timeRecordId,
            LocalDateTime startWork,
            LocalDateTime endWork,
            StatusRecord statusRecord,
            boolean edited,
            boolean active,
            UUID employeeId,
            Double latitude,
            Double longitude,
            Double endLatitude,
            Double endLongitude,
            boolean geolocationPresent,
            boolean endGeolocationPresent,
            Long nsrCheckin,
            Long nsrCheckout,
            LocalDateTime originalStartWork,
            LocalDateTime originalEndWork
    ) {
        static ExportedTimeRecord from(TimeRecord record, boolean includePreciseGeolocation) {
            boolean geolocationPresent = record.latitude() != null || record.longitude() != null;
            boolean endGeolocationPresent = record.endLatitude() != null || record.endLongitude() != null;
            return new ExportedTimeRecord(
                    record.timeRecordId(),
                    record.startWork(),
                    record.endWork(),
                    record.statusRecord(),
                    record.edited(),
                    record.active(),
                    record.employeeId(),
                    includePreciseGeolocation ? record.latitude() : null,
                    includePreciseGeolocation ? record.longitude() : null,
                    includePreciseGeolocation ? record.endLatitude() : null,
                    includePreciseGeolocation ? record.endLongitude() : null,
                    geolocationPresent,
                    endGeolocationPresent,
                    record.nsrCheckin(),
                    record.nsrCheckout(),
                    record.originalStartWork(),
                    record.originalEndWork()
            );
        }
    }

    public record ExportedMessage(
            UUID messageId,
            UUID employeeId,
            UUID companyId,
            String title,
            String messageText,
            MessagePriority priority,
            LocalDateTime createdAt,
            UUID recipientEmployeeId
    ) {
        static ExportedMessage from(Message message) {
            return new ExportedMessage(
                    message.messageId(),
                    message.employeeId(),
                    message.companyId(),
                    message.title(),
                    message.messageText(),
                    message.priority(),
                    message.createdAt(),
                    message.recipientEmployeeId()
            );
        }
    }

    public record ExportedAuditLog(
            UUID id,
            UUID userId,
            String action,
            String ipAddress,
            String userAgent,
            String details,
            LocalDateTime timestamp
    ) {
        static ExportedAuditLog from(AuditLog auditLog) {
            return new ExportedAuditLog(
                    auditLog.id(),
                    auditLog.userId(),
                    auditLog.action(),
                    auditLog.ipAddress(),
                    auditLog.userAgent(),
                    SensitiveDataMasker.sanitizeDetails(auditLog.details()),
                    auditLog.timestamp()
            );
        }
    }

    public record ExportedLegalConsent(
            UUID consentId,
            UUID employeeId,
            UUID userId,
            ConsentType consentType,
            LegalBasis legalBasis,
            String purpose,
            String version,
            Instant grantedAt,
            Instant revokedAt,
            String ipAddress,
            String userAgent,
            UUID evidenceDocumentId,
            String evidenceHashSha256,
            Instant createdAt,
            Instant updatedAt
    ) {
        static ExportedLegalConsent from(LegalConsent consent) {
            return new ExportedLegalConsent(
                    consent.consentId(),
                    consent.employeeId(),
                    consent.userId(),
                    consent.consentType(),
                    consent.legalBasis(),
                    consent.purpose(),
                    consent.version(),
                    consent.grantedAt(),
                    consent.revokedAt(),
                    consent.ipAddress(),
                    consent.userAgent(),
                    consent.evidenceDocumentId(),
                    consent.evidenceHashSha256(),
                    consent.createdAt(),
                    consent.updatedAt()
            );
        }
    }

    public record ExportedBiometricStatus(
            boolean faceImageRegistered,
            boolean activeBiometricConsent,
            boolean biometricLoginEnabled,
            long biometricEvidenceDocumentCount
    ) {
    }

    public record ExportManifest(
            UUID exportId,
            Instant exportedAt,
            UUID requestedByUserId,
            UUID targetEmployeeId,
            boolean includePreciseGeolocation,
            java.util.List<String> sections,
            java.util.List<String> warnings
    ) {
    }
}
