package com.autotest.execution.ui;

import com.autotest.entity.ExecutionStatus;
import com.autotest.execution.ExecutionUrlResolver;
import com.autotest.execution.model.ActionType;
import com.autotest.execution.model.ExecutionOutcome;
import com.autotest.execution.model.UiAction;
import com.autotest.execution.model.UiTestConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class PlaywrightUiExecutor {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightUiExecutor.class);
    private final ObjectMapper objectMapper;
    private final ExecutionUrlResolver urlResolver;

    public PlaywrightUiExecutor(ObjectMapper objectMapper, ExecutionUrlResolver urlResolver) {
        this.objectMapper = objectMapper;
        this.urlResolver = urlResolver;
    }

    public ExecutionOutcome execute(String configurationJson, String projectBaseUrl) {
        long startTime = System.currentTimeMillis();
        StringBuilder logs = new StringBuilder();
        String screenshotPath = null;

        logs.append("[EXECUTION START] Launching Playwright Chromium Headless...\n");

        Playwright playwright = null;
        Browser browser = null;
        BrowserContext context = null;
        Page page = null;

        try {
            UiTestConfiguration config = objectMapper.readValue(configurationJson, UiTestConfiguration.class);

            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 800));
            page = context.newPage();

            String effectiveBaseUrl = StringUtils.hasText(config.baseUrl()) ? config.baseUrl() : projectBaseUrl;
            if (effectiveBaseUrl.endsWith("/")) {
                effectiveBaseUrl = effectiveBaseUrl.substring(0, effectiveBaseUrl.length() - 1);
            }

            logs.append("[CONFIG] Base URL: ").append(effectiveBaseUrl).append("\n");
            logs.append("[CONFIG] Actions Count: ").append(config.actions().size()).append("\n");

            for (int i = 0; i < config.actions().size(); i++) {
                UiAction action = config.actions().get(i);
                int timeout = action.timeoutMs() != null ? action.timeoutMs() : 10000;
                logs.append(String.format("[ACTION %d] %s (selector: '%s', value: '%s')\n",
                        i + 1, action.type(), action.selector(), action.value()));

                switch (action.type()) {
                    case NAVIGATE -> {
                        String targetUrl = urlResolver.resolveUiUrl(effectiveBaseUrl, action.value());
                        logs.append(" -> Navigating to: ").append(targetUrl).append("\n");
                        page.navigate(targetUrl, new Page.NavigateOptions().setTimeout(timeout));
                    }
                    case CLICK -> {
                        Locator locator = page.locator(action.selector());
                        locator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(timeout));
                        locator.click();
                        logs.append(" -> Clicked: ").append(action.selector()).append("\n");
                    }
                    case FILL -> {
                        Locator locator = page.locator(action.selector());
                        locator.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(timeout));
                        locator.fill(action.value() != null ? action.value() : "");
                        logs.append(" -> Filled: ").append(action.selector()).append("\n");
                    }
                    case ASSERT_VISIBLE -> {
                        Locator locator = page.locator(action.selector());
                        waitForAssertionVisible(locator, timeout, action.selector());
                        boolean isVisible = locator.isVisible();
                        if (!isVisible) {
                            throw new AssertionError("Assertion Failed: Element '" + action.selector() + "' is not visible.");
                        }
                        logs.append(" -> Assertion PASSED: Element is visible\n");
                    }
                    case ASSERT_TEXT -> {
                        Locator locator = page.locator(action.selector());
                        waitForAssertionVisible(locator, timeout, action.selector());
                        String actualText = locator.innerText();
                        if (!actualText.contains(action.value())) {
                            throw new AssertionError("Assertion Failed: Expected text '" + action.value() + "' but found '" + actualText + "'.");
                        }
                        logs.append(" -> Assertion PASSED: Text contains '").append(action.value()).append("'\n");
                    }
                    case ASSERT_URL -> {
                        String currentUrl = page.url();
                        if (!currentUrl.contains(action.value())) {
                            throw new AssertionError("Assertion Failed: Expected URL to contain '" + action.value() + "' but current URL is '" + currentUrl + "'.");
                        }
                        logs.append(" -> Assertion PASSED: URL contains '").append(action.value()).append("'\n");
                    }
                    case SCREENSHOT -> {
                        Path screenshotDir = Paths.get("target/screenshots");
                        if (!Files.exists(screenshotDir)) {
                            Files.createDirectories(screenshotDir);
                        }
                        String fileName = "screenshot_" + UUID.randomUUID() + ".png";
                        Path dest = screenshotDir.resolve(fileName);
                        page.screenshot(new Page.ScreenshotOptions().setPath(dest).setFullPage(true));
                        screenshotPath = dest.toString();
                        logs.append(" -> Screenshot captured: ").append(dest).append("\n");
                    }
                }

            }

            long duration = System.currentTimeMillis() - startTime;
            logs.append(String.format("[SUCCESS] All UI actions passed in %d ms.\n", duration));
            return new ExecutionOutcome(ExecutionStatus.PASSED, duration, null, logs.toString(), screenshotPath);

        } catch (AssertionError e) {
            long duration = System.currentTimeMillis() - startTime;
            logs.append("[ASSERTION FAILURE] ").append(e.getMessage()).append("\n");
            // Capture failure screenshot if page is alive
            if (page != null) {
                try {
                    Path screenshotDir = Paths.get("target/screenshots");
                    if (!Files.exists(screenshotDir)) Files.createDirectories(screenshotDir);
                    String fileName = "failure_" + UUID.randomUUID() + ".png";
                    Path dest = screenshotDir.resolve(fileName);
                    page.screenshot(new Page.ScreenshotOptions().setPath(dest));
                    screenshotPath = dest.toString();
                    logs.append(" -> Captured failure screenshot: ").append(dest).append("\n");
                } catch (Exception ex) {
                    logs.append(" -> Could not capture failure screenshot: ").append(ex.getMessage()).append("\n");
                }
            }
            return new ExecutionOutcome(ExecutionStatus.FAILED, duration, e.getMessage(), logs.toString(), screenshotPath);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logs.append("[ERROR] Execution failed with exception: ").append(e.getMessage()).append("\n");
            logs.append(sw.toString());
            return new ExecutionOutcome(ExecutionStatus.ERROR, duration, e.getMessage(), logs.toString(), screenshotPath);

        } finally {
            if (page != null) {
                try { page.close(); } catch (Exception ignored) {}
            }
            if (context != null) {
                try { context.close(); } catch (Exception ignored) {}
            }
            if (browser != null) {
                try { browser.close(); } catch (Exception ignored) {}
            }
            if (playwright != null) {
                try { playwright.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void waitForAssertionVisible(Locator locator, int timeout, String selector) {
        try {
            locator.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(timeout));
        } catch (TimeoutError e) {
            throw new AssertionError("Assertion Failed: Element '" + selector
                    + "' was not visible within " + timeout + " ms.");
        }
    }
}
