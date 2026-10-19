package com.autotest.dto;

import com.autotest.entity.ExecutionStatus;
import java.time.Instant;

public record TestResultResponse(
        Long id,
        Long testRunId,
        Long testCaseId,
        String testCaseName,
        String testType,
        ExecutionStatus status,
        Instant startedAt,
        Instant completedAt,
        long durationMs,
        String errorMessage,
        String executionLog,
        String screenshotUrl
) {}
