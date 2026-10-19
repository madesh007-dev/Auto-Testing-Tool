package com.autotest.service.impl;

import com.autotest.entity.ExecutionStatus;
import com.autotest.entity.TestResult;
import com.autotest.entity.TestRun;
import com.autotest.exception.BadRequestException;
import com.autotest.exception.ResourceNotFoundException;
import com.autotest.repository.TestResultRepository;
import com.autotest.repository.TestRunRepository;
import com.autotest.service.ExecutionReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ExecutionReportServiceImpl implements ExecutionReportService {

    private final TestRunRepository testRunRepository;
    private final TestResultRepository testResultRepository;

    public ExecutionReportServiceImpl(TestRunRepository testRunRepository, TestResultRepository testResultRepository) {
        this.testRunRepository = testRunRepository;
        this.testResultRepository = testResultRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public String generateHtmlReport(Long testRunId, Long currentUserId) {
        TestRun run = testRunRepository.findByIdAndOwnerId(testRunId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Test run not found with id: " + testRunId));
        if (run.getStatus() == ExecutionStatus.PENDING || run.getStatus() == ExecutionStatus.RUNNING) {
            throw new BadRequestException("The HTML report is available after the test run has completed.");
        }
        List<TestResult> results = testResultRepository.findByTestRunIdAndOwnerId(testRunId, currentUserId);

        long passed = count(results, ExecutionStatus.PASSED);
        long failed = count(results, ExecutionStatus.FAILED);
        long errors = count(results, ExecutionStatus.ERROR);
        long skipped = count(results, ExecutionStatus.SKIPPED);
        long executed = passed + failed + errors;
        double passRate = executed == 0 ? 0 : passed * 100.0 / executed;

        StringBuilder html = new StringBuilder(4096);
        html.append("""
                <!doctype html><html lang="en"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>AutoTest AI run report</title>
                <style>body{font:15px system-ui,sans-serif;max-width:1100px;margin:2rem auto;padding:0 1rem;color:#222}
                table{border-collapse:collapse;width:100%;margin:1rem 0}th,td{border:1px solid #ccc;padding:.6rem;text-align:left;vertical-align:top}
                pre{white-space:pre-wrap;overflow-wrap:anywhere;background:#f4f4f4;padding:1rem}
                .FAILED,.ERROR{color:#a11}.PASSED{color:#176b2c}.SKIPPED{color:#765b00}</style>
                </head><body><h1>AutoTest AI execution report</h1><dl>
                """);
        field(html, "Project", run.getProject().getName());
        field(html, "Test run ID", run.getId());
        field(html, "Status", run.getStatus());
        field(html, "Started", run.getStartedAt());
        field(html, "Completed", run.getCompletedAt());
        field(html, "Duration", run.getDurationMs() > 0 ? run.getDurationMs() + " ms" : "Unavailable for this historical run");
        field(html, "Total tests", run.getTotalTests());
        field(html, "Results recorded", results.size());
        field(html, "Passed", passed);
        field(html, "Failed", failed);
        field(html, "Errors", errors);
        field(html, "Skipped", skipped);
        field(html, "Pass rate", String.format("%.1f%%", passRate));
        html.append("</dl><h2>Individual results</h2><table><thead><tr><th>Test</th><th>Type</th>")
                .append("<th>Status</th><th>Started</th><th>Completed</th><th>Duration</th><th>Failure details</th><th>Screenshot</th>")
                .append("</tr></thead><tbody>");
        for (TestResult result : results) {
            html.append("<tr><td>").append(escape(result.getTestCase().getName())).append("</td><td>")
                    .append(escape(result.getTestCase().getTestType())).append("</td><td class=\"")
                    .append(escape(result.getStatus())).append("\">").append(escape(result.getStatus()))
                    .append("</td><td>").append(escape(result.getStartedAt()))
                    .append("</td><td>").append(escape(result.getCompletedAt()))
                    .append("</td><td>").append(result.getDurationMs()).append(" ms</td><td>")
                    .append(escape(sanitize(result.getErrorMessage()))).append("</td><td>");
            if (result.getScreenshotPath() != null) {
                html.append("Captured - authenticated screenshot endpoint: <code>/api/v1/test-results/")
                        .append(result.getId()).append("/screenshot</code>");
            } else {
                html.append("Not captured");
            }
            html.append("</td></tr>");
            if (result.getExecutionLog() != null && !result.getExecutionLog().isBlank()) {
                html.append("<tr><td colspan=\"8\"><details><summary>Execution log</summary><pre>")
                        .append(escape(sanitize(result.getExecutionLog())))
                        .append("</pre></details></td></tr>");
            }
        }
        return html.append("</tbody></table></body></html>").toString();
    }

    private long count(List<TestResult> results, ExecutionStatus status) {
        return results.stream().filter(result -> result.getStatus() == status).count();
    }

    private void field(StringBuilder html, String label, Object value) {
        html.append("<dt>").append(escape(label)).append("</dt><dd>")
                .append(escape(value)).append("</dd>");
    }

    private String sanitize(String value) {
        if (value == null) return "";
        return value
                .replaceAll("(?i)(authorization[\"']?\\s*[:=]\\s*[\"']?)(?:bearer\\s+)?[^\\s\"',}]+", "$1[REDACTED]")
                .replaceAll("(?i)(bearer\\s+)[A-Za-z0-9._~+/-]+=*", "$1[REDACTED]")
                .replaceAll("(?i)(password|passwd|secret|token|jwt|credential|cookie|session|api[-_ ]?key|authorization)([\"']?\\s*[:=]\\s*[\"']?)[^\\s\"',}]+", "$1$2[REDACTED]")
                .replaceAll("(?i)(value:\\s*)'[^']*'", "$1'[REDACTED]'")
                .replaceAll("(?i)([?&](?:token|key|password|secret|authorization)=)[^&\\s]+", "$1[REDACTED]");
    }

    private String escape(Object value) {
        if (value == null) return "";
        return value.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
