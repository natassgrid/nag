-- =============================================================================
-- Seed Data — Test users and roles for development
-- Only runs via Flyway after V1 schema is created
-- =============================================================================

INSERT INTO identity_service.user_account (
    id, tenant_id, username, email_hash, mobile_hash,
    account_status, mfa_enabled, failed_attempt_count,
    created_at, updated_at, version
) VALUES
('a0000001-0000-0000-0000-000000000001', 'exam-authority-1', 'superadmin',
 'e3b0c44298fc1c149afbf4c8996fb924', 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('a0000002-0000-0000-0000-000000000002', 'exam-authority-1', 'secadmin',
 'b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4', 'c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('a0000003-0000-0000-0000-000000000003', 'exam-authority-1', 'author1',
 'd3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6', 'e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('a0000004-0000-0000-0000-000000000004', 'exam-authority-1', 'reviewer1',
 'f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2', 'a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('a0000005-0000-0000-0000-000000000005', 'exam-authority-1', 'controller1',
 'b1c2d3e4f5a6b1c2d3e4f5a6b1c2d300', 'c2d3e4f5a6b1c2d3e4f5a6b1c2d300',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('a0000006-0000-0000-0000-000000000006', 'exam-authority-1', 'candidate1',
 'c2d3e4f5a6b1c2d3e4f5a6b1c2d3e401', 'd3e4f5a6b1c2d3e4f5a6b1c2d3e401',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('a0000007-0000-0000-0000-000000000007', 'exam-authority-1', 'translator1',
 'd3e4f5a6b1c2d3e4f5a6b1c2d3e4f502', 'e4f5a6b1c2d3e4f5a6b1c2d3e4f502',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('a0000008-0000-0000-0000-000000000008', 'exam-authority-1', 'evaluator1',
 'e4f5a6b1c2d3e4f5a6b1c2d3e4f5a603', 'f5a6b1c2d3e4f5a6b1c2d3e4f5a603',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('a0000009-0000-0000-0000-000000000009', 'exam-authority-1', 'auditor1',
 'f5a6b1c2d3e4f5a6b1c2d3e4f5a6b104', 'a6b1c2d3e4f5a6b1c2d3e4f5a6b104',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('a0000010-0000-0000-0000-000000000010', 'exam-authority-1', 'approver1',
 'a6b1c2d3e4f5a6b1c2d3e4f5a6b1c205', 'b1c2d3e4f5a6b1c2d3e4f5a6b1c205',
 'ACTIVE', false, 0, NOW(), NOW(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO identity_service.user_role_assignment (
    id, tenant_id, user_id, role, assigned_by, assigned_at,
    created_at, updated_at, version
) VALUES
('b0000001-0000-0000-0000-000000000001', 'exam-authority-1',
 'a0000001-0000-0000-0000-000000000001', 'SUPER_ADMIN', NULL, NOW(), NOW(), NOW(), 0),
('b0000002-0000-0000-0000-000000000002', 'exam-authority-1',
 'a0000002-0000-0000-0000-000000000002', 'SECURITY_ADMIN',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0),
('b0000003-0000-0000-0000-000000000003', 'exam-authority-1',
 'a0000003-0000-0000-0000-000000000003', 'QUESTION_AUTHOR',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0),
('b0000004-0000-0000-0000-000000000004', 'exam-authority-1',
 'a0000004-0000-0000-0000-000000000004', 'REVIEWER',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0),
('b0000005-0000-0000-0000-000000000005', 'exam-authority-1',
 'a0000005-0000-0000-0000-000000000005', 'EXAM_CONTROLLER',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0),
('b0000006-0000-0000-0000-000000000006', 'exam-authority-1',
 'a0000006-0000-0000-0000-000000000006', 'CANDIDATE',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0),
('b0000007-0000-0000-0000-000000000007', 'exam-authority-1',
 'a0000007-0000-0000-0000-000000000007', 'TRANSLATOR',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0),
('b0000008-0000-0000-0000-000000000008', 'exam-authority-1',
 'a0000008-0000-0000-0000-000000000008', 'EVALUATOR',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0),
('b0000009-0000-0000-0000-000000000009', 'exam-authority-1',
 'a0000009-0000-0000-0000-000000000009', 'AUDITOR',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0),
('b0000010-0000-0000-0000-000000000010', 'exam-authority-1',
 'a0000010-0000-0000-0000-000000000010', 'APPROVER',
 'a0000001-0000-0000-0000-000000000001', NOW(), NOW(), NOW(), 0)
ON CONFLICT (id) DO NOTHING;
