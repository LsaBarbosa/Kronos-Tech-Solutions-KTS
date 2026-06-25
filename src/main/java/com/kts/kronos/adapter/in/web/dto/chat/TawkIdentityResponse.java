package com.kts.kronos.adapter.in.web.dto.chat;

public record TawkIdentityResponse(
        String userId,
        String name,
        String email,
        String hash,
        long ttlSeconds
) {}
