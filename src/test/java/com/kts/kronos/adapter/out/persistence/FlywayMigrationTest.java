package com.kts.kronos.adapter.out.persistence;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("kronos_migration_test")
            .withUsername("kronos")
            .withPassword("kronos");

    @Test
    void shouldApplyMigrationsAndCreateCriticalConstraints() {
        DataSource dataSource = DataSourceBuilder.create()
                .driverClassName(POSTGRES.getDriverClassName())
                .url(POSTGRES.getJdbcUrl())
                .username(POSTGRES.getUsername())
                .password(POSTGRES.getPassword())
                .build();

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        var jdbcTemplate = new JdbcTemplate(dataSource);

        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'tb_company'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'uk_tb_company_cnpj'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'uk_tb_employee_cpf'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'uix_tb_user_username_lower'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'uk_tb_password_reset_token_user'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'tb_blacklisted_token'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'idx_tb_blacklisted_token_expires_at'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'tb_legal_consent'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'idx_legal_consent_employee_type'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'idx_legal_consent_active'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'uix_legal_consent_active_by_employee_type'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'tb_legal_text'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'uix_legal_text_type_version'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'uix_legal_text_active_by_type'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'tb_document' AND column_name = 'checksum_sha256'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'tb_lgpd_request'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'tb_lgpd_request_history'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'idx_lgpd_request_employee_created'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'idx_lgpd_request_company_status'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'idx_lgpd_request_history_request_created'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'tb_retention_policy'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'uix_retention_policy_code'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'idx_retention_policy_enabled'"));
        assertEquals(2, count(jdbcTemplate, "SELECT COUNT(*) FROM tb_retention_policy WHERE enabled = true"));
        assertEquals(100, count(jdbcTemplate, """
                SELECT character_maximum_length
                  FROM information_schema.columns
                 WHERE table_name = 'tb_employee'
                   AND column_name = 'email'
                """));
        assertEquals(1, count(jdbcTemplate, """
                SELECT COUNT(*)
                  FROM tb_legal_text
                 WHERE document_type = 'BIOMETRIC_CONSENT_TERM'
                   AND active = true
                """));
        assertEquals(1, count(jdbcTemplate, """
                SELECT COUNT(*)
                  FROM information_schema.table_constraints
                 WHERE table_name = 'tb_company_nsr'
                   AND constraint_type = 'PRIMARY KEY'
                """));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'tb_user' AND column_name = 'deleted_at'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'tb_company' AND column_name = 'deactivation_reason'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'tb_audit_logs' AND column_name = 'company_id'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'tb_audit_logs' AND column_name = 'resource_type'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'tb_audit_logs' AND column_name = 'resource_id'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'tb_audit_logs' AND column_name = 'correlation_id'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'tb_audit_logs' AND column_name = 'risk_level'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'idx_audit_logs_company_id'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'idx_audit_logs_resource'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'tb_security_incident'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'tb_security_incident' AND column_name = 'incident_id'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'tb_security_incident' AND column_name = 'title'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'tb_security_incident' AND column_name = 'description'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'tb_security_incident' AND column_name = 'detected_at'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'tb_security_incident' AND column_name = 'severity'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'tb_security_incident' AND column_name = 'status'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'idx_security_incident_status'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'idx_security_incident_detected_at'"));
    }

    private Integer count(JdbcTemplate jdbcTemplate, String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
