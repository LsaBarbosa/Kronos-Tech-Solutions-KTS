package com.kts.kronos.adapter.in.web.dto.employee;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.IMAGE_DATA_REGISTER_NOT_BLANK;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record RegisterFaceRequest(
    @NotBlank(message = IMAGE_DATA_REGISTER_NOT_BLANK)
    String faceImageBase64,

     UUID employeeId
){}
