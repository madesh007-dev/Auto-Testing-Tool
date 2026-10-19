package com.autotest;

import com.autotest.execution.ExecutionUrlResolver;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExecutionUrlResolverTest {

    private final ExecutionUrlResolver resolver = new ExecutionUrlResolver();

    @Test
    void resolvesUiPathAgainstFrontendBaseUrl() {
        assertEquals(
                "http://localhost:5174/restaurants",
                resolver.resolveUiUrl("http://localhost:5174", "/restaurants")
        );
    }

    @Test
    void resolvesRelativeApiEndpointAgainstApiBaseUrl() {
        assertEquals(
                "http://localhost:8081/restaurants",
                resolver.resolveApiUrl("http://localhost:8081", "/restaurants")
        );
    }

    @Test
    void usesAbsoluteApiEndpointWithoutPrependingBaseUrl() {
        assertEquals(
                "http://localhost:8081/restaurants",
                resolver.resolveApiUrl("http://localhost:5174", "http://localhost:8081/restaurants")
        );
    }

    @Test
    void handlesTrailingAndLeadingSlashes() {
        assertEquals(
                "http://localhost:5174/restaurants",
                resolver.resolveUiUrl("http://localhost:5174/", "/restaurants")
        );
        assertEquals(
                "http://localhost:8081/restaurants",
                resolver.resolveApiUrl("http://localhost:8081/", "/restaurants")
        );
    }
}
