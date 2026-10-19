package com.autotest.dto;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        String baseUrl,
        Long ownerId,
        String ownerUsername,
        Instant createdAt,
        Instant updatedAt
) {}
