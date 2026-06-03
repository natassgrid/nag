CREATE SCHEMA IF NOT EXISTS notification_service;
SET search_path TO notification_service;

CREATE TABLE notification (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     VARCHAR(255) NOT NULL,
    user_id       UUID NOT NULL,
    type          VARCHAR(10) NOT NULL,
    subject       VARCHAR(500),
    body          TEXT NOT NULL,
    status        VARCHAR(15) NOT NULL DEFAULT 'PENDING',
    retry_count   INTEGER NOT NULL DEFAULT 0,
    sent_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    version       BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_notification_type CHECK (type IN ('EMAIL', 'PUSH', 'IN_APP')),
    CONSTRAINT chk_notification_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'UNDELIVERED'))
);

CREATE INDEX idx_notification_tenant ON notification(tenant_id);
CREATE INDEX idx_notification_user_tenant ON notification(user_id, tenant_id);
CREATE INDEX idx_notification_status ON notification(status);
CREATE INDEX idx_notification_status_retry ON notification(status, retry_count) WHERE status = 'FAILED';
