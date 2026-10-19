package com.autotest;

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
import com.autotest.service.impl.TestExecutionRunnerImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestExecutionRunnerImplTest {

    @Mock
    private TestRunRepository testRunRepository;

    @Mock
    private TestResultRepository testResultRepository;

    @Mock
    private PlaywrightUiExecutor playwrightUiExecutor;

    @Mock
    private RestApiExecutor restApiExecutor;

    @InjectMocks
    private TestExecutionRunnerImpl runner;

    @Test
    void recordsExecutorErrorsAsErrorsRatherThanPasses() {
        TestResult result = new TestResult();
        TestRun run = new TestRun();
        ExecutionTaskData task = new ExecutionTaskData(2L, 3L, "API test", TestType.API_TEST, "{}");
        when(testResultRepository.findById(2L)).thenReturn(Optional.of(result));
        when(testRunRepository.findById(1L)).thenReturn(Optional.of(run));
        when(restApiExecutor.execute("{}"))
                .thenReturn(new ExecutionOutcome(ExecutionStatus.ERROR, 12, "Connection refused", "Request failed", null));

        runner.executeTestSuite(1L, List.of(task), "http://example.test", System.nanoTime());

        assertEquals(ExecutionStatus.ERROR, result.getStatus());
        assertEquals("Connection refused", result.getErrorMessage());
        assertEquals(ExecutionStatus.ERROR, run.getStatus());
        assertEquals(1, run.getErrorTests());
        assertTrue(run.getDurationMs() > 0);
    }

    @Test
    void missingPersistedResultIsRecordedAsRunErrorRatherThanSuccess() {
        TestRun run = new TestRun();
        run.setTotalTests(1);
        ExecutionTaskData task = new ExecutionTaskData(2L, 3L, "Missing result", TestType.API_TEST, "{}");
        when(testResultRepository.findById(2L)).thenReturn(Optional.empty());
        when(testRunRepository.findById(1L)).thenReturn(Optional.of(run));

        runner.executeTestSuite(1L, List.of(task), "http://example.test", System.nanoTime());

        assertEquals(ExecutionStatus.ERROR, run.getStatus());
        assertEquals(1, run.getErrorTests());
        assertEquals(0, run.getPassedTests());
        assertTrue(run.getDurationMs() > 0);
        verify(restApiExecutor, never()).execute("{}");
    }

    @Test
    void recordsNonTerminalExecutorOutcomesAsErrors() {
        TestResult result = new TestResult();
        TestRun run = new TestRun();
        ExecutionTaskData task = new ExecutionTaskData(2L, 3L, "API test", TestType.API_TEST, "{}");
        when(testResultRepository.findById(2L)).thenReturn(Optional.of(result));
        when(testRunRepository.findById(1L)).thenReturn(Optional.of(run));
        when(restApiExecutor.execute("{}"))
                .thenReturn(new ExecutionOutcome(ExecutionStatus.RUNNING, 0, null, "Incomplete", null));

        runner.executeTestSuite(1L, List.of(task), "http://example.test", System.nanoTime());

        assertEquals(ExecutionStatus.ERROR, result.getStatus());
        assertEquals(ExecutionStatus.ERROR, run.getStatus());
        assertEquals(1, run.getErrorTests());
        assertTrue(result.getErrorMessage().contains("non-terminal"));
    }
}
