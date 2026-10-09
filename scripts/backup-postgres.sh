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
# Automated & Economical PostgreSQL Backup Pipeline
# - Ultra-low memory host footprint (< 50MB)
# - Streamed high-ratio compression (zstd -19 or gzip -9)
# - SHA-256 cryptographic checksumming
# - Preserves all platform schemas & pgvector / custom types / indexes
# - Direct S3 upload with cost-optimized storage tiering (--storage-class STANDARD_IA)
# - Automatic local cleanup to protect host disk
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

# ── Color Output Helpers ──────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

log_info()  { echo -e "${BLUE}[INFO]${NC}  $(date -u +'%Y-%m-%dT%H:%M:%SZ') - $*"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}    $(date -u +'%Y-%m-%dT%H:%M:%SZ') - $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $(date -u +'%Y-%m-%dT%H:%M:%SZ') - $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $(date -u +'%Y-%m-%dT%H:%M:%SZ') - $*" >&2; }

# ── Configuration Defaults ────────────────────────────────────────────────────
POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"
POSTGRES_DB="${POSTGRES_DB:-exam_platform}"
POSTGRES_USER="${POSTGRES_USER:-exam_admin}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-exam_secret}"

S3_BUCKET="${S3_BUCKET:-}"
S3_PREFIX="${S3_PREFIX:-backups/postgres}"
S3_STORAGE_CLASS="${S3_STORAGE_CLASS:-STANDARD_IA}"
AWS_ENDPOINT_URL="${AWS_ENDPOINT_URL:-}"
BACKUP_DIR="${BACKUP_DIR:-/tmp/nag-postgres-backups}"
COMPRESSION_ALGO="${COMPRESSION_ALGO:-auto}" # auto | zstd | gzip
KEEP_LOCAL="${KEEP_LOCAL:-false}"
NO_UPLOAD="${NO_UPLOAD:-false}"
WEBHOOK_URL="${WEBHOOK_URL:-}"
SCHEMAS="${SCHEMAS:-}" # Empty means all schemas in cluster

# Standard NAG Platform Schemas
PLATFORM_SCHEMAS=(
    "identity_service"
    "candidate_service"
    "question_service"
    "examination_service"
    "paper_generator"
    "delivery_service"
    "response_service"
    "evaluation_service"
    "result_service"
    "audit_service"
    "notification_service"
    "admin_service"
    "analytics_service"
    "asset_service"
    "practice_service"
    "recommendation_service"
    "keycloak"
    "public"
)

# ── Usage / Help ──────────────────────────────────────────────────────────────
usage() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS]

Economical & Automated PostgreSQL backup pipeline for NAG.

Options:
  -b, --bucket <bucket>          S3 bucket name (env: S3_BUCKET)
  -p, --prefix <prefix>          S3 key prefix (default: backups/postgres, env: S3_PREFIX)
  -s, --storage-class <class>    S3 Storage Class (default: STANDARD_IA, env: S3_STORAGE_CLASS)
  -c, --compression <algo>       Compression: auto, zstd, gzip (default: auto, env: COMPRESSION_ALGO)
  -o, --output-dir <path>        Local directory for temporary dump (default: /tmp/nag-postgres-backups)
      --schemas <list>           Comma-separated list of schemas to dump (default: all schemas)
      --keep-local               Retain local dump archive after S3 upload (default: delete)
      --no-upload                Skip S3 upload; generate dump locally only
      --endpoint-url <url>       Custom S3 endpoint URL (for Lightsail Object Storage / MinIO)
      --webhook-url <url>        Webhook URL for backup failure/success alerts (Slack/Discord/SNS)
  -h, --help                     Display this help message and exit

Database Connection (via environment variables):
  POSTGRES_HOST        Host (default: localhost)
  POSTGRES_PORT        Port (default: 5432)
  POSTGRES_DB          Database name (default: exam_platform)
  POSTGRES_USER        Database user (default: exam_admin)
  POSTGRES_PASSWORD    Database password (default: exam_secret)

Examples:
  # Dump and upload to S3 using auto compression (zstd -19 or gzip -9):
  S3_BUCKET=my-nag-backups ./scripts/backup-postgres.sh

  # Dump locally without S3 upload:
  ./scripts/backup-postgres.sh --no-upload --output-dir ./backups

  # Upload with AWS Lightsail Object Storage endpoint:
  ./scripts/backup-postgres.sh --bucket lightsail-nag-bucket --endpoint-url https://s3.us-east-1.amazonaws.com
EOF
    exit 0
}

# ── Parse Command Line Arguments ──────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        -b|--bucket)
            S3_BUCKET="$2"
            shift 2
            ;;
        -p|--prefix)
            S3_PREFIX="$2"
            shift 2
            ;;
        -s|--storage-class)
            S3_STORAGE_CLASS="$2"
            shift 2
            ;;
        -c|--compression)
            COMPRESSION_ALGO="$2"
            shift 2
            ;;
        -o|--output-dir)
            BACKUP_DIR="$2"
            shift 2
            ;;
        --schemas)
            SCHEMAS="$2"
            shift 2
            ;;
        --keep-local)
            KEEP_LOCAL=true
            shift
            ;;
        --no-upload)
            NO_UPLOAD=true
            shift
            ;;
        --endpoint-url)
            AWS_ENDPOINT_URL="$2"
            shift 2
            ;;
        --webhook-url)
            WEBHOOK_URL="$2"
            shift 2
            ;;
        -h|--help)
            usage
            ;;
        *)
            log_error "Unknown option: $1"
            usage
            ;;
    esac
done

# ── Alerting Notification Handler ─────────────────────────────────────────────
notify_webhook() {
    local status="$1"
    local message="$2"
    if [[ -n "$WEBHOOK_URL" ]]; then
        local payload
        payload=$(printf '{"service":"nag-postgres-backup","status":"%s","message":"%s","timestamp":"%s"}' \
            "$status" "$message" "$(date -u +'%Y-%m-%dT%H:%M:%SZ')")
        curl -s -X POST -H 'Content-Type: application/json' -d "$payload" "$WEBHOOK_URL" > /dev/null 2>&1 || true
    fi
}

on_failure() {
    local exit_code=$?
    log_error "Backup pipeline failed with exit code ${exit_code} at line ${BASH_LINENO[0]}."
    notify_webhook "FAILED" "PostgreSQL backup failed with exit code ${exit_code}"
    exit "$exit_code"
}
trap on_failure ERR

# ── Check Prerequisites ───────────────────────────────────────────────────────
log_info "Starting National Assessment Grid (NAG) PostgreSQL Backup Pipeline..."

command -v pg_dump >/dev/null 2>&1 || {
    log_error "'pg_dump' is required but not installed or not in PATH."
    exit 1
}

# Auto-detect SHA256 tool
if command -v sha256sum >/dev/null 2>&1; then
    SHA256_CMD="sha256sum"
elif command -v shasum >/dev/null 2>&1; then
    SHA256_CMD="shasum -a 256"
else
    log_error "Neither 'sha256sum' nor 'shasum' found for cryptographic integrity verification."
    exit 1
fi

# Detect compressor
if [[ "$COMPRESSION_ALGO" == "auto" ]]; then
    if command -v zstd >/dev/null 2>&1; then
        COMPRESSION_ALGO="zstd"
    elif command -v gzip >/dev/null 2>&1; then
        COMPRESSION_ALGO="gzip"
    else
        log_error "Neither 'zstd' nor 'gzip' found for stream compression."
        exit 1
    fi
fi

case "$COMPRESSION_ALGO" in
    zstd)
        command -v zstd >/dev/null 2>&1 || { log_error "'zstd' selected but not installed."; exit 1; }
        COMPRESS_PIPE="zstd -19 -T0"
        FILE_EXT="sql.zst"
        log_info "Compression configured: zstd -19 (ultra-high ratio)"
        ;;
    gzip)
        command -v gzip >/dev/null 2>&1 || { log_error "'gzip' selected but not installed."; exit 1; }
        COMPRESS_PIPE="gzip -9"
        FILE_EXT="sql.gz"
        log_info "Compression configured: gzip -9 (maximum deflate)"
        ;;
    *)
        log_error "Unsupported compression algorithm: $COMPRESSION_ALGO (choose 'zstd', 'gzip', or 'auto')"
        exit 1
        ;;
esac

if [[ "$NO_UPLOAD" != "true" ]]; then
    if [[ -z "$S3_BUCKET" ]]; then
        log_error "S3_BUCKET is required for upload. Provide --bucket <name> or set S3_BUCKET (or use --no-upload for local only)."
        exit 1
    fi
    command -v aws >/dev/null 2>&1 || {
        log_error "'aws' CLI is required for S3 upload. Install AWS CLI or specify --no-upload."
        exit 1
    }
fi

# ── Prepare Directories and Filenames ─────────────────────────────────────────
mkdir -p "$BACKUP_DIR"
TIMESTAMP="$(date -u +'%Y%m%d_%H%M%SZ')"
BACKUP_BASENAME="nag-db-${POSTGRES_DB}-${TIMESTAMP}"
BACKUP_FILENAME="${BACKUP_BASENAME}.${FILE_EXT}"
CHECKSUM_FILENAME="${BACKUP_FILENAME}.sha256"
LOCAL_BACKUP_PATH="${BACKUP_DIR}/${BACKUP_FILENAME}"
LOCAL_CHECKSUM_PATH="${BACKUP_DIR}/${CHECKSUM_FILENAME}"

START_TIME=$(date +%s)
log_info "Target Database : ${POSTGRES_DB} at ${POSTGRES_HOST}:${POSTGRES_PORT}"
log_info "Output File     : ${LOCAL_BACKUP_PATH}"

# ── Build pg_dump Command Options ─────────────────────────────────────────────
# We export PGPASSWORD to avoid prompt
export PGPASSWORD="$POSTGRES_PASSWORD"

PG_DUMP_ARGS=(
    "-h" "$POSTGRES_HOST"
    "-p" "$POSTGRES_PORT"
    "-U" "$POSTGRES_USER"
    "-d" "$POSTGRES_DB"
    "--clean"
    "--if-exists"
    "--quote-all-identifiers"
    "--no-owner"
    "--no-privileges"
)

if [[ -n "$SCHEMAS" ]]; then
    log_info "Dumping specified schemas: ${SCHEMAS}"
    IFS=',' read -ra SCHEMA_ARRAY <<< "$SCHEMAS"
    for s in "${SCHEMA_ARRAY[@]}"; do
        PG_DUMP_ARGS+=("-n" "$s")
    done
else
    log_info "Dumping entire cluster database (all schemas, extensions & pgvector compatibility preserved)"
fi

# ── Execute Stream Dump & Compression ─────────────────────────────────────────
log_info "Initiating non-blocking stream dump via pg_dump..."

# Stream dump directly into compressor to prevent huge uncompressed intermediate files
# shellcheck disable=SC2086
pg_dump "${PG_DUMP_ARGS[@]}" | $COMPRESS_PIPE > "$LOCAL_BACKUP_PATH"

log_ok "Database dump and compression completed successfully."

# ── Generate SHA-256 Checksum ─────────────────────────────────────────────────
log_info "Computing SHA-256 cryptographic checksum..."
(
    cd "$BACKUP_DIR"
    # Format: <hash>  <filename>
    $SHA256_CMD "$BACKUP_FILENAME" > "$CHECKSUM_FILENAME"
)
CHECKSUM_VALUE=$(awk '{print $1}' "$LOCAL_CHECKSUM_PATH")
FILE_SIZE_BYTES=$(wc -c < "$LOCAL_BACKUP_PATH" | tr -d ' ')
FILE_SIZE_HUMAN=$(du -h "$LOCAL_BACKUP_PATH" | awk '{print $1}')

log_ok "SHA-256 : ${CHECKSUM_VALUE}"
log_ok "Size    : ${FILE_SIZE_HUMAN} (${FILE_SIZE_BYTES} bytes)"

# ── S3 / Lightsail Upload ─────────────────────────────────────────────────────
if [[ "$NO_UPLOAD" != "true" ]]; then
    S3_TARGET_URI="s3://${S3_BUCKET}/${S3_PREFIX}/${BACKUP_FILENAME}"
    S3_CHECKSUM_URI="s3://${S3_BUCKET}/${S3_PREFIX}/${CHECKSUM_FILENAME}"

    AWS_ARGS=()
    if [[ -n "$AWS_ENDPOINT_URL" ]]; then
        AWS_ARGS+=("--endpoint-url" "$AWS_ENDPOINT_URL")
    fi

    log_info "Uploading backup to ${S3_TARGET_URI} (Storage Class: ${S3_STORAGE_CLASS})..."
    aws s3 cp "${AWS_ARGS[@]}" "$LOCAL_BACKUP_PATH" "$S3_TARGET_URI" \
        --storage-class "$S3_STORAGE_CLASS"

    log_info "Uploading checksum manifest to ${S3_CHECKSUM_URI}..."
    aws s3 cp "${AWS_ARGS[@]}" "$LOCAL_CHECKSUM_PATH" "$S3_CHECKSUM_URI" \
        --storage-class "$S3_STORAGE_CLASS"

    log_ok "Backup archive and checksum manifest uploaded successfully."

    # Clean up local temporary file if requested
    if [[ "$KEEP_LOCAL" != "true" ]]; then
        log_info "Cleaning up temporary local files to conserve Lightsail host storage..."
        rm -f "$LOCAL_BACKUP_PATH" "$LOCAL_CHECKSUM_PATH"
        log_ok "Local temporary files removed."
    else
        log_info "Keeping local files at: ${LOCAL_BACKUP_PATH}"
    fi
else
    log_info "S3 upload bypassed (--no-upload). Backup kept locally at: ${LOCAL_BACKUP_PATH}"
fi

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

# ── Summary Report ────────────────────────────────────────────────────────────
echo ""
echo "=========================================================================="
echo "          NAG PostgreSQL Backup Pipeline Summary"
echo "=========================================================================="
echo "  Timestamp       : ${TIMESTAMP}"
echo "  Target Database : ${POSTGRES_DB} (${POSTGRES_HOST}:${POSTGRES_PORT})"
echo "  Backup Archive  : ${BACKUP_FILENAME}"
echo "  Size            : ${FILE_SIZE_HUMAN} (${FILE_SIZE_BYTES} bytes)"
echo "  Compression     : ${COMPRESSION_ALGO}"
echo "  SHA-256 Sum     : ${CHECKSUM_VALUE}"
if [[ "$NO_UPLOAD" != "true" ]]; then
echo "  S3 Location     : s3://${S3_BUCKET}/${S3_PREFIX}/${BACKUP_FILENAME}"
echo "  Storage Class   : ${S3_STORAGE_CLASS}"
else
echo "  Storage         : Local (${LOCAL_BACKUP_PATH})"
fi
echo "  Elapsed Time    : ${DURATION}s"
echo "  Status          : SUCCESS"
echo "=========================================================================="

notify_webhook "SUCCESS" "PostgreSQL backup ${BACKUP_FILENAME} completed in ${DURATION}s (${FILE_SIZE_HUMAN})"

exit 0
