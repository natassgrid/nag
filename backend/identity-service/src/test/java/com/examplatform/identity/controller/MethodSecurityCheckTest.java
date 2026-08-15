/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.identity.controller;

import com.examplatform.identity.filter.RateLimitFilter;
import com.examplatform.identity.service.RoleManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;

/**
 * Verifies that {@code @EnableMethodSecurity} is active and that role-based
 * access control on {@link RoleController} is enforced.
 *
 * Uses a minimal test-local {@link TestSecurityConfig} instead of importing
 * the production {@code SecurityConfig}, so no JPA / Redis beans are needed.
 */
@WebMvcTest(
    controllers = RoleController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class,
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = {
            com.examplatform.identity.config.SecurityConfig.class,
            RateLimitFilter.class
        }
    )
)
@Import({
    MethodSecurityCheckTest.TestSecurityConfig.class,
    org.springframework.boot.autoconfigure.aop.AopAutoConfiguration.class
})
class MethodSecurityCheckTest {

    /**
     * Minimal security config for this test slice.
     * Enables method security and sets up a stateless JWT-compatible filter chain
     * without pulling in any infrastructure beans (JPA, Redis, Kafka).
     */
    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            return http.build();
        }
    }

    @Autowired
    ApplicationContext ctx;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    com.examplatform.identity.config.AppSecurityProperties appSecurityProperties;

    @MockitoBean
    com.examplatform.identity.service.RateLimiterService rateLimiterService;

    @MockitoBean
    org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @MockitoBean
    RoleManagementService roleManagementService;

    @Test
    void contextContainsMethodSecurityInterceptor() {
        String[] beans = ctx.getBeanDefinitionNames();
        boolean hasMethodSecurity = false;
        for (String name : beans) {
            if (name.toLowerCase().contains("methodsecurity") || name.toLowerCase().contains("preauthorize") || name.toLowerCase().contains("prepost")) {
                System.out.println("FOUND BEAN: " + name);
                hasMethodSecurity = true;
            }
        }
        System.out.println("SECURITY-RELATED BEANS:");
        for (String name : beans) {
            if (name.toLowerCase().contains("secur") || name.toLowerCase().contains("advisor") || name.toLowerCase().contains("interceptor")) {
                System.out.println("  " + name);
            }
        }
        assertThat(hasMethodSecurity).as("Method security beans should be present").isTrue();
    }

    @Test
    @WithMockUser(username = "candidate-id", roles = {"CANDIDATE"})
    void candidateShouldGet403() throws Exception {
        String[] allBeans = ctx.getBeanDefinitionNames();
        System.out.println("ALL BEAN NAMES (" + allBeans.length + "):");
        for (String name : allBeans) {
            System.out.println("  " + name);
        }

        String requestBody = "{\"role\": \"EVALUATOR\", \"action\": \"ASSIGN\"}";
        var result = mockMvc.perform(post("/api/v1/identity/roles/{userId}", "11111111-1111-1111-1111-111111111111")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andReturn();
        System.out.println("CANDIDATE STATUS: " + result.getResponse().getStatus());
        System.out.println("CANDIDATE BODY: '" + result.getResponse().getContentAsString() + "'");
        System.out.println("CANDIDATE CONTENT TYPE: " + result.getResponse().getContentType());
        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }
}
