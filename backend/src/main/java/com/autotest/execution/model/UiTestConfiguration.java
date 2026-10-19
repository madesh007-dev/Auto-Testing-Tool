package com.autotest.execution.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UiTestConfiguration(
        String baseUrl,
        @NotEmpty(message = "Actions list cannot be empty")
        List<@Valid UiAction> actions
) {}
