package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.kts.kronos.domain.model.StatusRecord;
import jakarta.validation.constraints.NotNull;

import static com.kts.kronos.constants.Messages.STATUS_NOT_BLANK;

public record UpdateTimeRecordStatusRequest(
        @NotNull(message = STATUS_NOT_BLANK) StatusRecord statusRecord
) {

}

