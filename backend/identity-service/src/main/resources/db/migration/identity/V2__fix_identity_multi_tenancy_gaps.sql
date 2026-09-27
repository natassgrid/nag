-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors

-- ============================================================
-- Fix Identity Multi-Tenancy Hardening & Gaps (Issue #133)
-- ============================================================

-- 1. Compound Unique Constraints for Tenant Isolation
ALTER TABLE identity_service.user_account
    ADD CONSTRAINT uq_user_account_tenant_username UNIQUE (tenant_id, username);

ALTER TABLE identity_service.user_account
    ADD CONSTRAINT uq_user_account_tenant_email_hash UNIQUE (tenant_id, email_hash);

ALTER TABLE identity_service.user_account
    ADD CONSTRAINT uq_user_account_tenant_mobile_hash UNIQUE (tenant_id, mobile_hash);

ALTER TABLE identity_service.user_role_assignment
    ADD CONSTRAINT uq_user_role_assignment_tenant_user_role UNIQUE (tenant_id, user_id, role);

-- 2. Foreign Key Constraints with Cascading Deletions
ALTER TABLE identity_service.user_role_assignment
    ADD CONSTRAINT fk_user_role_assignment_user
    FOREIGN KEY (user_id) REFERENCES identity_service.user_account(id) ON DELETE CASCADE;

ALTER TABLE identity_service.otp_verification
    ADD CONSTRAINT fk_otp_verification_user
    FOREIGN KEY (user_id) REFERENCES identity_service.user_account(id) ON DELETE CASCADE;

ALTER TABLE identity_service.active_session
    ADD CONSTRAINT fk_active_session_user
    FOREIGN KEY (user_id) REFERENCES identity_service.user_account(id) ON DELETE CASCADE;

ALTER TABLE identity_service.webauthn_credential
    ADD CONSTRAINT fk_webauthn_credential_user
    FOREIGN KEY (user_id) REFERENCES identity_service.user_account(id) ON DELETE CASCADE;

-- 3. Column Defaults for JPA Many-to-Many Join Table (role_permission)
ALTER TABLE identity_service.role_permission
    ALTER COLUMN id SET DEFAULT gen_random_uuid();

ALTER TABLE identity_service.role_permission
    ALTER COLUMN tenant_id SET DEFAULT 'default';

ALTER TABLE identity_service.role_permission
    ALTER COLUMN created_at SET DEFAULT NOW();

ALTER TABLE identity_service.role_permission
    ALTER COLUMN updated_at SET DEFAULT NOW();

ALTER TABLE identity_service.role_permission
    ALTER COLUMN version SET DEFAULT 0;

-- 4. High-Performance Composite Multi-Tenant Indexes
CREATE INDEX IF NOT EXISTS idx_active_session_tenant_token
    ON identity_service.active_session(tenant_id, session_token);

CREATE INDEX IF NOT EXISTS idx_active_session_tenant_user
    ON identity_service.active_session(tenant_id, user_id);

CREATE INDEX IF NOT EXISTS idx_otp_verification_tenant_mobile_verified_exp
    ON identity_service.otp_verification(tenant_id, mobile_hash, verified, expires_at DESC);

CREATE INDEX IF NOT EXISTS idx_otp_verification_tenant_email_verified_exp
    ON identity_service.otp_verification(tenant_id, email_hash, verified, expires_at DESC);

CREATE INDEX IF NOT EXISTS idx_otp_verification_tenant_user
    ON identity_service.otp_verification(tenant_id, user_id);

CREATE INDEX IF NOT EXISTS idx_user_role_assignment_tenant_user
    ON identity_service.user_role_assignment(tenant_id, user_id);

CREATE INDEX IF NOT EXISTS idx_webauthn_credential_tenant_user
    ON identity_service.webauthn_credential(tenant_id, user_id);
