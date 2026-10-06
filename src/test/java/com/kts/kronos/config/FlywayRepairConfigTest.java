package com.kts.kronos.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import org.mockito.InOrder;
import static org.mockito.Mockito.*;

class FlywayRepairConfigTest {

    @Test
    void repairAndMigrate_invokesRepairBeforeMigrate() {
        FlywayRepairConfig config = new FlywayRepairConfig();
        Flyway flyway = mock(Flyway.class);
        config.repairAndMigrate().migrate(flyway);
        InOrder order = inOrder(flyway);
        order.verify(flyway).repair();
        order.verify(flyway).migrate();
    }
}
