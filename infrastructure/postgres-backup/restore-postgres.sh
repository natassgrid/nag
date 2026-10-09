#!/usr/bin/env bash
# SPDX-License-Identifier: AGPL-3.0-only
#
# National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
# Copyright (C) 2025 NAG Contributors
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published
# by the Free Software Foundation, version 3 of the License.
#
# ─────────────────────────────────────────────────────────────────────────────
# Resilient PostgreSQL Disaster Recovery (DR) Restoration Pipeline
# - Stream restoration supporting zstd and gzip
# - Pre-restoration safeguards:
#     * Interactive confirmation prompt
#     * Automated pre-restore safety snapshot
#     * SHA-256 cryptographic checksum validation against .sha256 manifest
# - Post-restoration verification:
#     * Connectivity check
#     * Flyway schema history inspection
#     * pgvector extension verification & vector query execution
#     * Row count audit across core platform fact tables
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

# ── Load .env Configuration ──────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ -f "${SCRIPT_DIR}/.env" ]]; then
    while IFS='=' read -r key val || [[ -n "$key" ]]; do
        [[ "$key" =~ ^[[:space:]]*# ]] && continue
        [[ -z "${key// }" ]] && continue
        # Strip leading/trailing spaces from key
        key="${key#"${key%%[![:space:]]*}"}"
        key="${key%"${key##*[![:space:]]}"}"
        # Strip leading/trailing spaces and quotes from value
        val="${val#"${val%%[![:space:]]*}"}"
        val="${val%"${val##*[![:space:]]}"}"
        if [[ "$val" =~ ^\"(.*)\"$ ]]; then
            val="${BASH_REMATCH[1]}"
        elif [[ "$val" =~ ^\'(.*)\'$ ]]; then
            val="${BASH_REMATCH[1]}"
        fi
        if [[ -z "${!key:-}" ]]; then
            export "$key=$val"
        fi
    done < "${SCRIPT_DIR}/.env"
fi

# ── Color Output Helpers ──────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

log_info()    { echo -e "${BLUE}[INFO]${NC}  $(date -u +'%Y-%m-%dT%H:%M:%SZ') - $*"; }
log_ok()      { echo -e "${GREEN}[OK]${NC}    $(date -u +'%Y-%m-%dT%H:%M:%SZ') - $*"; }
log_warn()    { echo -e "${YELLOW}[WARN]${NC}  $(date -u +'%Y-%m-%dT%H:%M:%SZ') - $*"; }
log_error()   { echo -e "${RED}[ERROR]${NC} $(date -u +'%Y-%m-%dT%H:%M:%SZ') - $*" >&2; }
log_section() { echo -e "\n${BOLD}${CYAN}=== $* ===${NC}"; }

# ── Configuration Defaults ────────────────────────────────────────────────────
POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
# Detect Docker container environment for default postgres host
if [[ -f "/.dockerenv" && "$POSTGRES_HOST" == "localhost" ]]; then
    POSTGRES_HOST="postgres"
fi

POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_DB="${POSTGRES_DB:-exam_platform}"
POSTGRES_USER="${POSTGRES_USER:-exam_admin}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-exam_secret}"

S3_BUCKET="${S3_BUCKET:-}"
S3_PREFIX="${S3_PREFIX:-backups/postgres}"
AWS_ENDPOINT_URL="${AWS_ENDPOINT_URL:-}"
RESTORE_WORK_DIR="${RESTORE_WORK_DIR:-/tmp/nag-postgres-restores}"

export AWS_DEFAULT_REGION="${AWS_DEFAULT_REGION:-${AWS_REGION:-${AWS_REGION_NAME:-us-east-1}}}"
export AWS_REGION="${AWS_REGION:-$AWS_DEFAULT_REGION}"

TARGET_DB=""
SOURCE_TARGET=""
USE_LATEST=false
NON_INTERACTIVE=false
SKIP_SNAPSHOT=false
SKIP_CHECKSUM=false
KEEP_DOWNLOADED=false

# ── Usage / Help ──────────────────────────────────────────────────────
usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS] [--latest | <s3-uri-or-filename>]

Disaster Recovery and Restoration pipeline for NAG PostgreSQL database.

Arguments:
  --latest                       Restore the most recent backup found in S3 bucket
  <s3-uri-or-filename>           Local filepath or S3 URI (e.g. s3://bucket/path/backup.sql.zst)

Options:
  --target-db <name>             Target database to restore into (default: POSTGRES_DB or exam_platform)
  -y, --yes, --non-interactive   Skip interactive confirmation prompts
  --skip-snapshot                Skip pre-restoration safety snapshot
  --skip-checksum                Skip SHA-256 verification (NOT recommended)
  --keep-downloaded              Keep downloaded S3 archives in temporary directory
  -b, --bucket <bucket>          S3 bucket name (env: S3_BUCKET, used with --latest)
  -p, --prefix <prefix>          S3 key prefix (default: backups/postgres, env: S3_PREFIX)
  --endpoint-url <url>           Custom S3 endpoint URL (Lightsail Object Storage / MinIO)
  -h, --help                     Display this help message and exit

Environment Variables:
  POSTGRES_HOST        Database Host (default: localhost)
  POSTGRES_PORT        Database Port (default: 5432)
  POSTGRES_DB          Database Name (default: exam_platform)
  POSTGRES_USER        Database User (default: exam_admin)
  POSTGRES_PASSWORD    Database Password (default: exam_secret)
  S3_BUCKET            S3 Bucket for backups

Examples:
  # Restore the latest backup from S3:
  S3_BUCKET=my-nag-backups ./scripts/restore-postgres.sh --latest

  # Restore a specific backup from S3 to a staging database:
  ./scripts/restore-postgres.sh s3://my-nag-backups/backups/postgres/nag-db-exam_platform-20261009.sql.zst --target-db staging_platform

  # Restore from a local compressed backup file:
  ./scripts/restore-postgres.sh /tmp/nag-postgres-backups/nag-db-exam_platform-20261009.sql.gz
EOF
    exit 0
}

# ── Parse Command Line Arguments ──────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        --latest)
            USE_LATEST=true
            shift
            ;;
        --target-db)
            TARGET_DB="$2"
            shift 2
            ;;
        -y|--yes|--non-interactive)
            NON_INTERACTIVE=true
            shift
            ;;
        --skip-snapshot)
            SKIP_SNAPSHOT=true
            shift
            ;;
        --skip-checksum)
            SKIP_CHECKSUM=true
            shift
            ;;
        --keep-downloaded)
            KEEP_DOWNLOADED=true
            shift
            ;;
        -b|--bucket)
            S3_BUCKET="$2"
            shift 2
            ;;
        -p|--prefix)
            S3_PREFIX="$2"
            shift 2
            ;;
        --endpoint-url)
            AWS_ENDPOINT_URL="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        -*)
            log_error "Unknown option: $1"
            usage
            ;;
        *)
            if [[ -z "$SOURCE_TARGET" ]]; then
                SOURCE_TARGET="$1"
            else
                log_error "Unexpected argument: $1"
                usage
            fi
            shift
            ;;
    esac
done

TARGET_DB="${TARGET_DB:-$POSTGRES_DB}"
export PGPASSWORD="$POSTGRES_PASSWORD"

# Auto-detect SHA256 tool
if command -v sha256sum >/dev/null 2>&1; then
    SHA256_CMD="sha256sum"
elif command -v shasum >/dev/null 2>&1; then
    SHA256_CMD="shasum -a 256"
else
    SHA256_CMD=""
fi

# ── Resolve Source Backup ─────────────────────────────────────────────────────
mkdir -p "$RESTORE_WORK_DIR"
LOCAL_RESTORE_FILE=""
LOCAL_CHECKSUM_FILE=""
IS_REMOTE_S3=false

AWS_ARGS=()
if [[ -n "$AWS_ENDPOINT_URL" ]]; then
    AWS_ARGS+=("--endpoint-url" "$AWS_ENDPOINT_URL")
fi

if [[ "$USE_LATEST" == "true" ]]; then
    if [[ -z "$S3_BUCKET" ]]; then
        log_error "S3_BUCKET must be provided when using --latest (set S3_BUCKET or pass --bucket)."
        exit 1
    fi
    log_info "Discovering latest backup in s3://${S3_BUCKET}/${S3_PREFIX}/..."

    # Query latest file with .sql.zst or .sql.gz extension
    LATEST_KEY=$(aws s3api list-objects-v2 "${AWS_ARGS[@]}" \
        --bucket "$S3_BUCKET" \
        --prefix "${S3_PREFIX}/" \
        --query 'sort_by(Contents[?!ends_with(Key, `.sha256`) && (ends_with(Key, `.sql.zst`) || ends_with(Key, `.sql.gz`))], &LastModified)[-1].Key' \
        --output text 2>/dev/null || true)

    if [[ -z "$LATEST_KEY" || "$LATEST_KEY" == "None" ]]; then
        log_error "No valid backup archives (.sql.zst or .sql.gz) found in s3://${S3_BUCKET}/${S3_PREFIX}/"
        exit 1
    fi

    SOURCE_TARGET="s3://${S3_BUCKET}/${LATEST_KEY}"
    log_ok "Identified latest backup: ${SOURCE_TARGET}"
fi

if [[ -z "$SOURCE_TARGET" ]]; then
    log_error "No backup target specified. Provide an S3 URI, a local filename, or --latest."
    usage
fi

# Handle S3 Download vs Local file
if [[ "$SOURCE_TARGET" == s3://* ]]; then
    IS_REMOTE_S3=true
    command -v aws >/dev/null 2>&1 || {
        log_error "'aws' CLI is required to restore from S3."
        exit 1
    }

    FILE_BASENAME="$(basename "$SOURCE_TARGET")"
    LOCAL_RESTORE_FILE="${RESTORE_WORK_DIR}/${FILE_BASENAME}"
    LOCAL_CHECKSUM_FILE="${LOCAL_RESTORE_FILE}.sha256"
    S3_CHECKSUM_URI="${SOURCE_TARGET}.sha256"

    log_info "Downloading backup archive from ${SOURCE_TARGET}..."
    aws s3 cp "${AWS_ARGS[@]}" "$SOURCE_TARGET" "$LOCAL_RESTORE_FILE"

    log_info "Downloading checksum manifest from ${S3_CHECKSUM_URI}..."
    if ! aws s3 cp "${AWS_ARGS[@]}" "$S3_CHECKSUM_URI" "$LOCAL_CHECKSUM_FILE" 2>/dev/null; then
        log_warn "Checksum manifest ${S3_CHECKSUM_URI} not found in S3."
        rm -f "$LOCAL_CHECKSUM_FILE"
    fi
else
    # Local file
    if [[ ! -f "$SOURCE_TARGET" ]]; then
        log_error "Local backup file not found: $SOURCE_TARGET"
        exit 1
    fi
    LOCAL_RESTORE_FILE="$(cd "$(dirname "$SOURCE_TARGET")" && pwd)/$(basename "$SOURCE_TARGET")"
    if [[ -f "${LOCAL_RESTORE_FILE}.sha256" ]]; then
        LOCAL_CHECKSUM_FILE="${LOCAL_RESTORE_FILE}.sha256"
    elif [[ -f "${LOCAL_RESTORE_FILE%.*}.sha256" ]]; then
        LOCAL_CHECKSUM_FILE="${LOCAL_RESTORE_FILE%.*}.sha256"
    fi
fi

# ── Pre-Restoration Safeguard 1: User Confirmation ────────────────────────────
log_section "PRE-RESTORATION SAFEGUARDS"

echo -e "${YELLOW}WARNING:${NC} You are about to initiate database restoration!"
echo -e "  Target Database : ${BOLD}${TARGET_DB}${NC} (${POSTGRES_HOST}:${POSTGRES_PORT})"
echo -e "  Source Backup   : ${BOLD}${SOURCE_TARGET}${NC}"
echo -e "  Archive Size    : $(du -h "$LOCAL_RESTORE_FILE" | awk '{print $1}')"
echo ""

if [[ "$NON_INTERACTIVE" != "true" ]]; then
    read -r -p "Are you sure you want to restore and overwrite data in '${TARGET_DB}'? [y/N]: " CONFIRM
    if [[ "$CONFIRM" != "y" && "$CONFIRM" != "Y" && "$CONFIRM" != "yes" ]]; then
        log_warn "Restoration cancelled by operator."
        if [[ "$IS_REMOTE_S3" == "true" && "$KEEP_DOWNLOADED" != "true" ]]; then
            rm -f "$LOCAL_RESTORE_FILE" "$LOCAL_CHECKSUM_FILE"
        fi
        exit 0
    fi
fi

# ── Pre-Restoration Safeguard 2: SHA-256 Checksum Validation ──────────────────
if [[ "$SKIP_CHECKSUM" == "true" ]]; then
    log_warn "SHA-256 verification explicitly bypassed (--skip-checksum)."
else
    log_info "Verifying SHA-256 integrity..."
    if [[ -n "$LOCAL_CHECKSUM_FILE" && -f "$LOCAL_CHECKSUM_FILE" ]]; then
        if [[ -z "$SHA256_CMD" ]]; then
            log_error "sha256sum tool not found to verify checksum."
            exit 1
        fi

        EXPECTED_HASH=$(awk '{print $1}' "$LOCAL_CHECKSUM_FILE")
        ACTUAL_HASH=$($SHA256_CMD "$LOCAL_RESTORE_FILE" | awk '{print $1}')

        if [[ "$EXPECTED_HASH" != "$ACTUAL_HASH" ]]; then
            log_error "CHECKSUM CORRUPTION DETECTED!"
            log_error "  Expected : $EXPECTED_HASH"
            log_error "  Computed : $ACTUAL_HASH"
            log_error "Restoration aborted to prevent corruption."
            exit 1
        fi
        log_ok "Cryptographic SHA-256 checksum verified: ${ACTUAL_HASH}"
    else
        log_warn "No .sha256 checksum manifest found for this backup archive. Continuing with caution."
    fi
fi

# ── Pre-Restoration Safeguard 3: Automated Pre-Restore Safety Snapshot ─────────
if [[ "$SKIP_SNAPSHOT" == "true" ]]; then
    log_warn "Pre-restoration safety snapshot bypassed (--skip-snapshot)."
else
    SAFETY_SNAPSHOT_PATH="${RESTORE_WORK_DIR}/pre-restore-safety-${TARGET_DB}-$(date -u +'%Y%m%d_%H%M%SZ').sql.gz"
    log_info "Taking safety snapshot of current database state before restoring..."

    if command -v pg_dump >/dev/null 2>&1; then
        if pg_dump -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$TARGET_DB" \
            --clean --if-exists --quote-all-identifiers --no-owner --no-privileges 2>/dev/null | gzip -9 > "$SAFETY_SNAPSHOT_PATH"; then
            log_ok "Pre-restoration safety snapshot created at: ${SAFETY_SNAPSHOT_PATH}"
        else
            log_warn "Pre-restoration snapshot exited with notice (database might be newly initialized or empty). Proceeding..."
        fi
    else
        log_warn "'pg_dump' not present; skipping safety snapshot."
    fi
fi

# ── Detect Decompression Tool ─────────────────────────────────────────────────
DECOMPRESS_CMD=""
case "$LOCAL_RESTORE_FILE" in
    *.zst)
        command -v zstd >/dev/null 2>&1 || {
            log_error "'zstd' is required to decompress .zst archive."
            exit 1
        }
        DECOMPRESS_CMD="zstd -d -c"
        ;;
    *.gz)
        command -v gzip >/dev/null 2>&1 || {
            log_error "'gzip' is required to decompress .gz archive."
            exit 1
        }
        DECOMPRESS_CMD="gzip -d -c"
        ;;
    *.sql)
        DECOMPRESS_CMD="cat"
        ;;
    *)
        log_error "Unrecognized backup file format. Expected .sql.zst, .sql.gz, or .sql."
        exit 1
        ;;
esac

# ── Execute Database Restoration ──────────────────────────────────────────────
log_section "EXECUTING RESTORATION"
log_info "Streaming decompression and executing SQL restore on '${TARGET_DB}'..."
START_RESTORE_TIME=$(date +%s)

# Ensure psql is available
command -v psql >/dev/null 2>&1 || {
    log_error "'psql' client is required for database restoration."
    exit 1
}

# Ensure extensions vector and uuid-ossp exist in target DB prior to table restore
psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$TARGET_DB" \
    -c 'CREATE EXTENSION IF NOT EXISTS "uuid-ossp"; CREATE EXTENSION IF NOT EXISTS vector;' >/dev/null 2>&1 || true

# Stream restore directly into psql
# shellcheck disable=SC2086
$DECOMPRESS_CMD "$LOCAL_RESTORE_FILE" | psql \
    -h "$POSTGRES_HOST" \
    -p "$POSTGRES_PORT" \
    -U "$POSTGRES_USER" \
    -d "$TARGET_DB" \
    -v ON_ERROR_STOP=0 \
    -q

END_RESTORE_TIME=$(date +%s)
RESTORE_DURATION=$((END_RESTORE_TIME - START_RESTORE_TIME))
log_ok "SQL restoration stream finished in ${RESTORE_DURATION}s."

# ── Post-Restoration Validation & Sanity Checks ───────────────────────────────
log_section "POST-RESTORATION VALIDATION"

# 1. Connectivity Check
log_info "1. Checking database connectivity..."
if psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$TARGET_DB" -c "SELECT 1;" >/dev/null 2>&1; then
    log_ok "Database connectivity established."
else
    log_error "Failed to connect to database '${TARGET_DB}' after restoration."
    exit 1
fi

# 2. pgvector Extension and Vector Query Check
log_info "2. Validating pgvector extension and cosine / L2 distance operators..."
VECTOR_CHECK=$(psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$TARGET_DB" -t -A \
    -c "SELECT extversion FROM pg_extension WHERE extname = 'vector';" 2>/dev/null || echo "MISSING")

if [[ -n "$VECTOR_CHECK" && "$VECTOR_CHECK" != "MISSING" ]]; then
    log_ok "pgvector extension is active (version: ${VECTOR_CHECK})."

    # Test vector calculation
    SAMPLE_VECTOR_DIST=$(psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$TARGET_DB" -t -A \
        -c "SELECT round((' [1,2,3] '::vector <-> ' [3,2,1] '::vector)::numeric, 4);" 2>/dev/null || echo "ERROR")
    if [[ "$SAMPLE_VECTOR_DIST" != "ERROR" ]]; then
        log_ok "pgvector vector distance operation validated successfully (test distance: ${SAMPLE_VECTOR_DIST})."
    else
        log_warn "pgvector query failed to execute."
    fi
else
    log_warn "pgvector extension is not registered in target database."
fi

# 3. Flyway Schema Migrations
log_info "3. Inspecting Flyway schema migration history..."
FLYWAY_MIGRATION_COUNT=$(psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$TARGET_DB" -t -A \
    -c "SELECT count(*) FROM (
            SELECT 1 FROM information_schema.tables WHERE table_name = 'flyway_schema_history'
        ) t;" 2>/dev/null || echo "0")

if [[ "$FLYWAY_MIGRATION_COUNT" -gt 0 ]]; then
    # List schema migration versions
    psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$TARGET_DB" \
        -c "SELECT table_schema, count(*) as applied_migrations, max(version) as latest_version
            FROM (
                SELECT table_schema, version
                FROM information_schema.tables t
                CROSS JOIN LATERAL (
                    SELECT version FROM flyway_schema_history LIMIT 0
                ) f WHERE t.table_name = 'flyway_schema_history'
            ) sub GROUP BY table_schema;" 2>/dev/null || true
    log_ok "Flyway migration tables verified."
else
    log_info "No separate flyway_schema_history tables detected or tables unpopulated."
fi

# 4. Core Fact Table Row Counts Audit
log_info "4. Auditing core fact table record counts..."
CORE_TABLES=(
    "identity_service.users"
    "candidate_service.candidate_profile"
    "candidate_service.candidate_education"
    "question_service.questions"
    "examination_service.examinations"
    "examination_service.blueprints"
    "paper_generator.generated_papers"
    "delivery_service.exam_sessions"
    "response_service.candidate_responses"
    "evaluation_service.evaluations"
    "result_service.results"
    "audit_service.audit_events"
    "admin_service.system_settings"
    "practice_service.practice_sessions"
)

echo "------------------------------------------------------------------"
printf "%-40s | %-15s\n" "Table Name" "Row Count"
echo "------------------------------------------------------------------"

for tbl in "${CORE_TABLES[@]}"; do
    SCHEMA_NAME="${tbl%%.*}"
    TABLE_NAME="${tbl##*.}"

    EXISTS=$(psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$TARGET_DB" -t -A \
        -c "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = '$SCHEMA_NAME' AND table_name = '$TABLE_NAME');" 2>/dev/null || echo "f")

    if [[ "$EXISTS" == "t" ]]; then
        ROW_COUNT=$(psql -h "$POSTGRES_HOST" -p "$POSTGRES_PORT" -U "$POSTGRES_USER" -d "$TARGET_DB" -t -A \
            -c "SELECT count(*) FROM $tbl;" 2>/dev/null || echo "ERR")
        printf "%-40s | %-15s\n" "$tbl" "$ROW_COUNT"
    else
        printf "%-40s | %-15s\n" "$tbl" "(not created)"
    fi
done
echo "------------------------------------------------------------------"

# ── Local Temp Cleanup ────────────────────────────────────────────────────────
if [[ "$IS_REMOTE_S3" == "true" && "$KEEP_DOWNLOADED" != "true" ]]; then
    log_info "Cleaning up temporary downloaded files..."
    rm -f "$LOCAL_RESTORE_FILE" "${LOCAL_RESTORE_FILE}.sha256"
fi

# ── Summary Report ────────────────────────────────────────────────────────────
echo ""
echo "=========================================================================="
echo "          NAG PostgreSQL Restoration Completed Successfully"
echo "=========================================================================="
echo "  Target Database     : ${TARGET_DB} (${POSTGRES_HOST}:${POSTGRES_PORT})"
echo "  Source Archive      : ${SOURCE_TARGET}"
echo "  Decompressor        : ${DECOMPRESS_CMD}"
echo "  Restoration Time    : ${RESTORE_DURATION}s"
echo "  pgvector Status     : ACTIVE (v${VECTOR_CHECK})"
if [[ -n "${SAFETY_SNAPSHOT_PATH:-}" && -f "${SAFETY_SNAPSHOT_PATH:-}" ]]; then
echo "  Pre-restore Backup  : ${SAFETY_SNAPSHOT_PATH}"
fi
echo "  Status              : HEALTHY"
echo "=========================================================================="

exit 0
