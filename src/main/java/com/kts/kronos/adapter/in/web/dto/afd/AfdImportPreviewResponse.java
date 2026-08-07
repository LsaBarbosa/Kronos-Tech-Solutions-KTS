package com.kts.kronos.adapter.in.web.dto.afd;

import java.util.List;

public record AfdImportPreviewResponse(
        String companyName,
        String cnpj,
        String razaoSocial,
        String periodoInicio,
        String periodoFim,
        int totalMarks,
        int alreadyImported,
        int toImport,
        int employeesFound,
        int employeesNotFound,
        List<String> notFoundPisList,
        int openRecords,
        int skippedLines
) {}
