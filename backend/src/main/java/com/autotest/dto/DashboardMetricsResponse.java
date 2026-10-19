package com.autotest.dto;

import java.util.List;

public record DashboardMetricsResponse(
        long totalProjects,
        long totalTestCases,
        long totalTestRuns,
        long totalExecutedTests,
        long passedTests,
        long failedTests,
        long errorTests,
        long skippedTests,
        double passRate,
        List<TestRunResponse> recentExecutions,
        List<ExecutionTrendPoint> executionTrend
) {}
