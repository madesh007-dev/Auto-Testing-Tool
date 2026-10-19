package com.autotest;

import com.autotest.entity.ExecutionStatus;
import com.autotest.mapper.ExecutionMapper;
import com.autotest.repository.TestCaseRepository;
import com.autotest.repository.TestResultRepository;
import com.autotest.repository.TestRunRepository;
import com.autotest.repository.TestingProjectRepository;
import com.autotest.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private TestingProjectRepository projectRepository;
    @Mock
    private TestCaseRepository testCaseRepository;
    @Mock
    private TestRunRepository testRunRepository;
    @Mock
    private TestResultRepository testResultRepository;
    @Mock
    private ExecutionMapper executionMapper;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Test
    void returnsZeroMetricsAndTrendForAnEmptyAccount() {
        when(testResultRepository.countByStatusForOwner(7L)).thenReturn(List.of());
        when(testResultRepository.countCompletedByDayAndStatus(any(), any())).thenReturn(List.of());
        when(testRunRepository.findAllByOwnerId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(projectRepository.countByOwnerId(7L)).thenReturn(0L);
        when(testCaseRepository.countByOwnerId(7L)).thenReturn(0L);
        when(testRunRepository.countByProjectOwnerId(7L)).thenReturn(0L);

        var metrics = dashboardService.getMetrics(7L);

        assertEquals(0, metrics.totalProjects());
        assertEquals(0, metrics.totalTestCases());
        assertEquals(0, metrics.totalTestRuns());
        assertEquals(0, metrics.totalExecutedTests());
        assertEquals(0, metrics.passRate());
        assertEquals(List.of(), metrics.recentExecutions());
        assertEquals(14, metrics.executionTrend().size());
    }

    @Test
    void countsTerminalResultStatusesSeparatelyAndExcludesRunningTests() {
        when(testResultRepository.countByStatusForOwner(7L)).thenReturn(List.of(
                new Object[]{ExecutionStatus.PASSED, 3L},
                new Object[]{ExecutionStatus.FAILED, 1L},
                new Object[]{ExecutionStatus.ERROR, 2L},
                new Object[]{ExecutionStatus.SKIPPED, 1L},
                new Object[]{ExecutionStatus.RUNNING, 4L}
        ));
        when(testResultRepository.countCompletedByDayAndStatus(any(), any())).thenReturn(List.of());
        when(testRunRepository.findAllByOwnerId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(projectRepository.countByOwnerId(7L)).thenReturn(2L);
        when(testCaseRepository.countByOwnerId(7L)).thenReturn(11L);
        when(testRunRepository.countByProjectOwnerId(7L)).thenReturn(5L);

        var metrics = dashboardService.getMetrics(7L);

        assertEquals(2, metrics.totalProjects());
        assertEquals(11, metrics.totalTestCases());
        assertEquals(5, metrics.totalTestRuns());
        assertEquals(6, metrics.totalExecutedTests());
        assertEquals(3, metrics.passedTests());
        assertEquals(1, metrics.failedTests());
        assertEquals(2, metrics.errorTests());
        assertEquals(1, metrics.skippedTests());
        assertEquals(50.0, metrics.passRate());
    }

    @Test
    void mapsDailyExecutionTrendRowsToTheCorrectDayAndStatus() {
        LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
        when(testResultRepository.countByStatusForOwner(7L)).thenReturn(List.of());
        when(testResultRepository.countCompletedByDayAndStatus(any(), any())).thenReturn(List.of(
                new Object[]{java.sql.Date.valueOf(today), "PASSED", 2L},
                new Object[]{java.sql.Date.valueOf(today), "ERROR", 1L},
                new Object[]{java.sql.Date.valueOf(today), "SKIPPED", 4L}
        ));
        when(testRunRepository.findAllByOwnerId(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(projectRepository.countByOwnerId(7L)).thenReturn(0L);
        when(testCaseRepository.countByOwnerId(7L)).thenReturn(0L);
        when(testRunRepository.countByProjectOwnerId(7L)).thenReturn(0L);

        var metrics = dashboardService.getMetrics(7L);
        var todayPoint = metrics.executionTrend().get(metrics.executionTrend().size() - 1);

        assertEquals(today, todayPoint.date());
        assertEquals(3, todayPoint.executedTests());
        assertEquals(2, todayPoint.passedTests());
        assertEquals(1, todayPoint.errorTests());
    }
}
