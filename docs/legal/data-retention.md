# Data Retention & Soft Delete Policy

## Overview

Kronos implements data retention policies compliant with legal and regulatory requirements. This document defines retention periods, deletion procedures, and soft delete mechanisms.

## Legal Retention Requirements

### Employee Records

- **Retention Period**: 5 years from employee termination
- **Reason**: Labor law requirements in Brazil (CLT - Consolidação das Leis do Trabalho)
- **Categories**: Personal data, salary, attendance, documents, communications

### Financial Records

- **Retention Period**: 7 years from transaction date
- **Reason**: Tax and accounting law requirements
- **Categories**: Invoices, payments, time records with billing implications

### Audit Logs

- **Retention Period**: 2 years from event date
- **Reason**: Security and compliance audit trail
- **Categories**: Login, document access, configuration changes, data modifications

### Biometric Data

- **Retention Period**: Duration of employment + 1 year
- **Reason**: LGPD (Lei Geral de Proteção de Dados) - Brazilian GDPR equivalent
- **Storage**: Encrypted, isolated from other data
- **Access**: Only for authentication, no secondary use

### Personal Documents

- **Retention Period**: As required by regulation + 6 months grace
- **Reason**: Employment verification, regulatory compliance
- **Categories**: CPF, PIS, identity documents, medical certificates

## Soft Delete Implementation

### Strategy

Kronos uses soft deletes (logical deletion) instead of physical deletion:

- **Deleted rows are marked but not removed**
- **Enables data recovery and audit trail**
- **Preserves referential integrity**
- **Complies with retention policies**

### Database Pattern

All tables with retention requirements include:

```sql
ALTER TABLE employees ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE documents ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE time_records ADD COLUMN deleted_at TIMESTAMP;

CREATE INDEX idx_employees_deleted_at ON employees(deleted_at);
```

### Query Pattern

```java
// In JPA Repositories
@Query("SELECT e FROM Employee e WHERE e.deletedAt IS NULL")
List<Employee> findActiveEmployees();

// In Service
public void deleteEmployee(UUID employeeId) {
    Employee employee = findById(employeeId);
    employee.setDeletedAt(LocalDateTime.now());
    save(employee);
    
    log.info("Soft deleted employee: {}", employeeId);
}
```

## Deletion Process

### Employee Departure

1. **Termination Date**: Employee.terminatedAt = today
2. **Account Deactivation**: User.enabled = false
3. **Data Archival**: All documents marked for retention
4. **Soft Delete**: Employee.deletedAt = today + 6 months
5. **Permanent Deletion**: After 5 years (automated batch job)

### Document Deletion

1. **User Request**: Document marked for deletion
2. **Soft Delete**: Document.deletedAt = now
3. **Retention Check**: Based on document type and employee status
4. **Permanent Deletion**: After retention period expires

### Personal Data Deletion (LGPD Right to Erasure)

For LGPD requests (right to be forgotten):

1. **Validate Request**: Confirm identity and consent
2. **Soft Delete**: Mark all personal data
3. **Anonymize**: Replace with anonymized versions where possible
4. **Notify**: Inform data subject of deletion
5. **Audit Log**: Record deletion reason and date

```java
public void deletePersonalData(UUID employeeId, String reason) {
    // Soft delete employee
    Employee employee = findById(employeeId);
    employee.setDeletedAt(LocalDateTime.now());
    
    // Anonymize personal identifiers
    employee.setCpf("REDACTED");
    employee.setPis("REDACTED");
    
    // Keep only what's required for tax/legal purposes
    employee.setFullName("DELETED_" + UUID.randomUUID());
    
    // Audit
    auditLog.log(AuditEvent.PERSONAL_DATA_DELETED, employeeId, reason);
    
    save(employee);
}
```

## Batch Deletion (Scheduled Job)

### Job Definition

```java
@Configuration
@EnableScheduling
public class DataRetentionScheduler {
    
    @Scheduled(cron = "0 2 * * *")  // Daily at 2 AM
    public void permanentlyDeleteExpiredData() {
        deletePermanentlyExpiredEmployees();
        deletePermanentlyExpiredDocuments();
        cleanupPermanentlyDeletedAuditLogs();
    }
    
    private void deletePermanentlyExpiredEmployees() {
        LocalDateTime threshold = LocalDateTime.now().minusYears(5);
        
        List<Employee> toDelete = employeeRepository
            .findByDeletedAtBefore(threshold);
        
        toDelete.forEach(employee -> {
            // Permanent delete
            anonymizeEmployee(employee);
            employeeRepository.delete(employee);
            
            // Audit
            log.info("Permanently deleted employee: {}", employee.getId());
        });
    }
}
```

## Querying Active Data

### Best Practice

Always filter out soft-deleted rows in queries:

```java
// Good: Explicit filter for active records
@Query("SELECT e FROM Employee e WHERE e.deletedAt IS NULL")
List<Employee> findAll();

// Avoid: Including deleted records
@Query("SELECT e FROM Employee e")
List<Employee> findAllIncludingDeleted();
```

### Base Entity Pattern

```java
@MappedSuperclass
public abstract class AuditableEntity {
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    private LocalDateTime deletedAt;
    
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
```

## Audit Trail

### Audit Logging

All delete operations are logged:

```java
@Aspect
@Component
public class DeletionAudit {
    
    @AfterReturning("@annotation(SoftDelete)")
    public void auditDeletion(JoinPoint joinPoint) {
        Object id = joinPoint.getArgs()[0];
        UUID userId = getCurrentUserId();
        
        auditLog.log(AuditEvent.SOFT_DELETE, id, userId);
    }
}
```

### Retention Audit Report

```sql
-- Find data approaching retention expiration
SELECT 
    'Employee' as type,
    id,
    deleted_at,
    deleted_at + INTERVAL '5 years' as expiration_date,
    CURRENT_DATE - (deleted_at::date + INTERVAL '5 years'::interval) as days_until_expiration
FROM employees
WHERE deleted_at IS NOT NULL
  AND deleted_at + INTERVAL '5 years' < CURRENT_DATE + INTERVAL '30 days'
ORDER BY expiration_date;
```

## Compliance Verification

### Monthly Verification

```bash
#!/bin/bash
# Verify soft delete implementation

# Check for hard deletes in last 30 days
DELETE_COUNT=$(grep -r "DELETE FROM" logs/*.log | wc -l)

# Check for unlogged data access
UNAUTH_ACCESS=$(grep "Unauthorized data access" logs/*.log | wc -l)

# Verify audit completeness
UNAUDITED=$(psql -c "SELECT COUNT(*) FROM employees WHERE deleted_at IS NOT NULL AND audit_log_id IS NULL" | tail -1)

echo "Hard deletes last 30 days: $DELETE_COUNT"
echo "Unauthorized access attempts: $UNAUTH_ACCESS"
echo "Unaudited soft deletes: $UNAUDITED"
```

## LGPD Compliance

Kronos implements LGPD requirements:

- **Data Minimization**: Collect only necessary data
- **Explicit Consent**: For biometric and tracking data
- **Right to Access**: API endpoint for data export
- **Right to Correction**: Users can update their data
- **Right to Deletion**: Soft delete with anonymization
- **Right to Data Portability**: Export in standard format
- **Data Security**: Encryption at rest and in transit
- **Privacy by Design**: Privacy considered in all features

## Related Documentation

- [LGPD (Lei Geral de Proteção de Dados)](https://www.gov.br/cidadania/pt-br/acesso-a-informacao/lgpd)
- [Brazilian Labor Law (CLT)](https://www.planalto.gov.br/ccivil_03/decreto-lei/del5452.htm)
- [ISO 27001 - Information Security Management](https://www.iso.org/isoiec-27001-information-security-management.html)
