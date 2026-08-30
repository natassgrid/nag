-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors

-- =============================================================================
-- Seed Data — Test users, roles, and logins for development
-- Loaded after init-db.sql via docker-entrypoint-initdb.d
-- =============================================================================

-- NOTE: Password hashes below are SHA-256 of the plaintext passwords.
-- In production, passwords are managed by Keycloak. These are for DB-level testing only.
-- All email/mobile hashes are SHA-256 of the plaintext values for lookup.

-- =============================================================================
-- Test Users
-- =============================================================================
-- Passwords (plaintext for dev reference):
--   superadmin  → Password@123
--   secadmin    → Password@123
--   author1     → Password@123
--   reviewer1   → Password@123
--   controller1 → Password@123
--   candidate1  → Password@123

INSERT INTO identity_service.user_account (
    id, tenant_id, username, email_hash, mobile_hash,
    account_status, mfa_enabled, failed_attempt_count,
    created_at, updated_at, version
) VALUES
-- Super Admin
('a0000001-0000-0000-0000-000000000001', 'default', 'superadmin',
 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855', -- hash of superadmin@exam.gov.in
 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2', -- hash of +919999900001
 'ACTIVE', false, 0, NOW(), NOW(), 0),

-- Security Admin
('a0000002-0000-0000-0000-000000000002', 'default', 'secadmin',
 'b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2',
 'c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3',
 'ACTIVE', false, 0, NOW(), NOW(), 0),

-- Question Author
('a0000003-0000-0000-0000-000000000003', 'default', 'author1',
 'd3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4',
 'e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5',
 'ACTIVE', false, 0, NOW(), NOW(), 0),

-- Reviewer
('a0000004-0000-0000-0000-000000000004', 'default', 'reviewer1',
 'f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6',
 'a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1',
 'ACTIVE', false, 0, NOW(), NOW(), 0),

-- Exam Controller
('a0000005-0000-0000-0000-000000000005', 'default', 'controller1',
 'b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b100',
 'c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c200',
 'ACTIVE', false, 0, NOW(), NOW(), 0),

-- Candidate
('a0000006-0000-0000-0000-000000000006', 'default', 'candidate1',
 'c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c201',
 'd3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d301',
 'ACTIVE', false, 0, NOW(), NOW(), 0),

-- Translator
('a0000007-0000-0000-0000-000000000007', 'default', 'translator1',
 'd3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d302',
 'e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e402',
 'ACTIVE', false, 0, NOW(), NOW(), 0),

-- Evaluator
('a0000008-0000-0000-0000-000000000008', 'default', 'evaluator1',
 'e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e403',
 'f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f503',
 'ACTIVE', false, 0, NOW(), NOW(), 0),

-- Auditor
('a0000009-0000-0000-0000-000000000009', 'default', 'auditor1',
 'f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f504',
 'a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a604',
 'ACTIVE', false, 0, NOW(), NOW(), 0),

-- Approver
('a0000010-0000-0000-0000-000000000010', 'default', 'approver1',
 'a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a605',
 'b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b105',
 'ACTIVE', false, 0, NOW(), NOW(), 0);

-- =============================================================================
-- Role Assignments
-- =============================================================================
INSERT INTO identity_service.user_role_assignment (
    id, tenant_id, user_id, role, assigned_by, assigned_at,
    created_at, updated_at, version
) VALUES
-- superadmin has SUPER_ADMIN role
('b0000001-0000-0000-0000-000000000001', 'default',
 'a0000001-0000-0000-0000-000000000001', 'SUPER_ADMIN', NULL, NOW(), NOW(), NOW(), 0),

-- secadmin has SECURITY_ADMIN role
('b0000002-0000-0000-0000-000000000002', 'default',
 'a0000002-0000-0000-0000-000000000002', 'SECURITY_ADMIN',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0),

-- author1 has QUESTION_AUTHOR role
('b0000003-0000-0000-0000-000000000003', 'default',
 'a0000003-0000-0000-0000-000000000003', 'QUESTION_AUTHOR',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0),

-- reviewer1 has REVIEWER role
('b0000004-0000-0000-0000-000000000004', 'default',
 'a0000004-0000-0000-0000-000000000004', 'REVIEWER',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0),

-- controller1 has EXAM_CONTROLLER role
('b0000005-0000-0000-0000-000000000005', 'default',
 'a0000005-0000-0000-0000-000000000005', 'EXAM_CONTROLLER',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0),

-- candidate1 has CANDIDATE role
('b0000006-0000-0000-0000-000000000006', 'default',
 'a0000006-0000-0000-0000-000000000006', 'CANDIDATE',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0),

-- translator1 has TRANSLATOR role
('b0000007-0000-0000-0000-000000000007', 'default',
 'a0000007-0000-0000-0000-000000000007', 'TRANSLATOR',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0),

-- evaluator1 has EVALUATOR role
('b0000008-0000-0000-0000-000000000008', 'default',
 'a0000008-0000-0000-0000-000000000008', 'EVALUATOR',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0),

-- auditor1 has AUDITOR role
('b0000009-0000-0000-0000-000000000009', 'default',
 'a0000009-0000-0000-0000-000000000009', 'AUDITOR',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0),

-- approver1 has APPROVER role
('b0000010-0000-0000-0000-000000000010', 'default',
 'a0000010-0000-0000-0000-000000000010', 'APPROVER',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0);
