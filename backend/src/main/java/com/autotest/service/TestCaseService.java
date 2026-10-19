package com.autotest.service;

import com.autotest.dto.TestCaseRequest;
import com.autotest.dto.TestCaseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TestCaseService {
    Page<TestCaseResponse> getTestCases(Long projectId, Long currentUserId, Pageable pageable);
    TestCaseResponse getTestCaseById(Long testCaseId, Long currentUserId);
    TestCaseResponse createTestCase(Long projectId, TestCaseRequest request, Long currentUserId);
    TestCaseResponse updateTestCase(Long testCaseId, TestCaseRequest request, Long currentUserId);
    void deleteTestCase(Long testCaseId, Long currentUserId);
}
