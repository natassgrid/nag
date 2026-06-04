CREATE SCHEMA IF NOT EXISTS admin_service;

-- ============================================================
-- Table: system_config
-- ============================================================
CREATE TABLE admin_service.system_config (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       VARCHAR(255) NOT NULL,
    param_name      VARCHAR(255) NOT NULL,
    param_value     TEXT NOT NULL,
    updated_by      UUID,
    updated_at_config TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_system_config_tenant_id ON admin_service.system_config(tenant_id);
CREATE INDEX idx_system_config_param_name ON admin_service.system_config(param_name);
