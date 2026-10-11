-- SPDX-License-Identifier: AGPL-3.0-only
-- NAG Contributors 2026
-- Adds subtopic_wise_breakdown column to practice_session table for granular telemetry

ALTER TABLE practice_service.practice_session
    ADD COLUMN IF NOT EXISTS subtopic_wise_breakdown JSONB;
