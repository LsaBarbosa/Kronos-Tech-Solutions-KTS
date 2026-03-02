package com.kts.kronos.adapter.in.web.exceptions;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailTest {

    @Test
    void shouldBuildProblemDetailWithNestedErrorsAndTimestamp() {
        var error = ProblemDetail.Error.builder()
                .name("field")
                .userMessage("invalid")
                .build();

        var detail = ProblemDetail.builder()
                .status(400)
                .title("Bad Request")
                .type("validation")
                .detail("payload inválido")
                .errors(List.of(error))
                .build();

        assertThat(detail.getStatus()).isEqualTo(400);
        assertThat(detail.getTitle()).isEqualTo("Bad Request");
        assertThat(detail.getType()).isEqualTo("validation");
        assertThat(detail.getDetail()).isEqualTo("payload inválido");
        assertThat(detail.getErrors()).hasSize(1);
        assertThat(detail.getErrors().get(0).getName()).isEqualTo("field");
        assertThat(detail.getTimestamp()).isBeforeOrEqualTo(LocalDateTime.now());
    }
}
