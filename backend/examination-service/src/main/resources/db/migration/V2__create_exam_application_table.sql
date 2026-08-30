-- V2__create_exam_application_table.sql
-- Flyway migration: exam_application table for candidate exam registrations.

CREATE TABLE IF NOT EXISTS examination_service.exam_application
(
    id                  UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    candidate_id        UUID         NOT NULL,
    examination_id      UUID         NOT NULL REFERENCES examination_service.examination (id) ON DELETE CASCADE,
    tenant_id           VARCHAR(100) NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'APPLIED',
    hall_ticket_number  VARCHAR(30),
    applied_at          TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_candidate_exam_tenant UNIQUE (candidate_id, examination_id, tenant_id),
    CONSTRAINT chk_application_status CHECK (status IN ('APPLIED', 'CONFIRMED', 'REJECTED'))
);

CREATE INDEX idx_exam_application_candidate ON examination_service.exam_application (candidate_id, tenant_id);
CREATE INDEX idx_exam_application_exam ON examination_service.exam_application (examination_id, tenant_id);

COMMENT ON TABLE examination_service.exam_application IS
    'Tracks candidate applications to examinations. Unique per (candidate, exam, tenant).';
