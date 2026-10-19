package com.autotest.service.impl;

import com.autotest.entity.ExecutionStatus;
import com.autotest.entity.TestResult;
import com.autotest.entity.TestRun;
import com.autotest.entity.TestType;
import com.autotest.execution.api.RestApiExecutor;
import com.autotest.execution.model.ExecutionOutcome;
import com.autotest.execution.model.ExecutionTaskData;
import com.autotest.execution.ui.PlaywrightUiExecutor;
import com.autotest.repository.TestResultRepository;
import com.autotest.repository.TestRunRepository;
import com.autotest.service.TestExecutionRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class TestExecutionRunnerImpl implements TestExecutionRunner {

    private static final Logger log = LoggerFactory.getLogger(TestExecutionRunnerImpl.class);

    private final TestRunRepository testRunRepository;
    private final TestResultRepository testResultRepository;
    private final PlaywrightUiExecutor playwrightUiExecutor;
    private final RestApiExecutor restApiExecutor;

    public TestExecutionRunnerImpl(
            TestRunRepository testRunRepository,
            TestResultRepository testResultRepository,
            PlaywrightUiExecutor playwrightUiExecutor,
            RestApiExecutor restApiExecutor
    ) {
        this.testRunRepository = testRunRepository;
        this.testResultRepository = testResultRepository;
        this.playwrightUiExecutor = playwrightUiExecutor;
        this.restApiExecutor = restApiExecutor;
    }

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeTestSuite(Long testRunId, List<ExecutionTaskData> tasks, String baseUrl, long startedAtNanos) {
        log.info("Starting execution of TestRun ID: {} with {} test cases", testRunId, tasks.size());
        int passed = 0;
        int failed = 0;
        int errors = 0;
        int skipped = 0;

        for (ExecutionTaskData task : tasks) {
            TestResult result = testResultRepository.findById(task.resultId()).orElse(null);
            if (result == null) {
                log.warn("TestResult with id {} not found, skipping", task.resultId());
                errors++;
                continue;
            }

            result.setStatus(ExecutionStatus.RUNNING);
            result.setStartedAt(Instant.now());
            testResultRepository.saveAndFlush(result);
            long resultStartedAtNanos = System.nanoTime();

            ExecutionOutcome outcome;
            try {
                if (task.testType() == TestType.UI_TEST) {
                    outcome = playwrightUiExecutor.execute(task.configuration(), baseUrl);
                } else {
                    outcome = restApiExecutor.execute(task.configuration());
                }
            } catch (Exception e) {
                log.error("Execution error on test case: {}", task.testCaseName(), e);
                outcome = new ExecutionOutcome(ExecutionStatus.ERROR, 0, e.getMessage(), "System error: " + e.getMessage(), null);
            }

            if (outcome == null || outcome.status() == null
                    || outcome.status() == ExecutionStatus.PENDING
                    || outcome.status() == ExecutionStatus.RUNNING) {
                String message = "Executor returned a non-terminal result status.";
                long elapsedNanos = System.nanoTime() - resultStartedAtNanos;
                long durationMs = Math.max(1, (elapsedNanos + 999_999) / 1_000_000);
                outcome = new ExecutionOutcome(ExecutionStatus.ERROR, durationMs, message, "System error: " + message, null);
            }

            result.setStatus(outcome.status());
            result.setDurationMs(outcome.durationMs());
            result.setErrorMessage(outcome.errorMessage());
            result.setExecutionLog(outcome.executionLog());
            result.setScreenshotPath(outcome.screenshotPath());
            result.setCompletedAt(Instant.now());
            testResultRepository.saveAndFlush(result);

            if (outcome.status() == ExecutionStatus.PASSED) {
                passed++;
            } else if (outcome.status() == ExecutionStatus.FAILED) {
                failed++;
            } else if (outcome.status() == ExecutionStatus.ERROR) {
                errors++;
            } else if (outcome.status() == ExecutionStatus.SKIPPED) {
                skipped++;
            }
        }

        TestRun finalRun = testRunRepository.findById(testRunId).orElse(null);
        if (finalRun != null) {
            finalRun.setCompletedAt(Instant.now());
            long elapsedNanos = System.nanoTime() - startedAtNanos;
            finalRun.setDurationMs(Math.max(1, (elapsedNanos + 999_999) / 1_000_000));
            finalRun.setPassedTests(passed);
            finalRun.setFailedTests(failed);
            finalRun.setErrorTests(errors);
            finalRun.setSkippedTests(skipped);
            finalRun.setStatus(failed > 0 ? ExecutionStatus.FAILED
                    : errors > 0 ? ExecutionStatus.ERROR
                    : skipped > 0 ? ExecutionStatus.SKIPPED : ExecutionStatus.PASSED);
            testRunRepository.saveAndFlush(finalRun);
            log.info("Finished TestRun ID: {} -> Passed: {}, Failed: {}, Errors: {}", testRunId, passed, failed, errors);
        }
    }
}
