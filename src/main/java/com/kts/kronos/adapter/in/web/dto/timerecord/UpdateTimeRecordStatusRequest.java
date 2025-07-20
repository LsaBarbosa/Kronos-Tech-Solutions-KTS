package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.kts.kronos.domain.model.StatusRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import static com.kts.kronos.constants.Messages.STATUS_NOT_BLANK;
import static com.kts.kronos.constants.Swagger.*;

@Schema(name = UPDATE_RECORD_STATUS_REQUEST, description = DTO_UPDATE_RECORD_STATUS_REQUEST)
public record UpdateTimeRecordStatusRequest(
        @Schema(description = STATUS_RECORD, example = STATUS_RECORD_EXEMPLE)
        @NotNull(message = STATUS_NOT_BLANK) StatusRecord statusRecord
) {

}

