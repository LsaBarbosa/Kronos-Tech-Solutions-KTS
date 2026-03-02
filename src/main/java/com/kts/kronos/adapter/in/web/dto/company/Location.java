package com.kts.kronos.adapter.in.web.dto.company;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record Location(Double latitude, Double longitude) {
}
