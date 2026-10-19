package com.autotest;

import com.autotest.controller.TestArtifactController;
import com.autotest.exception.GlobalExceptionHandler;
import com.autotest.exception.ResourceNotFoundException;
import com.autotest.exception.BadRequestException;
import com.autotest.security.UserPrincipal;
import com.autotest.service.ExecutionReportService;
import com.autotest.service.ExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_HTML;

class TestArtifactControllerTest {

    private final ExecutionService executionService = mock(ExecutionService.class);
    private final ExecutionReportService reportService = mock(ExecutionReportService.class);
    private final UserPrincipal principal = new UserPrincipal(
            7L, "report-user", "report@example.com", "unused",
            AuthorityUtils.createAuthorityList("ROLE_USER")
    );
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TestArtifactController controller = new TestArtifactController(executionService, reportService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(
                            MethodParameter parameter,
                            ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest,
                            WebDataBinderFactory binderFactory
                    ) {
                        return principal;
                    }
                })
                .build();
    }

    @Test
    void reportEndpointReturnsDownloadableHtmlAndUsesAuthenticatedOwner() throws Exception {
        when(reportService.generateHtmlReport(12L, 7L)).thenReturn("<!doctype html><html>report</html>");

        mockMvc.perform(get("/api/v1/test-runs/12/report").accept(TEXT_HTML, APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(TEXT_HTML))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("autotest-report-run-12.html")))
                .andExpect(content().string(containsString("<!doctype html>")));

        verify(reportService).generateHtmlReport(12L, 7L);
    }

    @Test
    void reportEndpointReturnsNotFoundForAnUnknownRunId() throws Exception {
        when(reportService.generateHtmlReport(999L, 7L))
                .thenThrow(new ResourceNotFoundException("Test run not found with id: 999"));

        mockMvc.perform(get("/api/v1/test-runs/999/report").accept(TEXT_HTML, APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Test run not found with id: 999"));

        verify(reportService).generateHtmlReport(999L, 7L);
    }

    @Test
    void reportEndpointReturnsUsefulClientErrorWhileRunIsExecuting() throws Exception {
        when(reportService.generateHtmlReport(12L, 7L))
                .thenThrow(new BadRequestException("The HTML report is available after the test run has completed."));

        mockMvc.perform(get("/api/v1/test-runs/12/report").accept(TEXT_HTML, APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value(containsString("after the test run has completed")));
    }
}
