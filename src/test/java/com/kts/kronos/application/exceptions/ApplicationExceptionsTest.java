package com.kts.kronos.application.exceptions;

import org.junit.jupiter.api.Test;

import static com.kts.kronos.constants.ExceptionMessages.FORBIDDEN_RESOURCE_ACCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationExceptionsTest {

    @Test
    void badRequestExceptionShouldKeepOriginalMessage() {
        var ex = new BadRequestException("bad request");

        assertEquals("bad request", ex.getMessage());
    }

    @Test
    void resourceNotFoundExceptionShouldKeepOriginalMessage() {
        var ex = new ResourceNotFoundException("not found");

        assertEquals("not found", ex.getMessage());
    }

    @Test
    void internalServerExceptionShouldKeepOriginalMessage() {
        var ex = new InternalServerException("internal");

        assertEquals("internal", ex.getMessage());
    }

    @Test
    void serviceUnavailableExceptionShouldKeepOriginalMessage() {
        var ex = new ServiceUnavailableException("service down");

        assertEquals("service down", ex.getMessage());
    }

    @Test
    void tooManyRequestsExceptionShouldKeepOriginalMessage() {
        var ex = new TooManyRequestsException("too many requests");

        assertEquals("too many requests", ex.getMessage());
    }

    @Test
    void forbiddenExceptionShouldAlwaysUseStandardMessage() {
        var ex = new ForbiddenException("ignored");

        assertEquals(FORBIDDEN_RESOURCE_ACCESS, ex.getMessage());
    }
}
