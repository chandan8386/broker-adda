package com.trishakti.crm.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void bootstrapAdmin_thenLogin_thenAccessProtectedEndpoint() throws Exception {
        String register = """
                {"fullName":"Test Admin","username":"testadmin","email":"testadmin@trishakti.com","password":"Password@123"}
                """;

        var registerResult = mockMvc.perform(post("/auth/register-first-admin")
                        .contentType("application/json").content(register))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        JsonNode body = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String token = body.get("accessToken").asText();

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testadmin"))
                .andExpect(jsonPath("$.roles").isArray());

        mockMvc.perform(get("/dashboard/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLeads").exists())
                .andExpect(jsonPath("$.leadsByStatus").exists());
    }

    @Test
    void protectedEndpointWithoutToken_is401() throws Exception {
        mockMvc.perform(get("/leads")).andExpect(status().isUnauthorized());
    }

    @Test
    void login_withBadCredentials_is401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType("application/json")
                        .content("{\"usernameOrEmail\":\"nobody\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }
}
