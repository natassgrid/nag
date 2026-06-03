CREATE SCHEMA IF NOT EXISTS admin_service;
SET search_path TO admin_service;

CREATE TABLE system_config (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         VARCHAR(255) NOT NULL,
    param_name        VARCHAR(255) NOT NULL,
    param_value       TEXT NOT NULL,
    updated_by        UUID,
    updated_at_config TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    version           BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_config_param_tenant UNIQUE (param_name, tenant_id)
);

CREATE INDEX idx_system_config_tenant ON system_config(tenant_id);
CREATE INDEX idx_system_config_param ON system_config(param_name, tenant_id);
