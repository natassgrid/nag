-- V2: Create active_session table and user_account table (replaces/supplements V1 candidate_account)

-- The user_account table used by JPA entities (the V1 candidate_account is the legacy name)
CREATE TABLE IF NOT EXISTS identity_service.user_account (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id             VARCHAR(255) NOT NULL,
    username              VARCHAR(255) NOT NULL,
    email_hash            VARCHAR(64)  NOT NULL,
    mobile_hash           VARCHAR(64)  NOT NULL,
    identity_doc_type     VARCHAR(30),
    identity_doc_hash     VARCHAR(64),
    identity_doc_hmac     VARCHAR(128),
    account_status        VARCHAR(30)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    mfa_enabled           BOOLEAN      NOT NULL DEFAULT FALSE,
    mfa_secret_ref        VARCHAR(255),
    device_fingerprint    VARCHAR(255),
    failed_attempt_count  INT          NOT NULL DEFAULT 0,
    last_failed_at        TIMESTAMP,
    locked_at             TIMESTAMP,
    keycloak_user_id      VARCHAR(255),
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP    NOT NULL DEFAULT NOW(),
    version               BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_user_account_email_hash_tenant
    ON identity_service.user_account(email_hash, tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_account_mobile_hash_tenant
    ON identity_service.user_account(mobile_hash, tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_account_identity_doc_hash_tenant
    ON identity_service.user_account(identity_doc_hash, tenant_id);

-- Active session table for single-concurrent-session enforcement
CREATE TABLE IF NOT EXISTS identity_service.active_session (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(255) NOT NULL,
    user_id         UUID         NOT NULL,
    session_token   VARCHAR(512) NOT NULL,
    device_fp       VARCHAR(512),
    ip_address      VARCHAR(45),
    expires_at      TIMESTAMP    NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_active_session_user_tenant
    ON identity_service.active_session(user_id, tenant_id);
