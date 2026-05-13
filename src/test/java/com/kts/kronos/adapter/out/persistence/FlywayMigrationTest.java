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
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'uk_tb_company_company_cnpj'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'uk_tb_employee_cpf'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'uk_tb_user_username_lower'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM pg_indexes WHERE indexname = 'uk_tb_password_reset_token_user_id'"));
        assertEquals(1, count(jdbcTemplate, """
                SELECT COUNT(*)
                  FROM information_schema.table_constraints
                 WHERE table_name = 'tb_company_nsr'
                   AND constraint_type = 'PRIMARY KEY'
                """));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'tb_user' AND column_name = 'deleted_at'"));
        assertEquals(1, count(jdbcTemplate, "SELECT COUNT(*) FROM information_schema.columns WHERE table_name = 'tb_company' AND column_name = 'deactivation_reason'"));
    }

    private Integer count(JdbcTemplate jdbcTemplate, String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
