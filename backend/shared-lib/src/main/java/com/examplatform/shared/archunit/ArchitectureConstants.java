/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.\
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */

package com.examplatform.shared.archunit;

/**
 * Common package matchers and architecture constants for ArchUnit enforcement.
 */
public final class ArchitectureConstants {

    private ArchitectureConstants() {
        // Utility class
    }

    public static final String ROOT_PACKAGE = "com.examplatform";
    public static final String ROOT_PACKAGE_MATCHER = "com.examplatform..";

    public static final String CONTROLLER_PACKAGE = "..controller..";
    public static final String SERVICE_PACKAGE = "..service..";
    public static final String REPOSITORY_PACKAGE = "..repository..";
    public static final String DOMAIN_PACKAGE = "..domain..";
    public static final String DTO_PACKAGE = "..dto..";
    public static final String CONFIG_PACKAGE = "..config..";
    public static final String CLIENT_PACKAGE = "..client..";
    public static final String EVENT_PACKAGE = "..event..";
    public static final String ERROR_PACKAGE = "..error..";
    public static final String SECURITY_PACKAGE = "..security..";
    public static final String SHARED_PACKAGE = "com.examplatform.shared..";
}
