package com.kts.kronos.adapter.in.web.dto.public_privacy;

import java.time.LocalDate;
import java.util.List;

public record PublicBiometricTermResponse(
        String version,
        LocalDate effectiveDate,
        String title,
        List<BiometricTermSectionResponse> sections
) {
}
