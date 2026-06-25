package com.kts.kronos.adapter.in.web.dto.chat;

import java.util.List;
import java.util.Map;

public record TawkIdentityResponse(
        String userId,
        String name,
        String email,
        String hash,
        long ttlSeconds,
        Map<String, String> attributes,
        List<String> tags
) {}
