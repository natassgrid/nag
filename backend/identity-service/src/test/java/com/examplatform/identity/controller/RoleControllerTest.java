package com.examplatform.identity.controller;

import com.examplatform.identity.config.SecurityConfig;
import com.examplatform.identity.domain.enums.UserRole;
import com.examplatform.identity.dto.RoleAction;
import com.examplatform.identity.dto.RoleAssignmentResponse;
import com.examplatform.identity.filter.RateLimitFilter;
import com.examplatform.identity.service.RoleManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for {@link RoleController}.
 * Verifies RBAC enforcement: only SUPER_ADMIN can manage roles.
 *
 * <p><strong>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6</strong>
 */
@WebMvcTest(
    controllers = RoleController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, RateLimitFilter.class})
)
@Import(RoleControllerTest.TestSecurityConfig.class)
@DisplayName("RoleController")
class RoleControllerTest {

    /**
     * Minimal security configuration for WebMvcTest context.
     * Mirrors the production SecurityConfig authorization rules but without
     * oauth2ResourceServer (which needs Keycloak at runtime).
     * {@code @WithMockUser} populates the SecurityContext for test requests.
     */
    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                    .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                    .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                );
            return http.build();
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    RateLimitFilter rateLimitFilter;

    @MockitoBean
    RoleManagementService roleManagementService;

    private static final UUID TARGET_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Nested
    @DisplayName("POST /api/v1/identity/roles/{userId}")
    class ManageRole {

        @Test
        @DisplayName("SUPER_ADMIN can assign a role — returns 200")
        @WithMockUser(username = "admin-id", roles = {"SUPER_ADMIN"})
        void superAdminCanAssignRole() throws Exception {
            RoleAssignmentResponse response = RoleAssignmentResponse.builder()
                    .userId(TARGET_USER_ID)
                    .role(UserRole.EVALUATOR)
                    .action(RoleAction.ASSIGN)
                    .message("Role EVALUATOR assigned successfully.")
                    .build();

            when(roleManagementService.manageRole(eq(TARGET_USER_ID), any(), eq("admin-id"), eq("default")))
                    .thenReturn(response);

            String requestBody = """
                    {
                        "role": "EVALUATOR",
                        "action": "ASSIGN"
                    }
                    """;

            mockMvc.perform(post("/api/v1/identity/roles/{userId}", TARGET_USER_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.role").value("EVALUATOR"))
                    .andExpect(jsonPath("$.data.action").value("ASSIGN"))
                    .andExpect(jsonPath("$.data.message").value("Role EVALUATOR assigned successfully."));
        }

        @Test
        @DisplayName("CANDIDATE cannot assign a role — returns 403")
        @WithMockUser(username = "candidate-id", roles = {"CANDIDATE"})
        void candidateCannotAssignRole() throws Exception {
            String requestBody = """
                    {
                        "role": "EVALUATOR",
                        "action": "ASSIGN"
                    }
                    """;

            mockMvc.perform(post("/api/v1/identity/roles/{userId}", TARGET_USER_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Unauthenticated user — returns 401")
        void unauthenticatedUserReturns401() throws Exception {
            String requestBody = """
                    {
                        "role": "EVALUATOR",
                        "action": "ASSIGN"
                    }
                    """;

            mockMvc.perform(post("/api/v1/identity/roles/{userId}", TARGET_USER_ID)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/identity/roles/{userId}")
    class GetRoles {

        @Test
        @DisplayName("SUPER_ADMIN can get roles for any user — returns 200")
        @WithMockUser(username = "admin-id", roles = {"SUPER_ADMIN"})
        void superAdminCanGetRoles() throws Exception {
            when(roleManagementService.getRoles(TARGET_USER_ID, "default"))
                    .thenReturn(List.of(UserRole.CANDIDATE, UserRole.EVALUATOR));

            mockMvc.perform(get("/api/v1/identity/roles/{userId}", TARGET_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data[0]").value("CANDIDATE"))
                    .andExpect(jsonPath("$.data[1]").value("EVALUATOR"));
        }

        @Test
        @DisplayName("User can get their own roles — returns 200")
        @WithMockUser(username = "11111111-1111-1111-1111-111111111111", roles = {"CANDIDATE"})
        void userCanGetOwnRoles() throws Exception {
            when(roleManagementService.getRoles(TARGET_USER_ID, "default"))
                    .thenReturn(List.of(UserRole.CANDIDATE));

            mockMvc.perform(get("/api/v1/identity/roles/{userId}", TARGET_USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data[0]").value("CANDIDATE"));
        }
    }
}
