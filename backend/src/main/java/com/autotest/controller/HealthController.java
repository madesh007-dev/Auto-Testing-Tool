package com.autotest.controller;

import com.autotest.dto.HealthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health Check", description = "Verifies system and database health")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    @Operation(summary = "Check backend and database health")
    public ResponseEntity<HealthResponse> checkHealth() {
        Map<String, Object> details = new HashMap<>();
        String dbStatus = "UP";
        try {
            Integer queryResult = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            details.put("database", queryResult != null && queryResult == 1 ? "CONNECTED" : "UNEXPECTED_RESPONSE");
        } catch (Exception e) {
            dbStatus = "DOWN";
            details.put("database", "DOWN: " + e.getMessage());
        }

        details.put("databaseHealth", dbStatus);
        details.put("jvmVersion", System.getProperty("java.version"));

        HealthResponse response = new HealthResponse(
                "UP".equals(dbStatus) ? "UP" : "DEGRADED",
                "AutoTest AI",
                "1.0.0",
                Instant.now(),
                details
        );

        return ResponseEntity.ok(response);
    }
}
