CREATE SCHEMA IF NOT EXISTS audit_service;

-- ============================================================
-- Table: audit_event
-- Immutable audit log. Does NOT extend BaseEntity.
-- ============================================================
CREATE TABLE audit_service.audit_event (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type          VARCHAR(100) NOT NULL,
    actor_id            UUID NOT NULL,
    resource            VARCHAR(500) NOT NULL,
    ip_address          VARCHAR(45),
    device_fingerprint  VARCHAR(255),
    occurred_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    tenant_id           VARCHAR(255) NOT NULL,
    payload_hash        VARCHAR(128) NOT NULL,
    hsm_signature       VARCHAR(512),
    signing_key_id      VARCHAR(100),
    event_payload       JSONB,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_event_tenant_id ON audit_service.audit_event(tenant_id);
CREATE INDEX idx_audit_event_event_type ON audit_service.audit_event(event_type);
CREATE INDEX idx_audit_event_actor_id ON audit_service.audit_event(actor_id);
CREATE INDEX idx_audit_event_occurred_at ON audit_service.audit_event(occurred_at);
