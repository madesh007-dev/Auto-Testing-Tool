package com.autotest.execution;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;

@Component
public class ExecutionUrlResolver {

    public String resolveUiUrl(String frontendBaseUrl, String path) {
        if (isAbsoluteHttpUrl(path)) {
            return path;
        }
        return trimTrailingSlashes(frontendBaseUrl) + "/" + trimLeadingSlashes(path);
    }

    public String resolveApiUrl(String apiBaseUrl, String endpoint) {
        if (isAbsoluteHttpUrl(endpoint)) {
            return endpoint;
        }
        if (!StringUtils.hasText(apiBaseUrl)) {
            throw new IllegalArgumentException(
                    "An API base URL is required when the API endpoint is relative."
            );
        }
        if (!isAbsoluteHttpUrl(apiBaseUrl)) {
            throw new IllegalArgumentException("API base URL must be an absolute HTTP(S) URL.");
        }
        return trimTrailingSlashes(apiBaseUrl) + "/" + trimLeadingSlashes(endpoint);
    }

    private boolean isAbsoluteHttpUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            return uri.isAbsolute()
                    && uri.getHost() != null
                    && ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String trimTrailingSlashes(String value) {
        return value.replaceAll("/+$", "");
    }

    private String trimLeadingSlashes(String value) {
        return value.replaceAll("^/+", "");
    }
}
