package com.autotest.service;

import org.springframework.context.ApplicationEvent;

public class TestRunCreatedEvent extends ApplicationEvent {
    private final Long testRunId;
    private final java.util.List<com.autotest.execution.model.ExecutionTaskData> tasks;
    private final String baseUrl;

    public TestRunCreatedEvent(Object source, Long testRunId, java.util.List<com.autotest.execution.model.ExecutionTaskData> tasks, String baseUrl) {
        super(source);
        this.testRunId = testRunId;
        this.tasks = tasks;
        this.baseUrl = baseUrl;
    }

    public Long getTestRunId() {
        return testRunId;
    }

    public java.util.List<com.autotest.execution.model.ExecutionTaskData> getTasks() {
        return tasks;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
