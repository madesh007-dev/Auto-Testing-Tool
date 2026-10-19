package com.autotest.execution.model;

import com.autotest.entity.ExecutionStatus;

public record ExecutionOutcome(
        ExecutionStatus status,
        long durationMs,
        String errorMessage,
        String executionLog,
        String screenshotPath
) {}
