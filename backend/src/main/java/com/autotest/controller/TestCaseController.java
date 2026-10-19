package com.autotest.controller;

import com.autotest.dto.TestCaseRequest;
import com.autotest.dto.TestCaseResponse;
import com.autotest.security.UserPrincipal;
import com.autotest.service.TestCaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Test Cases", description = "Endpoints for managing UI and API test cases")
@SecurityRequirement(name = "bearerAuth")
public class TestCaseController {

    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @GetMapping("/projects/{projectId}/test-cases")
    @Operation(summary = "Get paginated test cases belonging to a project")
    public ResponseEntity<Page<TestCaseResponse>> getTestCases(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<TestCaseResponse> testCases = testCaseService.getTestCases(projectId, currentUser.getId(), pageable);
        return ResponseEntity.ok(testCases);
    }

    @PostMapping("/projects/{projectId}/test-cases")
    @Operation(summary = "Create a new test case in a project")
    public ResponseEntity<TestCaseResponse> createTestCase(
            @PathVariable Long projectId,
            @Valid @RequestBody TestCaseRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        TestCaseResponse testCase = testCaseService.createTestCase(projectId, request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(testCase);
    }

    @GetMapping("/test-cases/{testCaseId}")
    @Operation(summary = "Get test case details by ID")
    public ResponseEntity<TestCaseResponse> getTestCaseById(
            @PathVariable Long testCaseId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        TestCaseResponse testCase = testCaseService.getTestCaseById(testCaseId, currentUser.getId());
        return ResponseEntity.ok(testCase);
    }

    @PutMapping("/test-cases/{testCaseId}")
    @Operation(summary = "Update an existing test case")
    public ResponseEntity<TestCaseResponse> updateTestCase(
            @PathVariable Long testCaseId,
            @Valid @RequestBody TestCaseRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        TestCaseResponse testCase = testCaseService.updateTestCase(testCaseId, request, currentUser.getId());
        return ResponseEntity.ok(testCase);
    }

    @DeleteMapping("/test-cases/{testCaseId}")
    @Operation(summary = "Delete a test case")
    public ResponseEntity<Void> deleteTestCase(
            @PathVariable Long testCaseId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        testCaseService.deleteTestCase(testCaseId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}
