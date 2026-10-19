package com.autotest.mapper;

import com.autotest.dto.TestResultResponse;
import com.autotest.dto.TestRunResponse;
import com.autotest.entity.TestResult;
import com.autotest.entity.TestRun;
import org.springframework.stereotype.Component;

@Component
public class ExecutionMapper {

    public TestRunResponse toResponse(TestRun run) {
        if (run == null) return null;

        return new TestRunResponse(
                run.getId(),
                run.getProject() != null ? run.getProject().getId() : null,
                run.getProject() != null ? run.getProject().getName() : null,
                run.getStatus(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getDurationMs(),
                run.getTotalTests(),
                run.getPassedTests(),
                run.getFailedTests(),
                run.getErrorTests(),
                run.getSkippedTests(),
                run.getTriggeredBy() != null ? run.getTriggeredBy().getId() : null,
                run.getTriggeredBy() != null ? run.getTriggeredBy().getUsername() : null
        );
    }

    public TestResultResponse toResponse(TestResult result) {
        if (result == null) return null;

        return new TestResultResponse(
                result.getId(),
                result.getTestRun() != null ? result.getTestRun().getId() : null,
                result.getTestCase() != null ? result.getTestCase().getId() : null,
                result.getTestCase() != null ? result.getTestCase().getName() : null,
                result.getTestCase() != null ? result.getTestCase().getTestType().name() : null,
                result.getStatus(),
                result.getStartedAt(),
                result.getCompletedAt(),
                result.getDurationMs(),
                result.getErrorMessage(),
                result.getExecutionLog(),
                result.getScreenshotPath() == null ? null
                        : "/api/v1/test-results/" + result.getId() + "/screenshot"
        );
    }
}
