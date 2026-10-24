/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.shared.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.task.SimpleAsyncTaskExecutorCustomizer;
import org.springframework.boot.task.ThreadPoolTaskExecutorCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Auto-configuration for registering the {@link UserContextFilter} and
 * asynchronous {@link ContextPropagatingTaskDecorator}.
 */
@AutoConfiguration
@ConditionalOnClass({OncePerRequestFilter.class, SecurityContextHolder.class})
public class UserContextAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(UserContextFilter.class)
    public UserContextFilter userContextFilter() {
        return new UserContextFilter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "userContextFilterRegistration")
    public FilterRegistrationBean<UserContextFilter> userContextFilterRegistration(UserContextFilter filter) {
        FilterRegistrationBean<UserContextFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 100);
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(ContextPropagatingTaskDecorator.class)
    public ContextPropagatingTaskDecorator contextPropagatingTaskDecorator() {
        return new ContextPropagatingTaskDecorator();
    }

    @Bean
    @ConditionalOnClass(ThreadPoolTaskExecutorCustomizer.class)
    @ConditionalOnMissingBean(name = "contextPropagatingThreadPoolCustomizer")
    public ThreadPoolTaskExecutorCustomizer contextPropagatingThreadPoolCustomizer(ContextPropagatingTaskDecorator decorator) {
        return executor -> executor.setTaskDecorator(decorator);
    }

    @Bean
    @ConditionalOnClass(SimpleAsyncTaskExecutorCustomizer.class)
    @ConditionalOnMissingBean(name = "contextPropagatingSimpleAsyncCustomizer")
    public SimpleAsyncTaskExecutorCustomizer contextPropagatingSimpleAsyncCustomizer(ContextPropagatingTaskDecorator decorator) {
        return executor -> executor.setTaskDecorator(decorator);
    }
}
