package com.autotest;

import com.autotest.dto.LoginRequest;
import com.autotest.dto.ProjectRequest;
import com.autotest.dto.RegisterRequest;
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
class AuthAndProjectIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Complete Flow: Register, Login, Create Project, and Get Project")
    void completeAuthAndProjectFlow() throws Exception {
        // 1. Register User 1
        RegisterRequest registerReq = new RegisterRequest("testuser1", "testuser1@example.com", "password123");
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("testuser1")))
                .andExpect(jsonPath("$.token").isNotEmpty());

        // 2. Login User 1
        LoginRequest loginReq = new LoginRequest("testuser1", "password123");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token1 = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("token").asText();

        // 3. Create Project for User 1
        ProjectRequest projectReq = new ProjectRequest("E-Commerce UI Tests", "Testing storefront flows", "https://example.com");
        MvcResult projectResult = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("E-Commerce UI Tests")))
                .andExpect(jsonPath("$.baseUrl", is("https://example.com")))
                .andReturn();

        long projectId = objectMapper.readTree(projectResult.getResponse().getContentAsString()).get("id").asLong();

        // 4. Retrieve Projects list for User 1
        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("E-Commerce UI Tests")));

        // 5. Register User 2 (Testing isolation & ownership)
        RegisterRequest registerReq2 = new RegisterRequest("testuser2", "testuser2@example.com", "password123");
        MvcResult user2Result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq2)))
                .andExpect(status().isCreated())
                .andReturn();

        String token2 = objectMapper.readTree(user2Result.getResponse().getContentAsString()).get("token").asText();

        // User 2 cannot access User 1's project (Returns 404 Not Found to prevent data enumeration)
        mockMvc.perform(get("/api/v1/projects/" + projectId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isNotFound());

        // Unauthenticated access fails with 403 or 401
        mockMvc.perform(get("/api/v1/projects/" + projectId))
                .andExpect(status().isForbidden());
    }
}
