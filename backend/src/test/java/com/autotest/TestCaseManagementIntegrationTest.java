package com.autotest;

import com.autotest.dto.LoginRequest;
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
class TestCaseManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Create UI and API test cases with configuration validation and ownership enforcement")
    void testCaseManagementFlow() throws Exception {
        // 1. Register User
        RegisterRequest regReq = new RegisterRequest("tcuser", "tcuser@example.com", "password123");
        MvcResult regRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String token = objectMapper.readTree(regRes.getResponse().getContentAsString()).get("token").asText();

        // 2. Create Project
        ProjectRequest projReq = new ProjectRequest("Banking Portal", "Core banking automated tests", "https://bank.example.com");
        MvcResult projRes = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projReq)))
                .andExpect(status().isCreated())
                .andReturn();
        long projectId = objectMapper.readTree(projRes.getResponse().getContentAsString()).get("id").asLong();

        // 3. Create Valid UI Test Case
        String uiConfig = """
                {
                    "baseUrl": "https://bank.example.com",
                    "actions": [
                        { "type": "NAVIGATE", "value": "/login" },
                        { "type": "FILL", "selector": "[data-testid='username']", "value": "demo_user" },
                        { "type": "CLICK", "selector": "[data-testid='login-button']" },
                        { "type": "ASSERT_VISIBLE", "selector": "[data-testid='account-dashboard']" }
                    ]
                }
                """;
        TestCaseRequest uiTestReq = new TestCaseRequest("Verify Customer Login", "Verifies authentication flow", TestType.UI_TEST, uiConfig);
        MvcResult uiRes = mockMvc.perform(post("/api/v1/projects/" + projectId + "/test-cases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uiTestReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Verify Customer Login")))
                .andExpect(jsonPath("$.testType", is("UI_TEST")))
                .andReturn();
        long uiTestCaseId = objectMapper.readTree(uiRes.getResponse().getContentAsString()).get("id").asLong();

        // 4. Create Valid API Test Case
        String apiConfig = """
                {
                    "method": "GET",
                    "endpoint": "/api/v1/accounts/123",
                    "expectedStatus": 200,
                    "timeoutMs": 5000
                }
                """;
        TestCaseRequest apiTestReq = new TestCaseRequest("Get Account Details API", "Checks 200 OK on account query", TestType.API_TEST, apiConfig);
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/test-cases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(apiTestReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.testType", is("API_TEST")));

        // 5. Test Invalid UI Configuration (NAVIGATE without value should fail)
        String invalidUiConfig = """
                {
                    "baseUrl": "https://bank.example.com",
                    "actions": [
                        { "type": "NAVIGATE" }
                    ]
                }
                """;
        TestCaseRequest badReq = new TestCaseRequest("Invalid Test", "Missing value", TestType.UI_TEST, invalidUiConfig);
        mockMvc.perform(post("/api/v1/projects/" + projectId + "/test-cases")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(status().isBadRequest());

        // 6. List Test Cases in Project
        mockMvc.perform(get("/api/v1/projects/" + projectId + "/test-cases")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(2)));

        // 7. Get Single Test Case
        mockMvc.perform(get("/api/v1/test-cases/" + uiTestCaseId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) uiTestCaseId)));
    }
}
