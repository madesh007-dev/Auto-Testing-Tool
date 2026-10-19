package com.autotest.service;

import com.autotest.dto.DashboardMetricsResponse;

public interface DashboardService {
    DashboardMetricsResponse getMetrics(Long currentUserId);
}
