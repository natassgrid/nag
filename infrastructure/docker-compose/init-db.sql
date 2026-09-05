-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors

-- =============================================================================
-- Database initialisation — create per-service schemas
-- Open Digital Public Infrastructure (DPI) Platform
-- Design doc: "All services use separate schemas within a single PostgreSQL cluster"
-- =============================================================================

-- Create service schemas
CREATE SCHEMA IF NOT EXISTS identity_service;
CREATE SCHEMA IF NOT EXISTS candidate_service;
CREATE SCHEMA IF NOT EXISTS question_service;
CREATE SCHEMA IF NOT EXISTS examination_service;
CREATE SCHEMA IF NOT EXISTS paper_generator;
CREATE SCHEMA IF NOT EXISTS delivery_service;
CREATE SCHEMA IF NOT EXISTS response_service;
CREATE SCHEMA IF NOT EXISTS evaluation_service;
CREATE SCHEMA IF NOT EXISTS result_service;
CREATE SCHEMA IF NOT EXISTS audit_service;
CREATE SCHEMA IF NOT EXISTS notification_service;
CREATE SCHEMA IF NOT EXISTS admin_service;
CREATE SCHEMA IF NOT EXISTS analytics_service;
CREATE SCHEMA IF NOT EXISTS asset_service;

-- Schema for Keycloak (used when KC_DB_SCHEMA=keycloak is set)
CREATE SCHEMA IF NOT EXISTS keycloak;

-- Enable uuid-ossp for UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Enable pgvector extension for halfvec similarity search (question-bank-service)
CREATE EXTENSION IF NOT EXISTS vector;

-- Create service-specific roles with least-privilege access
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'identity_writer') THEN
        CREATE ROLE identity_writer LOGIN PASSWORD 'identity_pw';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'candidate_writer') THEN
        CREATE ROLE candidate_writer LOGIN PASSWORD 'candidate_pw';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'question_writer') THEN
        CREATE ROLE question_writer LOGIN PASSWORD 'question_pw';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'audit_writer_role') THEN
        CREATE ROLE audit_writer_role NOLOGIN;
    END IF;
END
$$;

-- Grant schema usage
GRANT USAGE ON SCHEMA identity_service    TO identity_writer;
GRANT USAGE ON SCHEMA candidate_service   TO candidate_writer;
GRANT USAGE ON SCHEMA question_service    TO question_writer;
GRANT USAGE ON SCHEMA audit_service       TO audit_writer_role;

-- audit_writer_role: INSERT-only (no UPDATE/DELETE — Req 15.6)
-- Applied per-table after schema init by the audit-service migration scripts.
