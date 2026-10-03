-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors

-- ============================================================
-- Migration: Add Merkle Tree Root & Public Ledger Anchoring Fields to Paper
-- Validates: Issue #156
-- ============================================================

ALTER TABLE paper_generator.paper
    ADD COLUMN IF NOT EXISTS variant                    VARCHAR(50),
    ADD COLUMN IF NOT EXISTS paper_root_hash            VARCHAR(64),
    ADD COLUMN IF NOT EXISTS manifest_digest            VARCHAR(64),
    ADD COLUMN IF NOT EXISTS ledger_tx_hash             VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ledger_consensus_timestamp VARCHAR(64),
    ADD COLUMN IF NOT EXISTS ledger_block_number        BIGINT,
    ADD COLUMN IF NOT EXISTS ledger_explorer_url        TEXT,
    ADD COLUMN IF NOT EXISTS ledger_network             VARCHAR(100),
    ADD COLUMN IF NOT EXISTS merkle_proof_json          JSONB,
    ADD COLUMN IF NOT EXISTS anchored_at                TIMESTAMP,
    ADD COLUMN IF NOT EXISTS time_lock_release_at       TIMESTAMP,
    ADD COLUMN IF NOT EXISTS is_time_locked             BOOLEAN DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_paper_root_hash   ON paper_generator.paper(paper_root_hash);
CREATE INDEX IF NOT EXISTS idx_paper_ledger_tx   ON paper_generator.paper(ledger_tx_hash);
