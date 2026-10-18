-- ============================================================
-- Flyway migration V4: Publish Approved Seed Questions
-- Ensures seed questions are in PUBLISHED state for exam paper assembly
-- ============================================================

UPDATE question_service.question
SET state = 'PUBLISHED'
WHERE state = 'APPROVED';
