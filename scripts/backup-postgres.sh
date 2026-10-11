#!/usr/bin/env bash
# SPDX-License-Identifier: AGPL-3.0-only
#
# NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
# Copyright (C) 2026 NAG Contributors
#
# Wrapper script delegating to infrastructure/postgres-backup/backup-postgres.sh
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec "${SCRIPT_DIR}/../infrastructure/postgres-backup/backup-postgres.sh" "$@"
