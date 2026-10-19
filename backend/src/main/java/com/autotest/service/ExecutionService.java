package com.autotest.service;

import com.autotest.dto.TestResultResponse;
import com.autotest.dto.TestRunResponse;
import com.autotest.entity.ExecutionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.nio.file.Path;
import java.util.List;

public interface ExecutionService {
    TestRunResponse triggerTestRun(Long projectId, Long currentUserId);
    TestRunResponse triggerTestCaseRun(Long testCaseId, Long currentUserId);
    Page<TestRunResponse> getTestRunHistory(Long currentUserId, Long projectId, ExecutionStatus status,
                                             Instant from, Instant to, Pageable pageable);
    Page<TestRunResponse> getTestRunsByProject(Long projectId, Long currentUserId, Pageable pageable);
    TestRunResponse getTestRunById(Long testRunId, Long currentUserId);
    List<TestResultResponse> getTestResultsByRun(Long testRunId, Long currentUserId);
    Path getScreenshotPath(Long testResultId, Long currentUserId);
}
