-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors

-- ============================================================
-- Add profile preferences and Personal Access Tokens (Issue #154)
-- ============================================================

ALTER TABLE identity_service.user_account
    ADD COLUMN IF NOT EXISTS designation VARCHAR(150),
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS timezone VARCHAR(100) DEFAULT 'Asia/Kolkata',
    ADD COLUMN IF NOT EXISTS date_format VARCHAR(50) DEFAULT 'DD/MM/YYYY',
    ADD COLUMN IF NOT EXISTS time_format VARCHAR(20) DEFAULT '24h',
    ADD COLUMN IF NOT EXISTS preferred_language VARCHAR(50) DEFAULT 'en',
    ADD COLUMN IF NOT EXISTS theme_preference VARCHAR(30) DEFAULT 'system';

CREATE TABLE IF NOT EXISTS identity_service.personal_access_token (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    token_prefix VARCHAR(16) NOT NULL,
    scopes VARCHAR(500) NOT NULL,
    ip_whitelist VARCHAR(255),
    expires_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id VARCHAR(50) NOT NULL DEFAULT 'default',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pat_user_tenant ON identity_service.personal_access_token (user_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_pat_token_hash ON identity_service.personal_access_token (token_hash);
