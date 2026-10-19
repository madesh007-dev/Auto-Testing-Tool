package com.autotest;

import com.autotest.dto.ProjectRequest;
import com.autotest.dto.RegisterRequest;
import com.autotest.dto.TestCaseRequest;
import com.autotest.entity.TestType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TestExecutionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Trigger Test Run and retrieve execution results asynchronously")
    void testExecutionFlow() throws Exception {
        // 1. Register User
        RegisterRequest regReq = new RegisterRequest("execuser", "execuser@example.com", "password123");
        MvcResult regRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String token = objectMapper.readTree(regRes.getResponse().getContentAsString()).get("token").asText();

        // 2. Create Project
        ProjectRequest projReq = new ProjectRequest("Execution Test Project", "Testing execution orchestrator", "https://httpbin.org");
        MvcResult projRes = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projReq)))
                .andExpect(status().isCreated())
                .andReturn();
        long projectId = objectMapper.readTree(projRes.getResponse().getContentAsString()).get("id").asLong();

        // 3. Add API Test Case
        String apiConfig = """
                {
                    "method": "GET",
                    "endpoint": "/get",
                    "expectedStatus": 200
                }
                """;
        TestCaseRequest apiReq = new TestCaseRequest("HTTPBin GET Test", "Checks GET status 200", TestType.API_TEST, apiConfig);
        MvcResult caseRes = mockMvc.perform(post("/api/v1/projects/" + projectId + "/test-cases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(apiReq)))
                .andExpect(status().isCreated())
                .andReturn();
        long testCaseId = objectMapper.readTree(caseRes.getResponse().getContentAsString()).get("id").asLong();

        // 4. Trigger Execution Run
        MvcResult runRes = mockMvc.perform(post("/api/v1/projects/" + projectId + "/test-runs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("RUNNING")))
                .andExpect(jsonPath("$.totalTests", is(1)))
                .andReturn();
        long testRunId = objectMapper.readTree(runRes.getResponse().getContentAsString()).get("id").asLong();

        // 5. Query Test Run by ID
        mockMvc.perform(get("/api/v1/test-runs/" + testRunId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) testRunId)))
                .andExpect(jsonPath("$.durationMs").isNumber());

        // 6. Query Test Results
        mockMvc.perform(get("/api/v1/test-runs/" + testRunId + "/results")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 7. Trigger and inspect an individual test case run
        MvcResult singleRunRes = mockMvc.perform(post("/api/v1/test-cases/" + testCaseId + "/test-runs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("RUNNING")))
                .andExpect(jsonPath("$.totalTests", is(1)))
                .andReturn();
        long singleRunId = objectMapper.readTree(singleRunRes.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get("/api/v1/test-runs/" + singleRunId + "/results")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].testCaseId", is((int) testCaseId)));

        mockMvc.perform(get("/api/v1/test-runs")
                        .header("Authorization", "Bearer " + token)
                        .param("projectId", String.valueOf(projectId))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(2)))
                .andExpect(jsonPath("$.content[0].durationMs").isNumber());

        mockMvc.perform(get("/api/v1/test-runs")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'status'."));

        mockMvc.perform(get("/api/v1/dashboard/metrics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalTestCases").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.totalTestRuns").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.executionTrend.length()", is(14)));
    }
}
