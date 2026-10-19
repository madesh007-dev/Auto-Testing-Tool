package com.autotest.execution.api;

import com.autotest.entity.ExecutionStatus;
import com.autotest.execution.ExecutionUrlResolver;
import com.autotest.execution.model.ApiTestConfiguration;
import com.autotest.execution.model.ExecutionOutcome;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Component
public class RestApiExecutor {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ExecutionUrlResolver urlResolver;

    public RestApiExecutor(ObjectMapper objectMapper, ExecutionUrlResolver urlResolver) {
        this.objectMapper = objectMapper;
        this.urlResolver = urlResolver;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public ExecutionOutcome execute(String configurationJson) {
        long startTime = System.currentTimeMillis();
        StringBuilder logs = new StringBuilder();
        URI targetUri = null;

        logs.append("[API EXECUTION START]\n");

        try {
            ApiTestConfiguration config = objectMapper.readValue(configurationJson, ApiTestConfiguration.class);
            targetUri = URI.create(urlResolver.resolveApiUrl(config.apiBaseUrl(), config.endpoint()));
            logs.append(String.format("[REQUEST] %s %s\n", config.method().toUpperCase(), safeUrl(targetUri)));
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(targetUri)
                    .timeout(Duration.ofMillis(config.timeoutMs() != null ? config.timeoutMs() : 10000));

            // Permitted headers
            if (config.headers() != null) {
                config.headers().forEach((k, v) -> {
                    if (isValidHeader(k)) {
                        reqBuilder.header(k, v);
                        logs.append(String.format(" -> Header: %s: %s\n", k, maskIfSensitive(k, v)));
                    }
                });
            }

            // HTTP Body & Method
            String method = config.method().toUpperCase();
            HttpRequest.BodyPublisher bodyPublisher = StringUtils.hasText(config.requestBody())
                    ? HttpRequest.BodyPublishers.ofString(config.requestBody())
                    : HttpRequest.BodyPublishers.noBody();

            reqBuilder.method(method, bodyPublisher);
            if (StringUtils.hasText(config.requestBody()) && (config.headers() == null || !config.headers().containsKey("Content-Type"))) {
                reqBuilder.header("Content-Type", "application/json");
            }

            HttpRequest request = reqBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            long duration = System.currentTimeMillis() - startTime;
            int actualStatus = response.statusCode();
            logs.append(String.format("[RESPONSE] Status: %d (Took %d ms)\n", actualStatus, duration));
            logs.append(" -> Body: ").append(truncateBody(response.body())).append("\n");

            // Evaluate Status Code Assertion
            if (actualStatus != config.expectedStatus()) {
                String errorMsg = String.format("Assertion Failed: Expected HTTP %d but received %d",
                        config.expectedStatus(), actualStatus);
                logs.append("[ASSERTION FAILURE] ").append(errorMsg).append("\n");
                return new ExecutionOutcome(ExecutionStatus.FAILED, duration, errorMsg, logs.toString(), null);
            }

            // Evaluate Optional JSON Assertions
            if (config.jsonAssertions() != null && !config.jsonAssertions().isEmpty()) {
                JsonNode rootNode = objectMapper.readTree(response.body());
                for (Map.Entry<String, Object> entry : config.jsonAssertions().entrySet()) {
                    String jsonKey = entry.getKey();
                    Object expectedVal = entry.getValue();
                    JsonNode valNode = rootNode.at(jsonKey.startsWith("/") ? jsonKey : "/" + jsonKey);

                    if (valNode.isMissingNode()) {
                        String errorMsg = "JSON Assertion Failed: Path '" + jsonKey + "' not found in response";
                        logs.append("[ASSERTION FAILURE] ").append(errorMsg).append("\n");
                        return new ExecutionOutcome(ExecutionStatus.FAILED, duration, errorMsg, logs.toString(), null);
                    }

                    String actualVal = valNode.asText();
                    if (!String.valueOf(expectedVal).equals(actualVal)) {
                        String errorMsg = String.format("JSON Assertion Failed at '%s': expected '%s' but found '%s'",
                                jsonKey, expectedVal, actualVal);
                        logs.append("[ASSERTION FAILURE] ").append(errorMsg).append("\n");
                        return new ExecutionOutcome(ExecutionStatus.FAILED, duration, errorMsg, logs.toString(), null);
                    }
                    logs.append(String.format(" -> Assertion PASSED for '%s' == '%s'\n", jsonKey, expectedVal));
                }
            }

            logs.append("[SUCCESS] All API assertions passed successfully.\n");
            return new ExecutionOutcome(ExecutionStatus.PASSED, duration, null, logs.toString(), null);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String errorMessage;
            if (targetUri == null) {
                errorMessage = "API URL configuration is invalid. Use an absolute HTTP(S) endpoint or configure an HTTP(S) API base URL for relative endpoints.";
            } else {
                errorMessage = String.format("API execution at %s failed (%s). Check the endpoint, response format, and API availability.",
                        targetUri.getHost() + (targetUri.getPort() >= 0 ? ":" + targetUri.getPort() : ""),
                        e.getClass().getSimpleName());
            }
            logs.append("[ERROR] ").append(errorMessage).append("\n");
            return new ExecutionOutcome(ExecutionStatus.ERROR, duration, errorMessage, logs.toString(), null);
        }
    }

    private String safeUrl(URI uri) {
        StringBuilder safe = new StringBuilder(uri.getScheme()).append("://").append(uri.getHost());
        if (uri.getPort() >= 0) {
            safe.append(':').append(uri.getPort());
        }
        if (uri.getRawPath() != null) {
            safe.append(uri.getRawPath());
        }
        return safe.toString();
    }

    private boolean isValidHeader(String header) {
        String lower = header.toLowerCase();
        return lower.startsWith("content-type") || lower.startsWith("accept")
                || lower.startsWith("x-") || lower.startsWith("authorization")
                || lower.startsWith("user-agent") || lower.startsWith("cache-control");
    }

    private String maskIfSensitive(String key, String val) {
        if (key.toLowerCase().contains("auth") || key.toLowerCase().contains("secret") || key.toLowerCase().contains("token")) {
            return "******";
        }
        return val;
    }

    private String truncateBody(String body) {
        if (body == null) return "<empty>";
        if (body.length() > 500) {
            return body.substring(0, 500) + "... [truncated]";
        }
        return body;
    }
}
