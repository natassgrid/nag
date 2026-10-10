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

package com.examplatform.shared.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hardcoded, fail-safe platform default configuration parameters.
 * Used as L3 fallback across all microservices if Redis/AdminService is unreachable.
 */
public final class DefaultPlatformConfigs {

    private DefaultPlatformConfigs() {}

    private static final String DEFAULT_CONFIGS_RAW = """
            auth.mfa.enforced=false
            auth.mfa.admin.policy=OPTIONAL
            auth.mfa.candidate.policy=OPTIONAL
            auth.mfa.allowed.methods=TOTP,EMAIL_OTP,RECOVERY_CODES
            auth.stepup.enforced=false
            auth.session.timeout.minutes=30
            auth.max.login.attempts=5
            auth.password.expiry.days=90
            auth.password.min.length=12
            auth.lockout.duration.minutes=15
            delivery.tamper.detection.enabled=true
            delivery.kiosk.mode.enforced=true
            delivery.telemetry.heartbeat.seconds=10
            delivery.autosave.interval.seconds=15
            delivery.max.disconnect.grace.seconds=180
            delivery.retest.authorization.required=true
            practice.mode.enabled=true
            practice.solutions.visible=true
            question.dual.review.required=true
            question.ai.generation.enabled=true
            evaluation.auto.grade.instant=true
            evaluation.anonymize.candidate.sheets=true
            alert.failed.login.spikes.enabled=true
            alert.exam.window.start.enabled=true
            alert.email.recipients=sec-ops@nag.gov.in, admin@nag.gov.in
            alert.critical.error.webhook=
            dpi.digilocker.verification.enabled=true
            dpi.face.verification.threshold=85
            platform.maintenance.mode=false
            platform.banner.message=
            """;

    public static final Map<String, String> DEFAULTS;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        DEFAULT_CONFIGS_RAW.strip().lines().forEach(line -> {
            int idx = line.indexOf('=');
            if (idx > 0) {
                m.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
            }
        });
        DEFAULTS = Collections.unmodifiableMap(m);
    }

    public static String getDefault(String paramName, String fallback) {
        return DEFAULTS.getOrDefault(paramName, fallback);
    }
}
