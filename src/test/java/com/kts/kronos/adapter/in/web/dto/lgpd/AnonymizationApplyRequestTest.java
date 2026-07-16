package com.kts.kronos.adapter.in.web.dto.lgpd;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers AnonymizationApplyRequest.isJustificationValid() - L26, all 4 branches:
 * justification=null, justification="", justification="  ", justification="valid"
 */
class AnonymizationApplyRequestTest {

    private boolean invokeIsJustificationValid(String justification) throws Exception {
        AnonymizationApplyRequest req = new AnonymizationApplyRequest(
                justification, true, UUID.randomUUID()
        );
        Method m = AnonymizationApplyRequest.class.getDeclaredMethod("isJustificationValid");
        m.setAccessible(true);
        return (Boolean) m.invoke(req);
    }

    @Test
    void isJustificationValid_null_returnsFalse() throws Exception {
        assertFalse(invokeIsJustificationValid(null));
    }

    @Test
    void isJustificationValid_empty_returnsFalse() throws Exception {
        assertFalse(invokeIsJustificationValid(""));
    }

    @Test
    void isJustificationValid_blankOnly_returnsFalse() throws Exception {
        assertFalse(invokeIsJustificationValid("   "));
    }

    @Test
    void isJustificationValid_valid_returnsTrue() throws Exception {
        assertTrue(invokeIsJustificationValid("Valid justification text"));
    }
}
