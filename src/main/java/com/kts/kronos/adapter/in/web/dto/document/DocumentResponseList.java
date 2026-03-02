package com.kts.kronos.adapter.in.web.dto.document;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record DocumentResponseList(
        List<DocumentResponse> documents
) {
}
