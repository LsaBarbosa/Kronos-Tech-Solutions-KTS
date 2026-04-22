package com.kts.kronos.adapter.in.web.exceptions;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProblemDetailTest {

    @Test
    void shouldExposeBuiltProblemDetailsAndErrors() {
        var error = ProblemDetail.Error.builder()
                .name("field")
                .userMessage("invalid")
                .build();

        var detail = ProblemDetail.builder()
                .status(400)
                .type("about:blank")
                .title("Bad Request")
                .detail("invalid input")
                .errors(List.of(error))
                .build();

        assertEquals(400, detail.getStatus());
        assertEquals("about:blank", detail.getType());
        assertEquals("Bad Request", detail.getTitle());
        assertEquals("invalid input", detail.getDetail());
        assertNotNull(detail.getTimestamp());
        assertEquals("field", detail.getErrors().getFirst().getName());
        assertEquals("invalid", detail.getErrors().getFirst().getUserMessage());
    }
}
