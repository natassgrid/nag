-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors

-- =============================================================================
-- Seed Platform Permissions
-- =============================================================================

INSERT INTO identity_service.permission (id, tenant_id, code, name, description, module)
VALUES
    -- IDENTITY Module
    ('018f4e2b-0001-7000-8000-000000000001', 'default', 'IDENTITY:USER_READ', 'View Users', 'View user accounts, statuses, and profiles', 'IDENTITY'),
    ('018f4e2b-0001-7000-8000-000000000002', 'default', 'IDENTITY:USER_WRITE', 'Manage Users', 'Create, update, and deactivate user accounts', 'IDENTITY'),
    ('018f4e2b-0001-7000-8000-000000000003', 'default', 'IDENTITY:ROLE_READ', 'View Roles', 'View role definitions, details, and permission mappings', 'IDENTITY'),
    ('018f4e2b-0001-7000-8000-000000000004', 'default', 'IDENTITY:ROLE_MANAGE', 'Manage Roles', 'Create, update, and delete custom roles and assign permissions', 'IDENTITY'),
    ('018f4e2b-0001-7000-8000-000000000005', 'default', 'IDENTITY:USER_ROLE_ASSIGN', 'Assign Roles', 'Assign and revoke roles for platform users', 'IDENTITY'),
    ('018f4e2b-0001-7000-8000-000000000006', 'default', 'IDENTITY:MFA_MANAGE', 'Manage MFA', 'Configure and reset Multi-Factor Authentication for users', 'IDENTITY'),

    -- QUESTION_BANK Module
    ('018f4e2b-0002-7000-8000-000000000001', 'default', 'QUESTION:READ', 'View Questions', 'Browse, search, and inspect question items', 'QUESTION_BANK'),
    ('018f4e2b-0002-7000-8000-000000000002', 'default', 'QUESTION:CREATE', 'Create Questions', 'Author and draft new questions in question bank', 'QUESTION_BANK'),
    ('018f4e2b-0002-7000-8000-000000000003', 'default', 'QUESTION:EDIT', 'Edit Questions', 'Modify drafted questions, choices, and metadata', 'QUESTION_BANK'),
    ('018f4e2b-0002-7000-8000-000000000004', 'default', 'QUESTION:DELETE', 'Delete Questions', 'Remove questions from the question bank', 'QUESTION_BANK'),
    ('018f4e2b-0002-7000-8000-000000000005', 'default', 'QUESTION:REVIEW', 'Review Questions', 'Perform peer review, rubric check, and question validation', 'QUESTION_BANK'),
    ('018f4e2b-0002-7000-8000-000000000006', 'default', 'QUESTION:APPROVE', 'Approve Questions', 'Approve reviewed questions for publication in blueprints', 'QUESTION_BANK'),
    ('018f4e2b-0002-7000-8000-000000000007', 'default', 'QUESTION:TRANSLATE', 'Translate Questions', 'Translate questions and options into regional languages', 'QUESTION_BANK'),
    ('018f4e2b-0002-7000-8000-000000000008', 'default', 'QUESTION:IMPORT_EXPORT', 'Import/Export Questions', 'Bulk import and export question banks (CSV, JSON, QTI)', 'QUESTION_BANK'),

    -- EXAM_MANAGEMENT Module
    ('018f4e2b-0003-7000-8000-000000000001', 'default', 'EXAM:READ', 'View Examinations', 'Browse and view exam blueprints and schedules', 'EXAM_MANAGEMENT'),
    ('018f4e2b-0003-7000-8000-000000000002', 'default', 'EXAM:CREATE', 'Create Examinations', 'Define new examination blueprints and paper templates', 'EXAM_MANAGEMENT'),
    ('018f4e2b-0003-7000-8000-000000000003', 'default', 'EXAM:SCHEDULE', 'Schedule Examinations', 'Configure time slots, test centers, and candidate allocations', 'EXAM_MANAGEMENT'),
    ('018f4e2b-0003-7000-8000-000000000004', 'default', 'EXAM:PUBLISH', 'Publish Examinations', 'Publish exam schedules for candidate registration', 'EXAM_MANAGEMENT'),
    ('018f4e2b-0003-7000-8000-000000000005', 'default', 'EXAM:CANCEL', 'Cancel Examinations', 'Cancel scheduled examination sessions and notify candidates', 'EXAM_MANAGEMENT'),

    -- EXAM_DELIVERY Module
    ('018f4e2b-0004-7000-8000-000000000001', 'default', 'DELIVERY:MONITOR', 'Live Monitoring', 'Monitor live exam sessions, heartbeats, and delivery status', 'EXAM_DELIVERY'),
    ('018f4e2b-0004-7000-8000-000000000002', 'default', 'DELIVERY:ATTEND', 'Attend Exam', 'Launch candidate exam interface and submit responses', 'EXAM_DELIVERY'),
    ('018f4e2b-0004-7000-8000-000000000003', 'default', 'DELIVERY:PROCTOR', 'Proctor Exam', 'Live proctoring oversight, anomaly flagging, and session controls', 'EXAM_DELIVERY'),
    ('018f4e2b-0004-7000-8000-000000000004', 'default', 'DELIVERY:RETEST', 'Authorize Retest', 'Issue retest authorization for affected candidates', 'EXAM_DELIVERY'),

    -- ASSESSMENT_EVALUATION Module
    ('018f4e2b-0005-7000-8000-000000000001', 'default', 'EVALUATION:READ', 'View Evaluation', 'Access candidate response sheets and answer keys', 'ASSESSMENT_EVALUATION'),
    ('018f4e2b-0005-7000-8000-000000000002', 'default', 'EVALUATION:SCORE_AUTO', 'Execute Auto-Grading', 'Run automated grading pipeline on objective responses', 'ASSESSMENT_EVALUATION'),
    ('018f4e2b-0005-7000-8000-000000000003', 'default', 'EVALUATION:SCORE_MANUAL', 'Manual Scoring', 'Evaluate subjective and descriptive candidate responses', 'ASSESSMENT_EVALUATION'),
    ('018f4e2b-0005-7000-8000-000000000004', 'default', 'EVALUATION:RESULT_PUBLISH', 'Publish Results', 'Approve final scorecards and publish merit lists', 'ASSESSMENT_EVALUATION'),

    -- AUDIT_SECURITY Module
    ('018f4e2b-0006-7000-8000-000000000001', 'default', 'AUDIT:READ', 'View Audit Logs', 'Inspect immutable audit trails and system event logs', 'AUDIT_SECURITY'),
    ('018f4e2b-0006-7000-8000-000000000002', 'default', 'AUDIT:EXPORT', 'Export Audit Logs', 'Export compliance and security audit logs', 'AUDIT_SECURITY'),
    ('018f4e2b-0006-7000-8000-000000000003', 'default', 'SECURITY:POLICY_MANAGE', 'Manage Security Policies', 'Configure password rules, IP restrictions, and session parameters', 'AUDIT_SECURITY'),

    -- ANALYTICS_REPORTS Module
    ('018f4e2b-0007-7000-8000-000000000001', 'default', 'ANALYTICS:VIEW', 'View Analytics', 'Access platform metrics, exam statistics, and item analysis', 'ANALYTICS_REPORTS'),
    ('018f4e2b-0007-7000-8000-000000000002', 'default', 'REPORTS:EXPORT', 'Export Reports', 'Generate and download administrative reports and insights', 'ANALYTICS_REPORTS')
ON CONFLICT (id) DO NOTHING;

-- =============================================================================
-- Map Permissions to Default System Roles
-- =============================================================================

-- SUPER_ADMIN: Receives ALL permissions
INSERT INTO identity_service.role_permission (id, tenant_id, role_id, permission_id)
SELECT
    gen_random_uuid(),
    'default',
    r.id,
    p.id
FROM identity_service.role_definition r
CROSS JOIN identity_service.permission p
WHERE r.code = 'SUPER_ADMIN'
ON CONFLICT (role_id, permission_id, tenant_id) DO NOTHING;

-- SECURITY_ADMIN: Identity, Audit, Security, Analytics
INSERT INTO identity_service.role_permission (id, tenant_id, role_id, permission_id)
SELECT
    gen_random_uuid(),
    'default',
    r.id,
    p.id
FROM identity_service.role_definition r
JOIN identity_service.permission p ON p.module IN ('IDENTITY', 'AUDIT_SECURITY', 'ANALYTICS_REPORTS')
WHERE r.code = 'SECURITY_ADMIN'
ON CONFLICT (role_id, permission_id, tenant_id) DO NOTHING;

-- QUESTION_AUTHOR: Question read, create, edit, import/export
INSERT INTO identity_service.role_permission (id, tenant_id, role_id, permission_id)
SELECT
    gen_random_uuid(),
    'default',
    r.id,
    p.id
FROM identity_service.role_definition r
JOIN identity_service.permission p ON p.code IN ('QUESTION:READ', 'QUESTION:CREATE', 'QUESTION:EDIT', 'QUESTION:IMPORT_EXPORT')
WHERE r.code = 'QUESTION_AUTHOR'
ON CONFLICT (role_id, permission_id, tenant_id) DO NOTHING;

-- REVIEWER: Question read, review
INSERT INTO identity_service.role_permission (id, tenant_id, role_id, permission_id)
SELECT
    gen_random_uuid(),
    'default',
    r.id,
    p.id
FROM identity_service.role_definition r
JOIN identity_service.permission p ON p.code IN ('QUESTION:READ', 'QUESTION:REVIEW')
WHERE r.code = 'REVIEWER'
ON CONFLICT (role_id, permission_id, tenant_id) DO NOTHING;

-- APPROVER: Question read, approve, Exam read
INSERT INTO identity_service.role_permission (id, tenant_id, role_id, permission_id)
SELECT
    gen_random_uuid(),
    'default',
    r.id,
    p.id
FROM identity_service.role_definition r
JOIN identity_service.permission p ON p.code IN ('QUESTION:READ', 'QUESTION:APPROVE', 'EXAM:READ')
WHERE r.code = 'APPROVER'
ON CONFLICT (role_id, permission_id, tenant_id) DO NOTHING;

-- EXAM_CONTROLLER: Exams, Delivery monitoring, proctoring, retest, result publish, analytics, reports
INSERT INTO identity_service.role_permission (id, tenant_id, role_id, permission_id)
SELECT
    gen_random_uuid(),
    'default',
    r.id,
    p.id
FROM identity_service.role_definition r
JOIN identity_service.permission p ON p.module IN ('EXAM_MANAGEMENT', 'EXAM_DELIVERY', 'ANALYTICS_REPORTS')
    OR p.code = 'EVALUATION:RESULT_PUBLISH'
WHERE r.code = 'EXAM_CONTROLLER'
ON CONFLICT (role_id, permission_id, tenant_id) DO NOTHING;

-- TRANSLATOR: Question read, translate
INSERT INTO identity_service.role_permission (id, tenant_id, role_id, permission_id)
SELECT
    gen_random_uuid(),
    'default',
    r.id,
    p.id
FROM identity_service.role_definition r
JOIN identity_service.permission p ON p.code IN ('QUESTION:READ', 'QUESTION:TRANSLATE')
WHERE r.code = 'TRANSLATOR'
ON CONFLICT (role_id, permission_id, tenant_id) DO NOTHING;

-- EVALUATOR: Evaluation read, manual scoring
INSERT INTO identity_service.role_permission (id, tenant_id, role_id, permission_id)
SELECT
    gen_random_uuid(),
    'default',
    r.id,
    p.id
FROM identity_service.role_definition r
JOIN identity_service.permission p ON p.code IN ('EVALUATION:READ', 'EVALUATION:SCORE_MANUAL')
WHERE r.code = 'EVALUATOR'
ON CONFLICT (role_id, permission_id, tenant_id) DO NOTHING;

-- AUDITOR: Audit read, export, analytics, reports
INSERT INTO identity_service.role_permission (id, tenant_id, role_id, permission_id)
SELECT
    gen_random_uuid(),
    'default',
    r.id,
    p.id
FROM identity_service.role_definition r
JOIN identity_service.permission p ON p.module IN ('AUDIT_SECURITY', 'ANALYTICS_REPORTS')
WHERE r.code = 'AUDITOR'
ON CONFLICT (role_id, permission_id, tenant_id) DO NOTHING;

-- CANDIDATE: Attend exam, read results
INSERT INTO identity_service.role_permission (id, tenant_id, role_id, permission_id)
SELECT
    gen_random_uuid(),
    'default',
    r.id,
    p.id
FROM identity_service.role_definition r
JOIN identity_service.permission p ON p.code IN ('DELIVERY:ATTEND', 'EVALUATION:READ')
WHERE r.code = 'CANDIDATE'
ON CONFLICT (role_id, permission_id, tenant_id) DO NOTHING;
