-- Translation service schema
CREATE SCHEMA IF NOT EXISTS translation_service;

CREATE TABLE translation_service.translation (
    id              UUID PRIMARY KEY,
    question_id     UUID NOT NULL,
    language_code   VARCHAR(10) NOT NULL,
    translated_content TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    translator_id   UUID NOT NULL,
    reviewer_id     UUID,
    review_comments TEXT,
    tenant_id       VARCHAR(255) NOT NULL,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_translation_status CHECK (status IN ('DRAFT', 'APPROVED', 'STALE')),
    CONSTRAINT chk_language_code CHECK (language_code IN (
        'hi', 'bn', 'te', 'mr', 'ta', 'ur', 'gu', 'kn', 'ml', 'or',
        'pa', 'as', 'mai', 'sa', 'sd', 'ne', 'kok', 'doi', 'mni', 'sat', 'bo', 'kas'
    ))
);

-- Indexes for common queries
CREATE INDEX idx_translation_question_id ON translation_service.translation(question_id);
CREATE INDEX idx_translation_language ON translation_service.translation(language_code);
CREATE INDEX idx_translation_status ON translation_service.translation(status);
CREATE INDEX idx_translation_translator ON translation_service.translation(translator_id);
CREATE INDEX idx_translation_tenant ON translation_service.translation(tenant_id);
CREATE UNIQUE INDEX idx_translation_question_lang_tenant
    ON translation_service.translation(question_id, language_code, tenant_id);
