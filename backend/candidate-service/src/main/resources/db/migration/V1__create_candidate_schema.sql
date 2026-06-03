CREATE SCHEMA IF NOT EXISTS candidate_service;
SET search_path TO candidate_service;

CREATE TABLE candidate_profile (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id                VARCHAR(255) NOT NULL,
    user_id                  UUID NOT NULL,
    full_name                TEXT,           -- encrypted
    date_of_birth            TEXT,           -- encrypted
    gender                   TEXT,           -- encrypted
    nationality              TEXT,           -- encrypted
    category                 TEXT,           -- encrypted
    mobile                   TEXT,           -- encrypted
    email                    TEXT,           -- encrypted
    address                  TEXT,           -- encrypted
    reservation_category     TEXT,           -- encrypted
    identity_doc_number      TEXT,           -- encrypted
    mobile_hash              VARCHAR(64) NOT NULL,
    identity_doc_hash        VARCHAR(64) NOT NULL,
    identity_doc_hmac        VARCHAR(64) NOT NULL,
    encryption_key_id        VARCHAR(255),
    digi_locker_verified     VARCHAR(20) DEFAULT 'PENDING',
    face_verification_status VARCHAR(20) DEFAULT 'PENDING',
    consent_recorded         BOOLEAN DEFAULT FALSE,
    consent_timestamp        TIMESTAMPTZ,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    version                  BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_tenant UNIQUE (user_id, tenant_id),
    CONSTRAINT uq_mobile_hash_tenant UNIQUE (mobile_hash, tenant_id),
    CONSTRAINT uq_doc_hash_tenant UNIQUE (identity_doc_hash, tenant_id)
);

CREATE INDEX idx_candidate_tenant ON candidate_profile(tenant_id);
CREATE INDEX idx_candidate_user ON candidate_profile(user_id);
