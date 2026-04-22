package com.kts.kronos.constants;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LegalTextsTest {

    @Test
    void shouldExposeLegalTextConstantsAndPrivateConstructor() throws Exception {
        Constructor<LegalTexts> constructor = LegalTexts.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertNotNull(constructor.newInstance());
        assertNotNull(LegalTexts.BIOMETRIC_TERM_TITLE);
        assertNotNull(LegalTexts.TECH_CERT_DECLARATION);
    }
}
