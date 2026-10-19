package com.autotest.controller;

import com.autotest.dto.TestResultResponse;
import com.autotest.dto.TestRunResponse;
import com.autotest.entity.ExecutionStatus;
import com.autotest.security.UserPrincipal;
import com.autotest.service.ExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Test Execution", description = "Endpoints for triggering test runs and inspecting test results")
@SecurityRequirement(name = "bearerAuth")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/projects/{projectId}/test-runs")
    @Operation(summary = "Trigger a test execution run for all test cases in a project")
    public ResponseEntity<TestRunResponse> triggerTestRun(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        TestRunResponse response = executionService.triggerTestRun(projectId, currentUser.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/test-cases/{testCaseId}/test-runs")
    @Operation(summary = "Trigger a test execution run for one test case")
    public ResponseEntity<TestRunResponse> triggerTestCaseRun(
            @PathVariable Long testCaseId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        TestRunResponse response = executionService.triggerTestCaseRun(testCaseId, currentUser.getId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/projects/{projectId}/test-runs")
    @Operation(summary = "Get historical test runs for a project")
    public ResponseEntity<Page<TestRunResponse>> getTestRuns(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PageableDefault(size = 10, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TestRunResponse> runs = executionService.getTestRunsByProject(projectId, currentUser.getId(), pageable);
        return ResponseEntity.ok(runs);
    }

    @GetMapping("/test-runs")
    @Operation(summary = "Get the authenticated user's filtered test run history")
    public ResponseEntity<Page<TestRunResponse>> getTestRunHistory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) ExecutionStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "startedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(executionService.getTestRunHistory(
                currentUser.getId(), projectId, status, from, to, pageable
        ));
    }

    @GetMapping("/test-runs/{testRunId}")
    @Operation(summary = "Get test run details by ID")
    public ResponseEntity<TestRunResponse> getTestRunById(
            @PathVariable Long testRunId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        TestRunResponse run = executionService.getTestRunById(testRunId, currentUser.getId());
        return ResponseEntity.ok(run);
    }

    @GetMapping("/test-runs/{testRunId}/results")
    @Operation(summary = "Get detailed execution results and logs for a test run")
    public ResponseEntity<List<TestResultResponse>> getTestResults(
            @PathVariable Long testRunId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        List<TestResultResponse> results = executionService.getTestResultsByRun(testRunId, currentUser.getId());
        return ResponseEntity.ok(results);
    }
}
