CREATE SCHEMA IF NOT EXISTS notification_service;

-- ============================================================
-- Table: notification
-- ============================================================
CREATE TABLE notification_service.notification (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(255) NOT NULL,
    user_id         UUID NOT NULL,
    recipient_email VARCHAR(320),
    type            VARCHAR(10) NOT NULL,
    subject         VARCHAR(500),
    body            TEXT NOT NULL,
    status          VARCHAR(15) NOT NULL,
    retry_count     INTEGER NOT NULL DEFAULT 0,
    sent_at         TIMESTAMP,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_notification_tenant_id ON notification_service.notification(tenant_id);
CREATE INDEX idx_notification_user_id ON notification_service.notification(user_id);
CREATE INDEX idx_notification_status ON notification_service.notification(status);
