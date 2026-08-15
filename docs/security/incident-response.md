# Incident Response & Forensic Plan — Open Digital Public Infrastructure (DPI) Platform

## 1. Overview & Incident Classification

The Incident Response Plan (IRP) defines operational protocols for detecting, containing, eradicating, and recovering from security incidents (e.g., paper leaks, unauthorized database access, exam center outage, or ransomware).

| Severity | Definition | Example | Escalation Team |
|---|---|---|---|
| **P1 - CRITICAL** | Active paper disclosure or core exam system compromise | Pre-exam paper published online | CERT-In, Security Admin, Exam Controller, Legal |
| **P2 - HIGH** | Partial service disruption or localized fraud attempt | Exam center network breach during live test | Security Admin, Infrastructure Team |
| **P3 - MEDIUM** | Non-critical security control failure | Misconfigured S3 bucket (no sensitive data exposed) | Security Engineer, DevOps |
| **P4 - LOW** | Minor security anomaly | Single candidate brute-force login attempt | SOC Analyst |

---

## 2. Incident Response Lifecycle

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Preparation │ ──> │ Detection &  │ ──> │ Containment  │
│  & Monitoring│     │ Analysis     │     │ & Isolation  │
└──────────────┘     └──────────────┘     └──────────────┘
                                                 │
┌──────────────┐     ┌──────────────┐            │
│ Lessons      │ <── │ Recovery &   │ <──────────┘
│ Learned      │     │ Eradication  │
└──────────────┘     └──────────────┘
```

### Phase 1: Detection & Analysis
- Automated alerts triggered by SIEM (Elastic/Splunk) on anomalous API request rates, unauthorized DB access, or cryptographic signature failures.
- Security Operations Center (SOC) verifies true positives.

### Phase 2: Containment & Isolation
- **Immediate Paper Freeze**: If a paper leak is suspected, revoke paper decryption keys from HSM/KMS.
- **Tenant Isolation**: Terminate active JWT sessions for impacted tenant/center IDs.
- **Network Isolation**: Block compromised IP ranges or isolate compromised container pods via Kubernetes CNI policy.

### Phase 3: Eradication & Recovery
- Patch root cause vulnerability or revoke compromised credentials.
- Restore database state from immutable read-only snapshots if data tampering occurred.
- Validate system integrity via automated smoke test suite.

### Phase 4: Post-Incident & Lessons Learned
- Conduct root cause analysis (RCA) within 72 hours.
- Update threat models and automated test suites to prevent recurrence.

---

## 3. Forensic Readiness & Chain of Custody

- **Immutable Audit Streams**: Audit logs (`X-Request-Id`, timestamp, user ID, IP) are signed and streamed to Write-Once-Read-Many (WORM) storage.
- **Memory & Storage Dumps**: Before container restart or pod termination, capture memory state and volatile storage snapshots for forensic analysis.
