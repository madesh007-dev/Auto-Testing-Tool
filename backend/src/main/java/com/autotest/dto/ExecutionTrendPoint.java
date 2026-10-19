package com.autotest.dto;

import java.time.LocalDate;

public record ExecutionTrendPoint(
        LocalDate date,
        long executedTests,
        long passedTests,
        long failedTests,
        long errorTests
) {}
