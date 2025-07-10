package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.StatusRecord;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SimpleReportRequest(

        @NotNull UUID employeeId,
        @NotNull String reference,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
        LocalDate[] dates
){}