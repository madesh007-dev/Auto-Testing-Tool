package com.autotest.execution.model;

import jakarta.validation.constraints.NotNull;

public record UiAction(
        @NotNull(message = "Action type is required")
        ActionType type,

        String selector,
        String value,
        Integer timeoutMs
) {}
