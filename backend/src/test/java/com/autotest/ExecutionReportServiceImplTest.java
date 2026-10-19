package com.autotest;

import com.autotest.entity.ExecutionStatus;
import com.autotest.entity.TestCase;
import com.autotest.entity.TestResult;
import com.autotest.entity.TestRun;
import com.autotest.entity.TestType;
import com.autotest.entity.TestingProject;
import com.autotest.exception.ResourceNotFoundException;
import com.autotest.exception.BadRequestException;
import com.autotest.repository.TestResultRepository;
import com.autotest.repository.TestRunRepository;
import com.autotest.service.impl.ExecutionReportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionReportServiceImplTest {

    @Mock
    private TestRunRepository testRunRepository;
    @Mock
    private TestResultRepository testResultRepository;

    @Test
    void reportEscapesUserContentAndRedactsCommonSecrets() {
        TestingProject project = new TestingProject();
        project.setName("<FeastFlow>");
        TestRun run = new TestRun();
        run.setId(4L);
        run.setProject(project);
        run.setStatus(ExecutionStatus.FAILED);
        run.setTotalTests(3);
        run.setDurationMs(850L);

        TestCase testCase = new TestCase();
        testCase.setName("<script>alert(1)</script>");
        testCase.setTestType(TestType.UI_TEST);
        TestResult result = new TestResult();
        result.setId(9L);
        result.setTestRun(run);
        result.setTestCase(testCase);
        result.setStatus(ExecutionStatus.FAILED);
        result.setErrorMessage("<img src=x onerror=alert(1)>");
        result.setExecutionLog("Authorization: Bearer abc.def.ghi\npassword: hunter2\nvalue: 'secret input'");
        result.setScreenshotPath("target/screenshots/failure.png");

        when(testRunRepository.findByIdAndOwnerId(4L, 7L)).thenReturn(Optional.of(run));
        TestCase passingCase = new TestCase();
        passingCase.setName("Restaurant list renders");
        passingCase.setTestType(TestType.UI_TEST);
        TestResult passingResult = new TestResult();
        passingResult.setId(10L);
        passingResult.setTestRun(run);
        passingResult.setTestCase(passingCase);
        passingResult.setStatus(ExecutionStatus.PASSED);
        passingResult.setDurationMs(125L);
        TestCase skippedCase = new TestCase();
        skippedCase.setName("Skipped test");
        skippedCase.setTestType(TestType.UI_TEST);
        TestResult skippedResult = new TestResult();
        skippedResult.setTestRun(run);
        skippedResult.setTestCase(skippedCase);
        skippedResult.setStatus(ExecutionStatus.SKIPPED);
        when(testResultRepository.findByTestRunIdAndOwnerId(4L, 7L))
                .thenReturn(List.of(result, passingResult, skippedResult));
        ExecutionReportServiceImpl service = new ExecutionReportServiceImpl(testRunRepository, testResultRepository);

        String report = service.generateHtmlReport(4L, 7L);

        assertTrue(report.contains("&lt;FeastFlow&gt;"));
        assertTrue(report.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertTrue(report.contains("&lt;img src=x onerror=alert(1)&gt;"));
        assertTrue(report.contains("Authorization: [REDACTED]"));
        assertTrue(report.contains("password: [REDACTED]"));
        assertTrue(report.contains("value: &#39;[REDACTED]&#39;"));
        assertTrue(report.contains("authenticated screenshot endpoint"));
        assertTrue(report.contains("Failed</dt><dd>1</dd>"));
        assertTrue(report.contains("Passed</dt><dd>1</dd>"));
        assertTrue(report.contains("Total tests</dt><dd>3</dd>"));
        assertTrue(report.contains("Results recorded</dt><dd>3</dd>"));
        assertTrue(report.contains("Duration</dt><dd>850 ms</dd>"));
        assertTrue(report.contains("Pass rate</dt><dd>50.0%</dd>"));
        assertTrue(report.contains("125 ms"));
        assertTrue(report.contains("/api/v1/test-results/9/screenshot"));
        assertFalse(report.contains("hunter2"));
        assertFalse(report.contains("abc.def.ghi"));
        assertFalse(report.contains("secret input"));
        verify(testResultRepository).findByTestRunIdAndOwnerId(4L, 7L);
    }

    @Test
    void reportIncludesPersistedPassStatusAndExecutionTimestamps() {
        TestingProject project = new TestingProject();
        project.setName("FeastFlow");
        TestRun run = new TestRun();
        run.setId(12L);
        run.setProject(project);
        run.setStatus(ExecutionStatus.PASSED);
        run.setTotalTests(1);
        run.setDurationMs(1000L);
        run.setStartedAt(Instant.parse("2026-10-18T10:00:00Z"));
        run.setCompletedAt(Instant.parse("2026-10-18T10:00:01Z"));

        TestCase testCase = new TestCase();
        testCase.setName("Landing page");
        testCase.setTestType(TestType.UI_TEST);
        TestResult result = new TestResult();
        result.setTestRun(run);
        result.setTestCase(testCase);
        result.setStatus(ExecutionStatus.PASSED);
        result.setStartedAt(Instant.parse("2026-10-18T10:00:00Z"));
        result.setCompletedAt(Instant.parse("2026-10-18T10:00:01Z"));
        result.setDurationMs(1000L);

        when(testRunRepository.findByIdAndOwnerId(12L, 7L)).thenReturn(Optional.of(run));
        when(testResultRepository.findByTestRunIdAndOwnerId(12L, 7L)).thenReturn(List.of(result));
        ExecutionReportServiceImpl service = new ExecutionReportServiceImpl(testRunRepository, testResultRepository);

        String report = service.generateHtmlReport(12L, 7L);

        assertTrue(report.contains("FeastFlow"));
        assertTrue(report.contains("PASSED"));
        assertTrue(report.contains("2026-10-18T10:00:00Z"));
        assertTrue(report.contains("2026-10-18T10:00:01Z"));
        assertTrue(report.contains("Passed</dt><dd>1</dd>"));
        assertTrue(report.contains("1000 ms"));
        assertTrue(report.contains("Duration</dt><dd>1000 ms</dd>"));
    }

    @Test
    void reportDoesNotExposeRunsOwnedByAnotherUser() {
        when(testRunRepository.findByIdAndOwnerId(4L, 7L)).thenReturn(Optional.empty());
        ExecutionReportServiceImpl service = new ExecutionReportServiceImpl(testRunRepository, testResultRepository);

        assertThrows(ResourceNotFoundException.class, () -> service.generateHtmlReport(4L, 7L));
    }

    @Test
    void reportIsUnavailableWhileRunIsStillExecuting() {
        TestRun run = new TestRun();
        run.setId(4L);
        run.setStatus(ExecutionStatus.RUNNING);
        when(testRunRepository.findByIdAndOwnerId(4L, 7L)).thenReturn(Optional.of(run));
        ExecutionReportServiceImpl service = new ExecutionReportServiceImpl(testRunRepository, testResultRepository);

        assertThrows(BadRequestException.class, () -> service.generateHtmlReport(4L, 7L));
    }
}
