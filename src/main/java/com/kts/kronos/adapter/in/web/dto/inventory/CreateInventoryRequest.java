package com.kts.kronos.adapter.in.web.dto.inventory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInventoryRequest(
        @NotBlank(message = "Código do processo é obrigatório")
        String processCode,

        @NotBlank(message = "Nome do processo é obrigatório")
        String processName,

        @NotBlank(message = "Categoria de dados é obrigatória")
        String dataCategory,

        @NotBlank(message = "Campos de dados são obrigatórios")
        String dataFields,

        @NotBlank(message = "Categoria de titular de dados é obrigatória")
        String dataSubjectCategory,

        @NotBlank(message = "Finalidade é obrigatória")
        String purpose,

        @NotBlank(message = "Base legal é obrigatória")
        String legalBasis,

        @NotNull(message = "Indicador de dados sensíveis é obrigatório")
        Boolean sensitiveData,

        @NotBlank(message = "Sistema de origem é obrigatório")
        String sourceSystem,

        String storageLocation,

        String retentionPolicyCode,

        String externalSharing,

        @NotNull(message = "Indicador de transferência internacional é obrigatório")
        Boolean internationalTransfer,

        String securityMeasures,

        @NotNull(message = "Status ativo é obrigatório")
        Boolean active
) {}
