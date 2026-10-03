-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors

-- ============================================================
-- Add profile fields to user_account table (Issue #190)
-- ============================================================

ALTER TABLE identity_service.user_account
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS department VARCHAR(255);
