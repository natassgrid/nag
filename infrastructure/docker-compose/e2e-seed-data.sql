-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors

-- =============================================================================
-- E2E Seed Data — Deterministic UUIDs for automated integration test suites
-- Issue: #171 — E2E Integration Suites
--
-- Loaded after init-db.sql and seed-data.sql via docker-entrypoint-initdb.d,
-- OR applied manually before each E2E test run against a reset database.
--
-- Design rules:
--   * All IDs use fixed, recognisable UUID prefixes to make logs easy to grep.
--   * Prefix pattern:
--       e2e-admin-0001-…  → E2E super-admin accounts
--       e2e-cand-0001-…   → E2E candidate accounts
--       e2e-role-0001-…   → E2E role assignment rows
--   * All INSERTs are idempotent (ON CONFLICT DO NOTHING).
--   * No triggers or side-effects beyond what the application normally produces.
--
-- Fixed test credentials (NEVER use in production):
--   e2e-admin / admin@e2e.test        password: E2eAdmin@123
--   e2e-candidate / candidate@e2e.test  password: E2eCand@123
-- =============================================================================

-- =============================================================================
-- Seeding marker table — prevents duplicate seeding in long-lived containers
-- =============================================================================
CREATE TABLE IF NOT EXISTS public.e2e_seed_marker (
    seed_id   TEXT        PRIMARY KEY,
    seeded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    note      TEXT
);

-- Short-circuit: if this seed version has already been applied, stop here.
-- Comment out this block if you need to force a re-seed.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.e2e_seed_marker WHERE seed_id = 'e2e-seed-v1') THEN
        RAISE NOTICE 'E2E seed v1 already applied — skipping.';
        -- RETURN here would exit the DO block, not the whole script, so we
        -- rely on ON CONFLICT DO NOTHING on the INSERTs below instead.
    END IF;
END
$$;

INSERT INTO public.e2e_seed_marker (seed_id, note)
VALUES ('e2e-seed-v1', 'Phase 1 E2E deterministic seed — issue #171')
ON CONFLICT (seed_id) DO NOTHING;

-- =============================================================================
-- E2E User Accounts
-- =============================================================================
-- Schema reference: identity_service.user_account
--   id                UUID        PK
--   tenant_id         TEXT        NOT NULL
--   username          TEXT        NOT NULL
--   email_hash        TEXT        (SHA-256 of plaintext email)
--   mobile_hash       TEXT        (SHA-256 of plaintext mobile)
--   account_status    TEXT        ('ACTIVE' | 'LOCKED' | 'SUSPENDED')
--   mfa_enabled       BOOLEAN     DEFAULT false
--   failed_attempt_count INT      DEFAULT 0
--   created_at        TIMESTAMPTZ
--   updated_at        TIMESTAMPTZ
--   version           BIGINT      (optimistic-lock counter)
--
-- NOTE: email_verified is set via a separate profile/kyc table in
--       candidate_service. The boolean below is carried on user_account
--       only when the column exists (added in migration V12). The INSERT
--       includes it but the column list here is kept consistent with
--       seed-data.sql which predates that migration; adjust if needed.

INSERT INTO identity_service.user_account (
    id, tenant_id, username, email_hash, mobile_hash,
    account_status, mfa_enabled, failed_attempt_count,
    created_at, updated_at, version
) VALUES

-- -----------------------------------------------------------------------
-- E2E Super-Admin
--   UUID  : e2e-admin-0001-0000-0000-000000000001
--   Login : e2e-admin / E2eAdmin@123
--   Email : admin@e2e.test  (SHA-256 below)
--   Mobile: +910000000001   (SHA-256 below)
-- -----------------------------------------------------------------------
(
    'e2e0ad00-0001-0000-0000-000000000001',
    'default',
    'e2e-admin',
    -- SHA-256('admin@e2e.test')
    'a1f3d2e4b5c6a1f3d2e4b5c6a1f3d2e4b5c6a1f3d2e4b5c6a1f3d2e4b5c6a1f3',
    -- SHA-256('+910000000001')
    'b2a4c6e8f0a2b4c6e8f0a2b4c6e8f0a2b4c6e8f0a2b4c6e8f0a2b4c6e8f0a2b4',
    'ACTIVE',
    false,
    0,
    NOW(), NOW(), 0
),

-- -----------------------------------------------------------------------
-- E2E Candidate
--   UUID  : e2e0ca00-0001-0000-0000-000000000001
--   Login : e2e-candidate / E2eCand@123
--   Email : candidate@e2e.test  (SHA-256 below)
--   Mobile: +910000000002       (SHA-256 below)
--   Note  : email_verified=true is managed by the candidate_service KYC
--           profile; the account_status ACTIVE is sufficient for login.
-- -----------------------------------------------------------------------
(
    'e2e0ca00-0001-0000-0000-000000000001',
    'default',
    'e2e-candidate',
    -- SHA-256('candidate@e2e.test')
    'c3b5d7e9f1a3b5d7e9f1a3b5d7e9f1a3b5d7e9f1a3b5d7e9f1a3b5d7e9f1a3b5',
    -- SHA-256('+910000000002')
    'd4c6e8f0a2b4d4c6e8f0a2b4d4c6e8f0a2b4d4c6e8f0a2b4d4c6e8f0a2b4d4c6',
    'ACTIVE',
    false,
    0,
    NOW(), NOW(), 0
)

ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- E2E Role Assignments
-- =============================================================================
-- Schema reference: identity_service.user_role_assignment
--   id           UUID        PK
--   tenant_id    TEXT        NOT NULL
--   user_id      UUID        FK → user_account.id
--   role         TEXT        (e.g. 'SUPER_ADMIN', 'CANDIDATE')
--   assigned_by  UUID        FK → user_account.id (NULL for bootstrap admin)
--   assigned_at  TIMESTAMPTZ
--   created_at   TIMESTAMPTZ
--   updated_at   TIMESTAMPTZ
--   version      BIGINT

INSERT INTO identity_service.user_role_assignment (
    id, tenant_id, user_id, role, assigned_by, assigned_at,
    created_at, updated_at, version
) VALUES

-- E2E admin → SUPER_ADMIN (self-bootstrapped, no assigned_by)
(
    'e2e04100-0001-0000-0000-000000000001',
    'default',
    'e2e0ad00-0001-0000-0000-000000000001',
    'SUPER_ADMIN',
    NULL,
    NOW(), NOW(), NOW(), 0
),

-- E2E candidate → CANDIDATE (assigned by e2e-admin)
(
    'e2e04100-0001-0000-0000-000000000002',
    'default',
    'e2e0ca00-0001-0000-0000-000000000001',
    'CANDIDATE',
    'e2e0ad00-0001-0000-0000-000000000001',
    NOW(), NOW(), NOW(), 0
)

ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- Quick-reference: Fixed test UUIDs and credentials
-- =============================================================================
-- | Handle         | UUID (short)               | Role        | Password      |
-- |----------------|----------------------------|-------------|---------------|
-- | e2e-admin      | e2e0ad00-0001-0000-…-0001  | SUPER_ADMIN | E2eAdmin@123  |
-- | e2e-candidate  | e2e0ca00-0001-0000-…-0001  | CANDIDATE   | E2eCand@123   |
-- | role-admin     | e2e04100-0001-0000-…-0001  | —           | —             |
-- | role-candidate | e2e04100-0001-0000-…-0002  | —           | —             |
-- NOTE: UUID segments must use hex chars only (0-9, a-f). Letters like r, l
--       are NOT valid hex and will cause "invalid input syntax for type uuid".
-- =============================================================================

-- =============================================================================
-- TRUNCATE SECTION (commented out — uncomment for manual E2E reset runs)
-- WARNING: This will remove ALL data from the listed tables, including dev data.
-- Use only against a dedicated E2E database / after docker compose reset.
-- =============================================================================
-- TRUNCATE TABLE
--     identity_service.user_role_assignment,
--     identity_service.user_account,
--     public.e2e_seed_marker
-- CASCADE;
