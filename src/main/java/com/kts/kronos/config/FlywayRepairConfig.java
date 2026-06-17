package com.kts.kronos.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Estratégia de boot do Flyway que executa {@code repair()} antes de {@code migrate()}.
 *
 * <p>Necessário pontualmente quando uma migration já aplicada teve seu arquivo alterado
 * (line endings, reformatação, correção de tipo de coluna), gerando "checksum mismatch".
 * O {@code repair()} apenas realinha o checksum registrado em {@code flyway_schema_history}
 * com o conteúdo atual do arquivo — não modifica dados nem re-executa migrations.</p>
 *
 * <p>Como migrations idempotentes são aceitáveis, manter esse bean ativo é seguro em
 * desenvolvimento. Remover (ou condicionar a um profile) em produção depois do primeiro
 * boot bem-sucedido para voltar à validação estrita.</p>
 */
@Slf4j
@Configuration
public class FlywayRepairConfig {

    @Bean
    public FlywayMigrationStrategy repairAndMigrate() {
        return flyway -> {
            log.info("event=flyway_boot action=repair_then_migrate");
            flyway.repair();
            flyway.migrate();
        };
    }
}
