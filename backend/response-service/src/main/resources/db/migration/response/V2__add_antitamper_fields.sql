-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors

-- Add anti-tamper telemetry fields to response table (SPEC-R2)
ALTER TABLE response_service.response
    ADD COLUMN IF NOT EXISTS client_ip          VARCHAR(45),
    ADD COLUMN IF NOT EXISTS user_agent         TEXT,
    ADD COLUMN IF NOT EXISTS focus_loss_count   INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS integrity_checksum VARCHAR(64);

-- Partial index for fast final-response queries (SPEC-R1)
CREATE INDEX IF NOT EXISTS idx_response_session_is_final
    ON response_service.response(session_id, is_final)
    WHERE is_final = TRUE;

COMMENT ON COLUMN response_service.response.client_ip IS 'Client IP address from X-Forwarded-For header for audit/anti-tamper';
COMMENT ON COLUMN response_service.response.user_agent IS 'Browser user-agent string for anti-tamper telemetry';
COMMENT ON COLUMN response_service.response.focus_loss_count IS 'Number of browser focus-loss / tab-switch events reported by client';
COMMENT ON COLUMN response_service.response.integrity_checksum IS 'HMAC-SHA256 of (sessionId+questionId+answer+timestamp), submitted by client';
