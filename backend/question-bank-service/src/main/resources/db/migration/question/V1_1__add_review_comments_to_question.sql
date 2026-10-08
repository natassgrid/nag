-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors
--
-- Migration: V1_1__add_review_comments_to_question.sql
--
-- Gap fix for Issue #275: Persist reviewer rejection comments on the Question entity
-- so that authors can see the feedback and revise their questions accordingly.
--
-- Validates: Requirements 5.3 (reviewer rejection feedback visibility)

ALTER TABLE question_service.question
    ADD COLUMN IF NOT EXISTS review_comments TEXT;

COMMENT ON COLUMN question_service.question.review_comments
    IS 'Reviewer feedback comments set when a question is rejected (REVIEW -> DRAFT). '
       'Visible to the author for revision guidance.';
