package com.kts.kronos.adapter.in.web.dto.timerecord;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import static com.kts.kronos.constants.Messages.ID_NOT_BLANK;
import static com.kts.kronos.constants.Messages.INVALID_FORMAT;
import static com.kts.kronos.constants.Messages.TIME_NOT_NULL;
public record UpdateBreakRecordRequest(@NotNull(message = ID_NOT_BLANK)
                                       Long breakRecordId, // ID da pausa original a ser alterada

                                       @NotBlank(message = TIME_NOT_NULL)
                                       @Pattern(regexp = "\\d{2}:\\d{2}", message = INVALID_FORMAT)
                                       String startHour,

                                       @NotBlank(message = TIME_NOT_NULL)
                                       @Pattern(regexp = "\\d{2}:\\d{2}", message = INVALID_FORMAT)
                                       String endHour) {
}
