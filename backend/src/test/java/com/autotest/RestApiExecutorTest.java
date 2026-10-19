package com.autotest;

import com.autotest.entity.ExecutionStatus;
import com.autotest.execution.ExecutionUrlResolver;
import com.autotest.execution.api.RestApiExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestApiExecutorTest {

    private final RestApiExecutor executor = new RestApiExecutor(new ObjectMapper(), new ExecutionUrlResolver());

    @Test
    void reportsSafeDiagnosticWhenApiRequestFails() {
        String configuration = """
                {
                  "method": "GET",
                  "endpoint": "http://127.0.0.1:1/restaurants?token=private-value",
                  "expectedStatus": 200
                }
                """;

        var outcome = executor.execute(configuration);

        assertTrue(outcome.status() == ExecutionStatus.ERROR);
        assertTrue(outcome.errorMessage().contains("127.0.0.1:1"));
        assertFalse(outcome.executionLog().contains("private-value"));
        assertFalse(outcome.errorMessage().contains("private-value"));
    }
}
