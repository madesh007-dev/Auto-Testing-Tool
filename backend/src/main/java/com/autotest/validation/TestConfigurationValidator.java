package com.autotest.validation;

import com.autotest.entity.TestType;
import com.autotest.exception.BadRequestException;
import com.autotest.execution.model.ActionType;
import com.autotest.execution.model.ApiTestConfiguration;
import com.autotest.execution.model.UiAction;
import com.autotest.execution.model.UiTestConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Component
public class TestConfigurationValidator {

    private final ObjectMapper objectMapper;

    private static final Set<String> ALLOWED_HTTP_METHODS = Set.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS"
    );

    private static final Set<String> ALLOWED_HEADER_PREFIXES = Set.of(
            "content-type", "accept", "x-", "authorization", "user-agent", "cache-control"
    );

    public TestConfigurationValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void validate(TestType testType, String configurationJson) {
        if (!StringUtils.hasText(configurationJson)) {
            throw new BadRequestException("Test configuration cannot be empty");
        }

        try {
            if (testType == TestType.UI_TEST) {
                UiTestConfiguration uiConfig = objectMapper.readValue(configurationJson, UiTestConfiguration.class);
                validateUiConfig(uiConfig);
            } else if (testType == TestType.API_TEST) {
                ApiTestConfiguration apiConfig = objectMapper.readValue(configurationJson, ApiTestConfiguration.class);
                validateApiConfig(apiConfig);
            } else {
                throw new BadRequestException("Unsupported test type: " + testType);
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Invalid test configuration format: " + e.getMessage());
        }
    }

    private void validateUiConfig(UiTestConfiguration config) {
        if (config.actions() == null || config.actions().isEmpty()) {
            throw new BadRequestException("UI test must have at least one action");
        }

        for (int i = 0; i < config.actions().size(); i++) {
            UiAction action = config.actions().get(i);
            if (action.type() == null) {
                throw new BadRequestException("Action at index " + i + " must have a valid type");
            }

            switch (action.type()) {
                case NAVIGATE, ASSERT_URL -> {
                    if (!StringUtils.hasText(action.value())) {
                        throw new BadRequestException("Action " + action.type() + " at index " + i + " requires a 'value' (path/URL)");
                    }
                }
                case CLICK, ASSERT_VISIBLE -> {
                    if (!StringUtils.hasText(action.selector())) {
                        throw new BadRequestException("Action " + action.type() + " at index " + i + " requires a 'selector'");
                    }
                }
                case FILL, ASSERT_TEXT -> {
                    if (!StringUtils.hasText(action.selector())) {
                        throw new BadRequestException("Action " + action.type() + " at index " + i + " requires a 'selector'");
                    }
                    if (action.value() == null) {
                        throw new BadRequestException("Action " + action.type() + " at index " + i + " requires a 'value'");
                    }
                }
                case SCREENSHOT -> {
                    // Selector is optional for element-level screenshot, null means full-page
                }
            }
        }
    }

    private void validateApiConfig(ApiTestConfiguration config) {
        if (!StringUtils.hasText(config.method())) {
            throw new BadRequestException("HTTP method is required");
        }

        String upperMethod = config.method().toUpperCase();
        if (!ALLOWED_HTTP_METHODS.contains(upperMethod)) {
            throw new BadRequestException("Unsupported HTTP method: " + config.method());
        }

        if (!StringUtils.hasText(config.endpoint())) {
            throw new BadRequestException("Endpoint is required");
        }

        if (config.expectedStatus() == null || config.expectedStatus() < 100 || config.expectedStatus() > 599) {
            throw new BadRequestException("Valid HTTP expected status code (100-599) is required");
        }
    }
}
