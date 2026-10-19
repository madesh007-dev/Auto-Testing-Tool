package com.autotest.dto;

import com.autotest.entity.TestType;
import java.time.Instant;

public record TestCaseResponse(
        Long id,
        String name,
        String description,
        TestType testType,
        String configuration,
        Long projectId,
        String projectName,
        Instant createdAt,
        Instant updatedAt
) {}
