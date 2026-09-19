package com.commutesync.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class AuthAndAuthorizationIT extends IntegrationTestSupport {

    @Test
    void registerLoginAndAccessCurrentUser() throws Exception {
        String email = uniqueEmail("employee");
        registerAndGetToken(email, "EMPLOYEE");

        String loginBody = objectMapper.writeValueAsString(
                Map.of("email", email, "password", "password123"));
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("EMPLOYEE"))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("token").asText();

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeCannotAccessAdminEndpoint() throws Exception {
        String token = registerAndGetToken(uniqueEmail("employee"), "EMPLOYEE");

        mockMvc.perform(get("/api/admin/dashboard").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminEndpoint() throws Exception {
        String token = registerAndGetToken(uniqueEmail("admin"), "ADMIN");

        mockMvc.perform(get("/api/admin/dashboard").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEmployees").isNumber());
    }

    @Test
    void duplicateRegistrationIsRejected() throws Exception {
        String email = uniqueEmail("employee");
        registerAndGetToken(email, "EMPLOYEE");

        String body = objectMapper.writeValueAsString(Map.of(
                "fullName", "Integration User", "email", email,
                "password", "password123", "role", "EMPLOYEE"));

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidCredentialsAreRejected() throws Exception {
        String email = uniqueEmail("employee");
        registerAndGetToken(email, "EMPLOYEE");

        String body = objectMapper.writeValueAsString(Map.of("email", email, "password", "wrong-password"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
    }
}
