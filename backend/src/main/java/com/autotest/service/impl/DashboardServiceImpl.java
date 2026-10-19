package com.autotest.service.impl;

import com.autotest.dto.DashboardMetricsResponse;
import com.autotest.dto.ExecutionTrendPoint;
import com.autotest.entity.ExecutionStatus;
import com.autotest.mapper.ExecutionMapper;
import com.autotest.repository.TestCaseRepository;
import com.autotest.repository.TestResultRepository;
import com.autotest.repository.TestRunRepository;
import com.autotest.repository.TestingProjectRepository;
import com.autotest.service.DashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final int TREND_DAYS = 14;
    private final TestingProjectRepository projectRepository;
    private final TestCaseRepository testCaseRepository;
    private final TestRunRepository testRunRepository;
    private final TestResultRepository testResultRepository;
    private final ExecutionMapper executionMapper;

    public DashboardServiceImpl(
            TestingProjectRepository projectRepository,
            TestCaseRepository testCaseRepository,
            TestRunRepository testRunRepository,
            TestResultRepository testResultRepository,
            ExecutionMapper executionMapper
    ) {
        this.projectRepository = projectRepository;
        this.testCaseRepository = testCaseRepository;
        this.testRunRepository = testRunRepository;
        this.testResultRepository = testResultRepository;
        this.executionMapper = executionMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardMetricsResponse getMetrics(Long currentUserId) {
        Map<ExecutionStatus, Long> counts = new EnumMap<>(ExecutionStatus.class);
        for (Object[] row : testResultRepository.countByStatusForOwner(currentUserId)) {
            counts.put((ExecutionStatus) row[0], ((Number) row[1]).longValue());
        }

        long passed = count(counts, ExecutionStatus.PASSED);
        long failed = count(counts, ExecutionStatus.FAILED);
        long errors = count(counts, ExecutionStatus.ERROR);
        long skipped = count(counts, ExecutionStatus.SKIPPED);
        long executed = passed + failed + errors;

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate firstTrendDay = today.minusDays(TREND_DAYS - 1L);
        Map<LocalDate, long[]> trendCounts = new HashMap<>();
        for (Object[] row : testResultRepository.countCompletedByDayAndStatus(
                currentUserId, Timestamp.from(firstTrendDay.atStartOfDay(ZoneOffset.UTC).toInstant()))) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            ExecutionStatus status = ExecutionStatus.valueOf((String) row[1]);
            if (status == ExecutionStatus.SKIPPED) {
                continue;
            }
            long[] daily = trendCounts.computeIfAbsent(date, ignored -> new long[4]);
            daily[0] += ((Number) row[2]).longValue();
            switch (status) {
                case PASSED -> daily[1] += ((Number) row[2]).longValue();
                case FAILED -> daily[2] += ((Number) row[2]).longValue();
                case ERROR -> daily[3] += ((Number) row[2]).longValue();
                default -> { }
            }
        }

        List<ExecutionTrendPoint> trend = new ArrayList<>(TREND_DAYS);
        for (int day = 0; day < TREND_DAYS; day++) {
            LocalDate date = firstTrendDay.plusDays(day);
            long[] daily = trendCounts.getOrDefault(date, new long[4]);
            trend.add(new ExecutionTrendPoint(date, daily[0], daily[1], daily[2], daily[3]));
        }

        var recentRuns = testRunRepository.findAllByOwnerId(
                currentUserId,
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "startedAt"))
        ).map(executionMapper::toResponse).getContent();

        return new DashboardMetricsResponse(
                projectRepository.countByOwnerId(currentUserId),
                testCaseRepository.countByOwnerId(currentUserId),
                testRunRepository.countByProjectOwnerId(currentUserId),
                executed,
                passed,
                failed,
                errors,
                skipped,
                executed == 0 ? 0 : passed * 100.0 / executed,
                recentRuns,
                trend
        );
    }

    private long count(Map<ExecutionStatus, Long> counts, ExecutionStatus status) {
        return counts.getOrDefault(status, 0L);
    }
}
