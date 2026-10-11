-- SPDX-License-Identifier: AGPL-3.0-only
-- NAG Contributors 2026
-- Adds benchmark_time_seconds column to question table for pacing telemetry

ALTER TABLE question_service.question
    ADD COLUMN IF NOT EXISTS benchmark_time_seconds INTEGER NOT NULL DEFAULT 90;
