-- =============================================================================
-- Seed Data — Test users and roles for development
-- email_hash = SHA-256(username), mobile_hash = SHA-256(username + "_mobile")
-- Password validation is bypassed in 'docker'/'dev' profile (DevKeycloakService)
--
-- IDs are UUID v7 format:
--   018f4e2a-0000-7000-8000-0000000000xx  →  user_account rows  (xx = 01..10)
--   018f4e2a-0000-7001-8000-0000000000xx  →  user_role_assignment rows
--
-- UUID v7 anatomy:  tttttttt-tttt-7sss-vrrr-rrrrrrrrrrrr
--   t = 48-bit Unix ms timestamp  (018f4e2a0000 = 2024-01-15 approx.)
--   7 = version nibble
--   v = variant bits (8..b)
-- =============================================================================

INSERT INTO identity_service.user_account (
    id, tenant_id, username, email_hash, mobile_hash,
    account_status, mfa_enabled, failed_attempt_count,
    created_at, updated_at, version
) VALUES
('018f4e2a-0000-7000-8000-000000000001', 'exam-authority-1', 'superadmin',
 '186cf774c97b60a1c106ef718d10970a6a06e06bef89553d9ae65d938a886eae',
 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('018f4e2a-0000-7000-8000-000000000002', 'exam-authority-1', 'secadmin',
 '429143a61064c974ed58080b7d7ea39cf806571abf7ec5f9f79f0faba5f1ec0b',
 'b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('018f4e2a-0000-7000-8000-000000000003', 'exam-authority-1', 'author1',
 '1d0e1b1bd678143a050f6cb90e6ebdfd2927c81b1321e1fc01f2e2490f1459c7',
 'c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('018f4e2a-0000-7000-8000-000000000004', 'exam-authority-1', 'reviewer1',
 '674ea6de0c758c87a3a9156288164e9781ae2d5cf765824c4e2ee7ed0756b73a',
 'd3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('018f4e2a-0000-7000-8000-000000000005', 'exam-authority-1', 'controller1',
 '3bf7bd3a4c57518cb4d1622284d6b4b6769b6a1c3b5973ae770226f7b5ba839b',
 'e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('018f4e2a-0000-7000-8000-000000000006', 'exam-authority-1', 'candidate1',
 'e75c44736ae122cf1ec886b703515008e1be7dd934cdf94239d7edb202eb52df',
 'f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('018f4e2a-0000-7000-8000-000000000007', 'exam-authority-1', 'translator1',
 '24d1de25d350ae44f9ac21ca873d279f37b05c5cb59b8e71b6c136f8d1ff7df6',
 'a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('018f4e2a-0000-7000-8000-000000000008', 'exam-authority-1', 'evaluator1',
 'baec845d9fd8d0728a11a32c237f79edec6bc30d416b01f97ba255998746ab79',
 'b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b100',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('018f4e2a-0000-7000-8000-000000000009', 'exam-authority-1', 'auditor1',
 'a8e7654aed6072f7d449a51607c5164bfdc3bb0f96b2d83f978f526ce9d2b576',
 'c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c200',
 'ACTIVE', false, 0, NOW(), NOW(), 0),
('018f4e2a-0000-7000-8000-00000000000a', 'exam-authority-1', 'approver1',
 'e973de4808e055b3238a774fbfba4bea8f563b24ff0d60fdb84023588251786f',
 'd3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d3e4f5a6b1c2d300',
 'ACTIVE', false, 0, NOW(), NOW(), 0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO identity_service.user_role_assignment (
    id, tenant_id, user_id, role, assigned_by, assigned_at,
    created_at, updated_at, version
) VALUES
('018f4e2a-0000-7001-8000-000000000001', 'exam-authority-1',
 '018f4e2a-0000-7000-8000-000000000001', 'SUPER_ADMIN', NULL, NOW(), NOW(), NOW(), 0),
('018f4e2a-0000-7001-8000-000000000002', 'exam-authority-1',
 '018f4e2a-0000-7000-8000-000000000002', 'SECURITY_ADMIN',
 '018f4e2a-0000-7000-8000-000000000001', NOW(), NOW(), NOW(), 0),
('018f4e2a-0000-7001-8000-000000000003', 'exam-authority-1',
 '018f4e2a-0000-7000-8000-000000000003', 'QUESTION_AUTHOR',
 '018f4e2a-0000-7000-8000-000000000001', NOW(), NOW(), NOW(), 0),
('018f4e2a-0000-7001-8000-000000000004', 'exam-authority-1',
 '018f4e2a-0000-7000-8000-000000000004', 'REVIEWER',
 '018f4e2a-0000-7000-8000-000000000001', NOW(), NOW(), NOW(), 0),
('018f4e2a-0000-7001-8000-000000000005', 'exam-authority-1',
 '018f4e2a-0000-7000-8000-000000000005', 'EXAM_CONTROLLER',
 '018f4e2a-0000-7000-8000-000000000001', NOW(), NOW(), NOW(), 0),
('018f4e2a-0000-7001-8000-000000000006', 'exam-authority-1',
 '018f4e2a-0000-7000-8000-000000000006', 'CANDIDATE',
 '018f4e2a-0000-7000-8000-000000000001', NOW(), NOW(), NOW(), 0),
('018f4e2a-0000-7001-8000-000000000007', 'exam-authority-1',
 '018f4e2a-0000-7000-8000-000000000007', 'TRANSLATOR',
 '018f4e2a-0000-7000-8000-000000000001', NOW(), NOW(), NOW(), 0),
('018f4e2a-0000-7001-8000-000000000008', 'exam-authority-1',
 '018f4e2a-0000-7000-8000-000000000008', 'EVALUATOR',
 '018f4e2a-0000-7000-8000-000000000001', NOW(), NOW(), NOW(), 0),
('018f4e2a-0000-7001-8000-000000000009', 'exam-authority-1',
 '018f4e2a-0000-7000-8000-000000000009', 'AUDITOR',
 '018f4e2a-0000-7000-8000-000000000001', NOW(), NOW(), NOW(), 0),
('018f4e2a-0000-7001-8000-00000000000a', 'exam-authority-1',
 '018f4e2a-0000-7000-8000-00000000000a', 'APPROVER',
 '018f4e2a-0000-7000-8000-000000000001', NOW(), NOW(), NOW(), 0)
ON CONFLICT (id) DO NOTHING;
