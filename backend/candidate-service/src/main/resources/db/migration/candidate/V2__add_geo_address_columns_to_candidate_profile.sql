-- SPDX-License-Identifier: AGPL-3.0-only
--
-- National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
-- Copyright (C) 2025 NAG Contributors

ALTER TABLE candidate_service.candidate_profile
    ADD COLUMN IF NOT EXISTS country  VARCHAR(255),
    ADD COLUMN IF NOT EXISTS state    VARCHAR(255),
    ADD COLUMN IF NOT EXISTS district VARCHAR(255),
    ADD COLUMN IF NOT EXISTS city     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS pin_code VARCHAR(255);
