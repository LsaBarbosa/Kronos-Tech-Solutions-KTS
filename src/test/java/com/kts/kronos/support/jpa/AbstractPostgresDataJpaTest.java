package com.kts.kronos.support.jpa;

import com.kts.kronos.KronosApplication;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = KronosApplication.class)
public abstract class AbstractPostgresDataJpaTest extends AbstractPostgresContainerTest {
}