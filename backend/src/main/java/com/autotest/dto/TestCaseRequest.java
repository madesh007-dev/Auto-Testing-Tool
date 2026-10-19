package com.autotest.dto;

import com.autotest.entity.TestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TestCaseRequest(
        @NotBlank(message = "Test case name is required")
        @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
        String name,

        String description,

        @NotNull(message = "Test type is required (UI_TEST or API_TEST)")
        TestType testType,

        @NotBlank(message = "Configuration JSON is required")
        String configuration
) {}
