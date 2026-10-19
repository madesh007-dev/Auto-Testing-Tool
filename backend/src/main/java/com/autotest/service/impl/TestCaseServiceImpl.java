package com.autotest.service.impl;

import com.autotest.dto.TestCaseRequest;
import com.autotest.dto.TestCaseResponse;
import com.autotest.entity.TestCase;
import com.autotest.entity.TestingProject;
import com.autotest.exception.ResourceNotFoundException;
import com.autotest.mapper.TestCaseMapper;
import com.autotest.repository.TestCaseRepository;
import com.autotest.repository.TestingProjectRepository;
import com.autotest.service.TestCaseService;
import com.autotest.validation.TestConfigurationValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestCaseServiceImpl implements TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final TestingProjectRepository projectRepository;
    private final TestCaseMapper testCaseMapper;
    private final TestConfigurationValidator configurationValidator;

    public TestCaseServiceImpl(
            TestCaseRepository testCaseRepository,
            TestingProjectRepository projectRepository,
            TestCaseMapper testCaseMapper,
            TestConfigurationValidator configurationValidator
    ) {
        this.testCaseRepository = testCaseRepository;
        this.projectRepository = projectRepository;
        this.testCaseMapper = testCaseMapper;
        this.configurationValidator = configurationValidator;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TestCaseResponse> getTestCases(Long projectId, Long currentUserId, Pageable pageable) {
        // Verify project belongs to user
        if (!projectRepository.existsByIdAndOwnerId(projectId, currentUserId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }

        return testCaseRepository.findByProjectIdAndOwnerId(projectId, currentUserId, pageable)
                .map(testCaseMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TestCaseResponse getTestCaseById(Long testCaseId, Long currentUserId) {
        TestCase testCase = testCaseRepository.findByIdAndOwnerId(testCaseId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + testCaseId));

        return testCaseMapper.toResponse(testCase);
    }

    @Override
    @Transactional
    public TestCaseResponse createTestCase(Long projectId, TestCaseRequest request, Long currentUserId) {
        TestingProject project = projectRepository.findByIdAndOwnerId(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        // Validate JSON configuration structure and parameters
        configurationValidator.validate(request.testType(), request.configuration());

        TestCase testCase = new TestCase(
                request.name(),
                request.description(),
                request.testType(),
                request.configuration(),
                project
        );

        TestCase savedTestCase = testCaseRepository.save(testCase);
        return testCaseMapper.toResponse(savedTestCase);
    }

    @Override
    @Transactional
    public TestCaseResponse updateTestCase(Long testCaseId, TestCaseRequest request, Long currentUserId) {
        TestCase testCase = testCaseRepository.findByIdAndOwnerId(testCaseId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + testCaseId));

        // Validate updated JSON configuration
        configurationValidator.validate(request.testType(), request.configuration());

        testCase.setName(request.name());
        testCase.setDescription(request.description());
        testCase.setTestType(request.testType());
        testCase.setConfiguration(request.configuration());

        TestCase updatedTestCase = testCaseRepository.save(testCase);
        return testCaseMapper.toResponse(updatedTestCase);
    }

    @Override
    @Transactional
    public void deleteTestCase(Long testCaseId, Long currentUserId) {
        TestCase testCase = testCaseRepository.findByIdAndOwnerId(testCaseId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + testCaseId));

        testCaseRepository.delete(testCase);
    }
}
