# Scorecard PDF Storage Architecture & Migration Guide

## Overview

Issue [#118](https://github.com/natassgrid/nag/issues/118) migrates scorecard PDF storage in `result-service` from local filesystem-only storage to cloud object storage (AWS S3 / MinIO / LocalStack) while providing a pluggable storage interface and preserving a local filesystem fallback for development, testing, and single-container Docker environments.

---

## Key Features

1. **Pluggable Storage Provider (`ScorecardStorageProvider`)**:
   - `S3ScorecardStorageProvider`: AWS SDK v2 based object storage provider supporting AWS S3, MinIO, and LocalStack.
   - `LocalFileScorecardStorageProvider`: Local filesystem provider with path-traversal protection.
2. **Presigned URL Generation**:
   - Secure, time-limited URLs (default 15 minutes / 900 seconds) for candidates and admins to download scorecards directly from S3 without proxying large binary payloads through the application server.
3. **Dual Access Mode Endpoints**:
   - `GET /api/v1/results/{id}/scorecard` (accepts JSON / default): Returns `ScorecardUrlResponse` containing the presigned download URL, expiration, and storage metadata.
   - `GET /api/v1/results/{id}/scorecard` (with `Accept: application/pdf` or `?download=true`): Directly streams the binary PDF.
   - `GET /api/v1/results/{id}/scorecard/presigned`: Explicit endpoint returning `ScorecardUrlResponse`.
   - `GET /api/v1/results/{id}/scorecard/download`: Direct binary stream download endpoint.
4. **End-to-End PDF Security**:
   - Scorecards are encrypted with 128-bit AES password protection (`dateOfBirth + candidateId`) before being uploaded to object storage.
   - Embedded verification QR code links to `https://nag.gov.in/verify?code={qrVerificationCode}`.

---

## Configuration

### Environment Variables & Application Properties

| Property | Environment Variable | Default | Description |
|---|---|---|---|
| `scorecard.storage.mode` | `SCORECARD_STORAGE_MODE` | `s3` (`local` in Monolith Docker) | `s3` for object storage or `local` for filesystem |
| `scorecard.storage.bucket` | `SCORECARD_STORAGE_BUCKET` | `exam-platform-scorecards` | S3 bucket name |
| `scorecard.storage.local.path` | `SCORECARD_STORAGE_PATH` | `./scorecards` | Path for local file storage |
| `scorecard.storage.s3.region` | `AWS_REGION` | `ap-south-1` | AWS S3 region |
| `scorecard.storage.s3.endpoint` | `AWS_S3_ENDPOINT` | `http://localhost:9000` | S3 endpoint override for MinIO / LocalStack |
| `scorecard.storage.s3.access-key` | `AWS_ACCESS_KEY_ID` | `minioadmin` | S3 Access Key |
| `scorecard.storage.s3.secret-key` | `AWS_SECRET_ACCESS_KEY` | `minioadmin` | S3 Secret Key |
| `scorecard.storage.s3.path-style-access` | - | `true` | Required for MinIO and LocalStack |
| `scorecard.storage.s3.presigned-url-duration-minutes` | - | `15` | Expiry duration for presigned URLs |

---

## Local Development in Docker

To run locally in Docker with local filesystem storage:

```yaml
# docker-compose.monolith.yml
services:
  monolith-app:
    environment:
      SCORECARD_STORAGE_MODE: local
      SCORECARD_STORAGE_PATH: /data/scorecards
    volumes:
      - scorecard_storage:/data/scorecards
```

To switch to MinIO or S3 in Docker, configure:

```yaml
services:
  monolith-app:
    environment:
      SCORECARD_STORAGE_MODE: s3
      AWS_S3_ENDPOINT: http://minio:9000
      AWS_ACCESS_KEY_ID: minioadmin
      AWS_SECRET_ACCESS_KEY: minioadmin
      SCORECARD_STORAGE_BUCKET: exam-platform-scorecards
```

---

## Migration from Local Filesystem to S3

If existing scorecards are stored locally on disk:

1. **Bucket Creation**: Create the target bucket in S3/MinIO (e.g. `exam-platform-scorecards`).
2. **Batch Upload**: Copy existing scorecards from the local directory to the S3 bucket maintaining the key structure `scorecards/scorecard-{resultId}.pdf`.
   ```bash
   aws s3 sync ./scorecards/ s3://exam-platform-scorecards/scorecards/ --endpoint-url http://localhost:9000
   ```
3. **Database Records**: The `Result.scorecardPdfRef` column stores relative keys (e.g. `scorecards/scorecard-{resultId}.pdf`), which resolve directly in both local and S3 storage providers.
4. **Deploy Result Service**: Set `SCORECARD_STORAGE_MODE=s3` and deploy `result-service` or `monolith-app`.
