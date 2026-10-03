-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors

-- ============================================================
-- Add multi-channel fields to notification table
-- ============================================================
ALTER TABLE notification_service.notification ALTER COLUMN type TYPE VARCHAR(20);
ALTER TABLE notification_service.notification ADD COLUMN IF NOT EXISTS recipient_phone VARCHAR(50);
ALTER TABLE notification_service.notification ADD COLUMN IF NOT EXISTS channel VARCHAR(20);
ALTER TABLE notification_service.notification ADD COLUMN IF NOT EXISTS template_id VARCHAR(100);
ALTER TABLE notification_service.notification ADD COLUMN IF NOT EXISTS external_message_id VARCHAR(255);
ALTER TABLE notification_service.notification ADD COLUMN IF NOT EXISTS fcm_token TEXT;

CREATE INDEX IF NOT EXISTS idx_notification_recipient_phone ON notification_service.notification(recipient_phone);
CREATE INDEX IF NOT EXISTS idx_notification_channel ON notification_service.notification(channel);

-- ============================================================
-- Table: notification_preference
-- ============================================================
CREATE TABLE IF NOT EXISTS notification_service.notification_preference (
    id                  UUID PRIMARY KEY,
    tenant_id           VARCHAR(255) NOT NULL,
    user_id             UUID NOT NULL UNIQUE,
    preferred_channel   VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    phone_number        VARCHAR(50),
    email               VARCHAR(320),
    fcm_token           TEXT,
    push_enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    whatsapp_enabled    BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_notification_pref_user_id ON notification_service.notification_preference(user_id);
CREATE INDEX IF NOT EXISTS idx_notification_pref_tenant_id ON notification_service.notification_preference(tenant_id);

-- ============================================================
-- Table: device_token
-- ============================================================
CREATE TABLE IF NOT EXISTS notification_service.device_token (
    id                  UUID PRIMARY KEY,
    tenant_id           VARCHAR(255) NOT NULL,
    user_id             UUID NOT NULL,
    token               TEXT NOT NULL,
    device_type         VARCHAR(50) DEFAULT 'WEB_PWA',
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_device_token UNIQUE (user_id, token)
);

CREATE INDEX IF NOT EXISTS idx_device_token_user_id ON notification_service.device_token(user_id);
CREATE INDEX IF NOT EXISTS idx_device_token_tenant_id ON notification_service.device_token(tenant_id);
