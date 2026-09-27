/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) — Open Digital Public Infrastructure (DPI) Platform
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
 */
package com.examplatform.e2e.config;

import com.examplatform.e2e.util.DbResetUtil;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit 5 extension that resets shared E2E state before every test method
 * and logs a completion banner after all tests in a class have run.
 *
 * <p>Register on a test class with:
 * <pre>{@code @ExtendWith(E2ETestExtension.class)}</pre>
 *
 * <p>The {@code beforeEach} hook calls {@link DbResetUtil#reset()} which:
 * <ul>
 *   <li>Truncates transactional tables in PostgreSQL</li>
 *   <li>Flushes {@code e2e:*} keys in Redis</li>
 *   <li>Resets WireMock scenario state</li>
 *   <li>Deletes all messages from MailHog</li>
 * </ul>
 * Each step is wrapped in try-catch so a single cleanup failure does not
 * abort the entire test run.
 */
public class E2ETestExtension implements BeforeEachCallback, AfterAllCallback {

    private static final Logger LOG = LoggerFactory.getLogger(E2ETestExtension.class);

    @Override
    public void beforeEach(ExtensionContext context) {
        DbResetUtil.reset();
        LOG.info("[E2E] DB reset completed for test: {}", context.getDisplayName());
    }

    @Override
    public void afterAll(ExtensionContext context) {
        LOG.info("[E2E] All tests in class completed: {}",
                context.getTestClass().map(Class::getSimpleName).orElse("unknown"));
    }
}
