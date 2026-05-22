package com.kts.kronos.adapter.in.web.dto.employee;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.IMAGE_DATA_REGISTER_NOT_BLANK;


public record RegisterFaceRequest(
    @NotBlank(message = IMAGE_DATA_REGISTER_NOT_BLANK)
    @Size(max = 1500000, message = "A imagem da face excede o tamanho máximo permitido.")
    String faceImageBase64,

     UUID employeeId
) {
    @Override
    public String toString() {
        return "RegisterFaceRequest[faceImageBase64=***MASKED***, employeeId=" + employeeId + "]";
    }
}
