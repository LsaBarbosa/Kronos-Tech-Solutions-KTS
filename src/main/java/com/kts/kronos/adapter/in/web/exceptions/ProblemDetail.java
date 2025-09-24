package com.kts.kronos.adapter.in.web.exceptions;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;


@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProblemDetail {
    private Integer status;
    private String type;
    private String title;
    private String detail;
    private List<Error> errors;

    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private final LocalDateTime timestamp = LocalDateTime.now();

    @Getter
    @Builder
    public static class Error {
        private String name;
        private String userMessage;
    }
}
