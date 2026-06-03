CREATE SCHEMA IF NOT EXISTS identity_service;

CREATE TABLE IF NOT EXISTS identity_service.candidate_account (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     VARCHAR(100) NOT NULL,
    doc_type      VARCHAR(20)  NOT NULL,
    doc_hash      VARCHAR(64)  NOT NULL UNIQUE,
    mobile_hash   VARCHAR(64)  NOT NULL UNIQUE,
    email_hash    VARCHAR(64),
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    mfa_enabled   BOOLEAN      NOT NULL DEFAULT FALSE,
    device_fingerprint_hash VARCHAR(64),
    failed_attempt_count    INT          NOT NULL DEFAULT 0,
    locked_until  TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP    NOT NULL DEFAULT NOW(),
    version       BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_candidate_account_tenant ON identity_service.candidate_account(tenant_id);
CREATE INDEX idx_candidate_account_status ON identity_service.candidate_account(status);

CREATE TABLE IF NOT EXISTS identity_service.otp_verification (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id    UUID         NOT NULL REFERENCES identity_service.candidate_account(id),
    otp_hash      VARCHAR(64)  NOT NULL,
    purpose       VARCHAR(30)  NOT NULL,
    expires_at    TIMESTAMP    NOT NULL,
    used          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_account ON identity_service.otp_verification(account_id);

CREATE TABLE IF NOT EXISTS identity_service.role_assignment (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     VARCHAR(100) NOT NULL,
    user_id       UUID         NOT NULL,
    role          VARCHAR(30)  NOT NULL,
    assigned_by   UUID,
    assigned_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    revoked_at    TIMESTAMP,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    version       BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_role_active ON identity_service.role_assignment(user_id, role) WHERE active = TRUE;
