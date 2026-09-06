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

    public static final Map<String, String> DEFAULTS;

    static {
        Map<String, String> m = new LinkedHashMap<>();

        // Security & Authentication
        m.put("auth.mfa.enforced", "false");
        m.put("auth.stepup.enforced", "false");
        m.put("auth.session.timeout.minutes", "30");
        m.put("auth.max.login.attempts", "5");
        m.put("auth.password.expiry.days", "90");
        m.put("auth.password.min.length", "12");
        m.put("auth.lockout.duration.minutes", "15");

        // Exam Delivery & Proctoring
        m.put("delivery.tamper.detection.enabled", "true");
        m.put("delivery.kiosk.mode.enforced", "true");
        m.put("delivery.telemetry.heartbeat.seconds", "10");
        m.put("delivery.autosave.interval.seconds", "15");
        m.put("delivery.max.disconnect.grace.seconds", "180");
        m.put("delivery.retest.authorization.required", "true");

        // Candidate Practice & Learning Governance
        m.put("practice.mode.enabled", "true");
        m.put("practice.solutions.visible", "true");

        // Assessment & Question Bank Governance
        m.put("question.dual.review.required", "true");
        m.put("question.ai.generation.enabled", "true");
        m.put("evaluation.auto.grade.instant", "true");
        m.put("evaluation.anonymize.candidate.sheets", "true");

        // Alerts & Notification Operations
        m.put("alert.failed.login.spikes.enabled", "true");
        m.put("alert.exam.window.start.enabled", "true");
        m.put("alert.email.recipients", "sec-ops@nag.gov.in, admin@nag.gov.in");
        m.put("alert.critical.error.webhook", "");

        // Platform Infrastructure & DPI Integration
        m.put("dpi.digilocker.verification.enabled", "true");
        m.put("dpi.face.verification.threshold", "85");
        m.put("platform.maintenance.mode", "false");
        m.put("platform.banner.message", "");

        DEFAULTS = Collections.unmodifiableMap(m);
    }

    public static String getDefault(String paramName, String fallback) {
        return DEFAULTS.getOrDefault(paramName, fallback);
    }
}
