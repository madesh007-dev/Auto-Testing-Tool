package com.autotest.execution.model;

import com.autotest.entity.TestType;

public record ExecutionTaskData(
        Long resultId,
        Long testCaseId,
        String testCaseName,
        TestType testType,
        String configuration
) {}
