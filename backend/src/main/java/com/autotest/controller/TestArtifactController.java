package com.autotest.controller;

import com.autotest.security.UserPrincipal;
import com.autotest.service.ExecutionReportService;
import com.autotest.service.ExecutionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Test artifacts", description = "Authorized test screenshots and HTML reports")
@SecurityRequirement(name = "bearerAuth")
public class TestArtifactController {

    private final ExecutionService executionService;
    private final ExecutionReportService reportService;

    public TestArtifactController(ExecutionService executionService, ExecutionReportService reportService) {
        this.executionService = executionService;
        this.reportService = reportService;
    }

    @GetMapping(value = "/test-runs/{testRunId}/report", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> downloadReport(
            @PathVariable Long testRunId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        String report = reportService.generateHtmlReport(testRunId, currentUser.getId());
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("autotest-report-run-" + testRunId + ".html", StandardCharsets.UTF_8).build().toString())
                .body(report);
    }

    @GetMapping(value = "/test-results/{testResultId}/screenshot", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<Resource> getScreenshot(
            @PathVariable Long testResultId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Resource screenshot = new FileSystemResource(executionService.getScreenshotPath(testResultId, currentUser.getId()));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("test-result-" + testResultId + ".png").build().toString())
                .body(screenshot);
    }
}
