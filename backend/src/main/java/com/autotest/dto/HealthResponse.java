package com.autotest.dto;

import java.time.Instant;
import java.util.Map;

public record HealthResponse(
        String status,
        String appName,
        String version,
        Instant timestamp,
        Map<String, Object> details
) {}
