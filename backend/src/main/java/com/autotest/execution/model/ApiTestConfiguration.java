package com.autotest.execution.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ApiTestConfiguration(
        @NotBlank(message = "HTTP method is required")
        String method,

        @NotBlank(message = "Endpoint path is required")
        String endpoint,

        Map<String, String> headers,

        String requestBody,

        @NotNull(message = "Expected status is required")
        Integer expectedStatus,

        Map<String, Object> jsonAssertions,

        Integer timeoutMs,

        String apiBaseUrl
) {}
