package com.autotest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProjectRequest(
        @NotBlank(message = "Project name is required")
        @Size(min = 2, max = 100, message = "Project name must be between 2 and 100 characters")
        String name,

        String description,

        @NotBlank(message = "Base URL is required")
        @Pattern(
                regexp = "^https?://.+",
                message = "Base URL must start with http:// or https://"
        )
        String baseUrl
) {}
