package com.examplatform.identity.controller;

import com.examplatform.identity.config.SecurityConfig;
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

@WebMvcTest(controllers = RoleController.class, excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {SecurityConfig.class, RateLimitFilter.class})
)
@Import({
    SecurityConfig.class,
    RateLimitFilter.class,
    org.springframework.boot.autoconfigure.aop.AopAutoConfiguration.class
})
class MethodSecurityCheckTest {

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
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
    }
}
