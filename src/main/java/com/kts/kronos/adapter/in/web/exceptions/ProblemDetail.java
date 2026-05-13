package com.kts.kronos.adapter.in.web.exceptions;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetail {
    private String code;
    private String message;
    private Integer status;
    private String path;
    @Builder.Default
    private Instant timestamp = Instant.now();
    private List<Error> validationErrors;
    private String redirectUrl;

    // Backward-compatible aliases for existing clients/tests while the API migrates to code/message.
    private String type;
    private String title;
    private String detail;
    private List<Error> errors;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Error {
        private String field;
        private String message;

        // Backward-compatible aliases.
        private String name;
        private String userMessage;
    }
}
