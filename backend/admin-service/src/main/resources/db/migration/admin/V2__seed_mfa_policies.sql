-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors

INSERT INTO admin_service.system_config (id, tenant_id, param_name, param_value, updated_by, updated_at_config, created_at, updated_at, version)
VALUES
    ('018f4e2b-0010-7000-8000-000000000007', 'default', 'auth.mfa.admin.policy', 'OPTIONAL', NULL, NOW(), NOW(), NOW(), 0),
    ('018f4e2b-0010-7000-8000-000000000008', 'default', 'auth.mfa.candidate.policy', 'OPTIONAL', NULL, NOW(), NOW(), NOW(), 0),
    ('018f4e2b-0010-7000-8000-000000000009', 'default', 'auth.mfa.allowed.methods', 'TOTP,EMAIL_OTP,RECOVERY_CODES', NULL, NOW(), NOW(), NOW(), 0)
ON CONFLICT (id) DO NOTHING;
