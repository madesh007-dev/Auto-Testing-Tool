package com.autotest.service;

import com.autotest.execution.model.ExecutionTaskData;

import java.util.List;

public interface TestExecutionRunner {
    void executeTestSuite(Long testRunId, List<ExecutionTaskData> tasks, String baseUrl, long startedAtNanos);
}
