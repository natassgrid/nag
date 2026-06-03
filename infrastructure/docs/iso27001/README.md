# ISO 27001 Evidence Package — Examination Platform

## Overview

This directory documents the ISO 27001:2022 Information Security Management System (ISMS) evidence structure for the Open Source Government Examination Platform. All artifacts are maintained for audit readiness and are accessible to users with the **Auditor** role via the Admin Service API.

## Evidence Package Structure

### 1. Asset Inventory (`asset-inventory/`)

| Category | Contents |
|----------|----------|
| Information Assets | Database schemas, encryption keys, exam papers, candidate PII |
| Software Assets | Microservices inventory, third-party dependencies (SBOM), container images |
| Hardware Assets | Kubernetes nodes, managed cloud services, HSM modules |
| Network Assets | VPC configurations, load balancers, DNS records |

- **Format**: CSV export + JSON (machine-readable)
- **Update frequency**: Automated weekly via CI pipeline
- **Owner**: Security_Admin role

### 2. Risk Register (`risk-register/`)

| Field | Description |
|-------|-------------|
| Risk ID | Unique identifier (RISK-XXXX) |
| Category | Confidentiality / Integrity / Availability |
| Description | Plain-language risk statement |
| Likelihood | 1–5 scale |
| Impact | 1–5 scale |
| Risk Score | Likelihood × Impact |
| Controls | Implemented mitigations |
| Residual Risk | Post-control risk level |
| Owner | Responsible role |
| Review Date | Last assessment date |

- **Format**: CSV + PDF report
- **Update frequency**: Quarterly review, ad-hoc on incidents
- **Owner**: Security_Admin role

### 3. Access Control Records (`access-control/`)

| Record Type | Source |
|-------------|--------|
| Role assignments | Keycloak realm export |
| Permission matrices | RBAC/ABAC policy definitions |
| Service account credentials | Vault audit log |
| MFA enrollment status | Identity Service database |
| Session logs | Redis + audit events |
| Privilege escalation events | Kafka `exam.audit.events` |

- **Format**: JSON exports from Keycloak + audit event queries
- **Update frequency**: Real-time (audit events), daily snapshots
- **Owner**: Security_Admin + Auditor roles
- **API endpoint**: `GET /api/v1/audit/events?actionType=ROLE_CHANGE,ACCESS_DENIED,ACCOUNT_LOCK`

### 4. Audit Log Exports (`audit-logs/`)

| Log Type | Retention | Source |
|----------|-----------|--------|
| Application audit events | 7 years (≥365 days online) | `audit_service.audit_event` table |
| Infrastructure access logs | 365 days | Cloud provider audit trails |
| Database query logs | 90 days | PostgreSQL `log_statement` |
| Authentication events | 7 years | Identity Service + Keycloak |
| Encryption key operations | 7 years | Vault audit backend |

- **Tamper evidence**: Every audit event is SHA-256 hashed and signed with HSM ECDSA P-256 key
- **Immutability**: `REVOKE UPDATE, DELETE ON audit_event` enforced at DB level
- **API endpoint**: `GET /api/v1/audit/events` (Auditor role, paginated, filterable)
- **Export format**: JSON Lines (.jsonl), signed with platform ECDSA key

## Controls Mapping (Annex A)

| ISO 27001 Control | Platform Implementation |
|-------------------|------------------------|
| A.5.1 Information security policies | SECURITY.md, this document |
| A.5.15 Access control | Keycloak RBAC, 10 named roles, least privilege |
| A.5.23 Information security for cloud services | External Secrets Operator, no plaintext secrets |
| A.8.2 Privileged access rights | Super_Admin/Security_Admin separation, audit trail |
| A.8.5 Secure authentication | MFA, WebAuthn/FIDO2, account lockout after 5 attempts |
| A.8.9 Configuration management | Helm values, GitOps, immutable containers |
| A.8.12 Data leakage prevention | AES-256 encryption at rest, TLS 1.3 in transit |
| A.8.15 Logging | Structured JSON logs, OpenTelemetry, 365-day retention |
| A.8.16 Monitoring | Grafana dashboards, alerting within 2 minutes |
| A.8.24 Use of cryptography | Vault Transit Engine, per-entity DEKs, HSM-backed |
| A.8.25 SDLC security | SAST, DAST, Dependency-Check, Trivy in CI/CD |
| A.8.28 Secure coding | SpotBugs, Semgrep, OWASP Top 10 rules |

## Generating Evidence Exports

```bash
# Export audit logs for a date range (Auditor role required)
curl -H "Authorization: Bearer $TOKEN" \
  "https://api.exam-platform.gov/api/v1/audit/events?from=2024-01-01&to=2024-03-31&format=jsonl" \
  -o audit-export-Q1-2024.jsonl

# Export access control snapshot
curl -H "Authorization: Bearer $TOKEN" \
  "https://api.exam-platform.gov/api/v1/admin/access-control/export" \
  -o access-control-snapshot.json

# Verify audit event integrity
curl -H "Authorization: Bearer $TOKEN" \
  "https://api.exam-platform.gov/api/v1/audit/events/{eventId}/verify"
```

## Review Schedule

| Activity | Frequency | Responsible |
|----------|-----------|-------------|
| Risk register review | Quarterly | Security_Admin |
| Access control review | Monthly | Security_Admin + Auditor |
| Penetration testing | Annually | External vendor |
| ISMS internal audit | Semi-annually | Auditor role |
| Evidence package update | Continuous (automated) | CI/CD pipeline |
