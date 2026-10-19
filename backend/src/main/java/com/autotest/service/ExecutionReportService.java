package com.autotest.service;

public interface ExecutionReportService {
    String generateHtmlReport(Long testRunId, Long currentUserId);
}
