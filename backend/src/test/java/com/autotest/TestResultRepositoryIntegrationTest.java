package com.autotest;

import com.autotest.entity.ExecutionStatus;
import com.autotest.entity.Role;
import com.autotest.entity.TestCase;
import com.autotest.entity.TestResult;
import com.autotest.entity.TestRun;
import com.autotest.entity.TestType;
import com.autotest.entity.TestingProject;
import com.autotest.entity.User;
import com.autotest.repository.TestCaseRepository;
import com.autotest.repository.TestResultRepository;
import com.autotest.repository.TestRunRepository;
import com.autotest.repository.TestingProjectRepository;
import com.autotest.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TestResultRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestingProjectRepository projectRepository;
    @Autowired
    private TestCaseRepository testCaseRepository;
    @Autowired
    private TestRunRepository testRunRepository;
    @Autowired
    private TestResultRepository testResultRepository;

    @Test
    void dailyMetricsQueryGroupsPersistedResultsAndExcludesSkippedTests() {
        User user = userRepository.saveAndFlush(
                new User("trend-user", "trend-user@example.com", "not-a-real-password", Role.ROLE_USER)
        );
        TestingProject project = projectRepository.saveAndFlush(
                new TestingProject("Metrics project", "", "http://example.test", user)
        );
        TestCase testCase = testCaseRepository.saveAndFlush(
                new TestCase("Metrics test", "", TestType.API_TEST, "{}", project)
        );
        Instant now = Instant.now();
        TestRun run = new TestRun(project, user);
        run.setStartedAt(now);
        run.setCompletedAt(now);
        run.setStatus(ExecutionStatus.FAILED);
        run.setTotalTests(3);
        run.setPassedTests(1);
        run.setFailedTests(1);
        run.setSkippedTests(1);
        run = testRunRepository.saveAndFlush(run);

        saveResult(run, testCase, ExecutionStatus.PASSED, now);
        saveResult(run, testCase, ExecutionStatus.FAILED, now);
        saveResult(run, testCase, ExecutionStatus.SKIPPED, now);

        List<Object[]> rows = testResultRepository.countCompletedByDayAndStatus(
                user.getId(),
                Timestamp.valueOf(LocalDate.now().minusDays(13).atStartOfDay())
        );

        assertFalse(rows.isEmpty());
        assertEquals(LocalDate.now(), ((java.sql.Date) rows.get(0)[0]).toLocalDate());
        assertEquals("FAILED", rows.get(0)[1]);
        assertEquals(1L, ((Number) rows.get(0)[2]).longValue());
        assertEquals(2, rows.size());
    }

    private void saveResult(TestRun run, TestCase testCase, ExecutionStatus status, Instant completedAt) {
        TestResult result = new TestResult(run, testCase);
        result.setStatus(status);
        result.setStartedAt(completedAt);
        result.setCompletedAt(completedAt);
        result.setDurationMs(5L);
        testResultRepository.saveAndFlush(result);
    }
}
