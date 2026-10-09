# PostgreSQL Automated Backup & Disaster Recovery (DR) Pipeline

This directory consolidates all scripts, configurations, policies, and orchestration units for the **National Assessment Grid (NAG)** PostgreSQL automated backup and disaster recovery pipeline.

## 📁 Directory Contents

| File | Purpose |
| :--- | :--- |
| [`backup-postgres.sh`](file:///F:/code/IdeaProjects/nag/infrastructure/postgres-backup/backup-postgres.sh) | Automated stream backup with `pg_dump`, `zstd -19`/`gzip -9`, SHA-256 checksum, and S3 `STANDARD_IA` upload |
| [`restore-postgres.sh`](file:///F:/code/IdeaProjects/nag/infrastructure/postgres-backup/restore-postgres.sh) | DR restoration utility supporting `--latest`, S3/local files, safety snapshots, SHA-256 validation, and sanity audits |
| [`docker-compose.backup.yml`](file:///F:/code/IdeaProjects/nag/infrastructure/postgres-backup/docker-compose.backup.yml) | Lightweight Alpine Docker sidecar runner (< 50MB RAM) for daily 02:00 UTC automated cron execution |
| [`s3-lifecycle-policy.json`](file:///F:/code/IdeaProjects/nag/infrastructure/postgres-backup/s3-lifecycle-policy.json) | AWS S3 Lifecycle tiering policy: 7 days -> Glacier Deep Archive, 60 days -> Expiry, 1 day multipart abort |
| [`iam-backup-policy.json`](file:///F:/code/IdeaProjects/nag/infrastructure/postgres-backup/iam-backup-policy.json) | Least-privilege IAM policy granting minimal permissions (`PutObject`, `GetObject`, `ListBucket`) |
| [`nag-postgres-backup.service`](file:///F:/code/IdeaProjects/nag/infrastructure/postgres-backup/nag-postgres-backup.service) | Systemd oneshot service unit definition with sandboxing and memory limits |
| [`nag-postgres-backup.timer`](file:///F:/code/IdeaProjects/nag/infrastructure/postgres-backup/nag-postgres-backup.timer) | Systemd timer for scheduling automated backups daily at 02:00 UTC |

## 🚀 Quick Usage

### 1. Trigger Manual Backup
```bash
S3_BUCKET="my-nag-backups" ./infrastructure/postgres-backup/backup-postgres.sh
```

### 2. Trigger Disaster Recovery Restoration
```bash
# Restore latest backup from S3
S3_BUCKET="my-nag-backups" ./infrastructure/postgres-backup/restore-postgres.sh --latest

# Restore from local archive
./infrastructure/postgres-backup/restore-postgres.sh /tmp/nag-postgres-backups/nag-db-exam_platform-20261009.sql.zst
```

### 3. Launch Docker Backup Sidecar
```bash
docker compose -f infrastructure/docker-compose/docker-compose.yml \
               -f infrastructure/postgres-backup/docker-compose.backup.yml up -d postgres-backup
```

For complete architectural documentation, cost calculations (< $1.00/mo), and step-by-step DR runbooks, refer to [`docs/operations/backup-and-restore.md`](file:///F:/code/IdeaProjects/nag/docs/operations/backup-and-restore.md).
