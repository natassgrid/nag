CREATE SCHEMA IF NOT EXISTS audit_service;
SET search_path TO audit_service;

-- Range-partitioned audit_event table by occurred_at (quarterly partitions)
CREATE TABLE audit_event (
    id                  UUID NOT NULL DEFAULT gen_random_uuid(),
    event_type          VARCHAR(100) NOT NULL,
    actor_id            UUID NOT NULL,
    resource            VARCHAR(500) NOT NULL,
    ip_address          VARCHAR(45),
    device_fingerprint  VARCHAR(255),
    occurred_at         TIMESTAMPTZ NOT NULL,
    tenant_id           VARCHAR(255) NOT NULL,
    payload_hash        VARCHAR(128) NOT NULL,
    hsm_signature       VARCHAR(512),
    signing_key_id      VARCHAR(100),
    event_payload       JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (id, occurred_at)
) PARTITION BY RANGE (occurred_at);

-- 28 quarterly partitions: Q1 2024 through Q4 2030 (7-year retention)
CREATE TABLE audit_event_2024_q1 PARTITION OF audit_event FOR VALUES FROM ('2024-01-01') TO ('2024-04-01');
CREATE TABLE audit_event_2024_q2 PARTITION OF audit_event FOR VALUES FROM ('2024-04-01') TO ('2024-07-01');
CREATE TABLE audit_event_2024_q3 PARTITION OF audit_event FOR VALUES FROM ('2024-07-01') TO ('2024-10-01');
CREATE TABLE audit_event_2024_q4 PARTITION OF audit_event FOR VALUES FROM ('2024-10-01') TO ('2025-01-01');

CREATE TABLE audit_event_2025_q1 PARTITION OF audit_event FOR VALUES FROM ('2025-01-01') TO ('2025-04-01');
CREATE TABLE audit_event_2025_q2 PARTITION OF audit_event FOR VALUES FROM ('2025-04-01') TO ('2025-07-01');
CREATE TABLE audit_event_2025_q3 PARTITION OF audit_event FOR VALUES FROM ('2025-07-01') TO ('2025-10-01');
CREATE TABLE audit_event_2025_q4 PARTITION OF audit_event FOR VALUES FROM ('2025-10-01') TO ('2026-01-01');

CREATE TABLE audit_event_2026_q1 PARTITION OF audit_event FOR VALUES FROM ('2026-01-01') TO ('2026-04-01');
CREATE TABLE audit_event_2026_q2 PARTITION OF audit_event FOR VALUES FROM ('2026-04-01') TO ('2026-07-01');
CREATE TABLE audit_event_2026_q3 PARTITION OF audit_event FOR VALUES FROM ('2026-07-01') TO ('2026-10-01');
CREATE TABLE audit_event_2026_q4 PARTITION OF audit_event FOR VALUES FROM ('2026-10-01') TO ('2027-01-01');

CREATE TABLE audit_event_2027_q1 PARTITION OF audit_event FOR VALUES FROM ('2027-01-01') TO ('2027-04-01');
CREATE TABLE audit_event_2027_q2 PARTITION OF audit_event FOR VALUES FROM ('2027-04-01') TO ('2027-07-01');
CREATE TABLE audit_event_2027_q3 PARTITION OF audit_event FOR VALUES FROM ('2027-07-01') TO ('2027-10-01');
CREATE TABLE audit_event_2027_q4 PARTITION OF audit_event FOR VALUES FROM ('2027-10-01') TO ('2028-01-01');

CREATE TABLE audit_event_2028_q1 PARTITION OF audit_event FOR VALUES FROM ('2028-01-01') TO ('2028-04-01');
CREATE TABLE audit_event_2028_q2 PARTITION OF audit_event FOR VALUES FROM ('2028-04-01') TO ('2028-07-01');
CREATE TABLE audit_event_2028_q3 PARTITION OF audit_event FOR VALUES FROM ('2028-07-01') TO ('2028-10-01');
CREATE TABLE audit_event_2028_q4 PARTITION OF audit_event FOR VALUES FROM ('2028-10-01') TO ('2029-01-01');

CREATE TABLE audit_event_2029_q1 PARTITION OF audit_event FOR VALUES FROM ('2029-01-01') TO ('2029-04-01');
CREATE TABLE audit_event_2029_q2 PARTITION OF audit_event FOR VALUES FROM ('2029-04-01') TO ('2029-07-01');
CREATE TABLE audit_event_2029_q3 PARTITION OF audit_event FOR VALUES FROM ('2029-07-01') TO ('2029-10-01');
CREATE TABLE audit_event_2029_q4 PARTITION OF audit_event FOR VALUES FROM ('2029-10-01') TO ('2030-01-01');

CREATE TABLE audit_event_2030_q1 PARTITION OF audit_event FOR VALUES FROM ('2030-01-01') TO ('2030-04-01');
CREATE TABLE audit_event_2030_q2 PARTITION OF audit_event FOR VALUES FROM ('2030-04-01') TO ('2030-07-01');
CREATE TABLE audit_event_2030_q3 PARTITION OF audit_event FOR VALUES FROM ('2030-07-01') TO ('2030-10-01');
CREATE TABLE audit_event_2030_q4 PARTITION OF audit_event FOR VALUES FROM ('2030-10-01') TO ('2031-01-01');

-- Indexes
CREATE INDEX idx_audit_event_tenant ON audit_event(tenant_id, occurred_at);
CREATE INDEX idx_audit_event_actor ON audit_event(actor_id, occurred_at);
CREATE INDEX idx_audit_event_type ON audit_event(event_type, tenant_id, occurred_at);

-- Enforce immutability: revoke UPDATE and DELETE from the writer role
-- The application connects with audit_writer_role for INSERT-only access
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'audit_writer_role') THEN
        CREATE ROLE audit_writer_role;
    END IF;
END
$$;

GRANT USAGE ON SCHEMA audit_service TO audit_writer_role;
GRANT INSERT, SELECT ON audit_event TO audit_writer_role;
REVOKE UPDATE, DELETE ON audit_event FROM audit_writer_role;
