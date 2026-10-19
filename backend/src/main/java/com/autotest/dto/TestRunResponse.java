package com.autotest.dto;

import com.autotest.entity.ExecutionStatus;
import java.time.Instant;

public record TestRunResponse(
        Long id,
        Long projectId,
        String projectName,
        ExecutionStatus status,
        Instant startedAt,
        Instant completedAt,
        long durationMs,
        int totalTests,
        int passedTests,
        int failedTests,
        int errorTests,
        int skippedTests,
        Long triggeredById,
        String triggeredByUsername
) {}
