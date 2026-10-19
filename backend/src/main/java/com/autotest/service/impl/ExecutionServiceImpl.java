package com.autotest.service.impl;

import com.autotest.dto.TestResultResponse;
import com.autotest.dto.TestRunResponse;
import com.autotest.entity.*;
import com.autotest.exception.BadRequestException;
import com.autotest.exception.ResourceNotFoundException;
import com.autotest.execution.model.ExecutionTaskData;
import com.autotest.mapper.ExecutionMapper;
import com.autotest.repository.*;
import com.autotest.service.ExecutionService;
import com.autotest.service.TestExecutionRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExecutionServiceImpl implements ExecutionService {

    private final TestingProjectRepository projectRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestRunRepository testRunRepository;
    private final TestResultRepository testResultRepository;
    private final UserRepository userRepository;
    private final TestExecutionRunner testExecutionRunner;
    private final ExecutionMapper executionMapper;

    public ExecutionServiceImpl(
            TestingProjectRepository projectRepository,
            TestCaseRepository testCaseRepository,
            TestRunRepository testRunRepository,
            TestResultRepository testResultRepository,
            UserRepository userRepository,
            TestExecutionRunner testExecutionRunner,
            ExecutionMapper executionMapper
    ) {
        this.projectRepository = projectRepository;
        this.testCaseRepository = testCaseRepository;
        this.testRunRepository = testRunRepository;
        this.testResultRepository = testResultRepository;
        this.userRepository = userRepository;
        this.testExecutionRunner = testExecutionRunner;
        this.executionMapper = executionMapper;
    }

    @Override
    @Transactional
    public TestRunResponse triggerTestRun(Long projectId, Long currentUserId) {
        TestingProject project = projectRepository.findByIdAndOwnerId(projectId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        List<TestCase> testCases = testCaseRepository.findAllByProjectIdAndOwnerId(projectId, currentUserId);
        if (testCases.isEmpty()) {
            throw new BadRequestException("Cannot trigger test run: No test cases found in project.");
        }

        return createTestRun(project, user, testCases);
    }

    @Override
    @Transactional
    public TestRunResponse triggerTestCaseRun(Long testCaseId, Long currentUserId) {
        TestCase testCase = testCaseRepository.findByIdAndOwnerId(testCaseId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Test case not found with id: " + testCaseId));
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        return createTestRun(testCase.getProject(), user, List.of(testCase));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TestRunResponse> getTestRunHistory(
            Long currentUserId,
            Long projectId,
            ExecutionStatus status,
            Instant from,
            Instant to,
            Pageable pageable
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("The start date must be before or equal to the end date.");
        }
        Pageable boundedPageable = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 100),
                pageable.getSortOr(Sort.by(Sort.Direction.DESC, "startedAt"))
        );
        return testRunRepository.findHistoryByOwnerId(
                        currentUserId, projectId, status, from, to, boundedPageable
                )
                .map(executionMapper::toResponse);
    }

    private TestRunResponse createTestRun(TestingProject project, User user, List<TestCase> testCases) {
        long startedAtNanos = System.nanoTime();
        TestRun testRun = new TestRun(project, user);
        testRun.setStatus(ExecutionStatus.RUNNING);
        testRun.setStartedAt(Instant.now());
        testRun.setTotalTests(testCases.size());
        TestRun savedRun = testRunRepository.saveAndFlush(testRun);

        List<ExecutionTaskData> tasks = new ArrayList<>();
        for (TestCase tc : testCases) {
            TestResult tr = new TestResult(savedRun, tc);
            tr.setStatus(ExecutionStatus.PENDING);
            TestResult savedResult = testResultRepository.saveAndFlush(tr);
            tasks.add(new ExecutionTaskData(
                    savedResult.getId(),
                    tc.getId(),
                    tc.getName(),
                    tc.getTestType(),
                    tc.getConfiguration()
            ));
        }

        String baseUrl = project.getBaseUrl();
        List<ExecutionTaskData> executionTasks = List.copyOf(tasks);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                testExecutionRunner.executeTestSuite(savedRun.getId(), executionTasks, baseUrl, startedAtNanos);
            }
        });

        return executionMapper.toResponse(savedRun);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TestRunResponse> getTestRunsByProject(Long projectId, Long currentUserId, Pageable pageable) {
        if (!projectRepository.existsByIdAndOwnerId(projectId, currentUserId)) {
            throw new ResourceNotFoundException("Project not found with id: " + projectId);
        }
        return testRunRepository.findByProjectIdAndOwnerId(projectId, currentUserId, pageable)
                .map(executionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TestRunResponse getTestRunById(Long testRunId, Long currentUserId) {
        TestRun testRun = testRunRepository.findByIdAndOwnerId(testRunId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Test run not found with id: " + testRunId));

        return executionMapper.toResponse(testRun);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestResultResponse> getTestResultsByRun(Long testRunId, Long currentUserId) {
        if (testRunRepository.findByIdAndOwnerId(testRunId, currentUserId).isEmpty()) {
            throw new ResourceNotFoundException("Test run not found with id: " + testRunId);
        }

        return testResultRepository.findByTestRunIdAndOwnerId(testRunId, currentUserId)
                .stream()
                .map(executionMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Path getScreenshotPath(Long testResultId, Long currentUserId) {
        TestResult result = testResultRepository.findByIdAndOwnerId(testResultId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Screenshot not found."));
        if (result.getScreenshotPath() == null || result.getScreenshotPath().isBlank()) {
            throw new ResourceNotFoundException("Screenshot not found.");
        }

        Path screenshotRoot = Paths.get("target", "screenshots").toAbsolutePath().normalize();
        try {
            Path savedPath = Paths.get(result.getScreenshotPath()).toAbsolutePath().normalize();
            if (!savedPath.startsWith(screenshotRoot)
                    || !savedPath.getFileName().toString().toLowerCase().endsWith(".png")) {
                throw new ResourceNotFoundException("Screenshot not found.");
            }
            Path realRoot = screenshotRoot.toRealPath();
            Path realScreenshot = savedPath.toRealPath();
            if (!realScreenshot.startsWith(realRoot) || !Files.isRegularFile(realScreenshot)) {
                throw new ResourceNotFoundException("Screenshot not found.");
            }
            return realScreenshot;
        } catch (IOException | java.nio.file.InvalidPathException e) {
            throw new ResourceNotFoundException("Screenshot not found.");
        }
    }
}
