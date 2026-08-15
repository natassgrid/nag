# Threat Model — Open Digital Public Infrastructure (DPI) Platform

## 1. Overview & Objectives

The **National Assessment Grid (NAG)** is an open-source Open Digital Public Infrastructure (DPI) Platform designed to manage high-stakes competitive examinations, question bank generation, paper encryption, exam delivery, and evaluation. Given the high-stakes nature of public sector examinations, securing the system against unauthorized paper disclosure, candidate impersonation, tampering, and denial-of-service attacks is paramount.

This document outlines the system threat model based on the **STRIDE** methodology (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege).

---

## 2. System Architecture & Trust Boundaries

```
[ Candidate / Center Terminal ] ──( Untrusted / Semi-trusted Boundary )──> [ Ingress Gateway / WAF ]
                                                                                   │
                                                                   ( Fully Trusted Internal Network )
                                                                                   │
                                                                         [ Spring Boot Microservices ]
                                                                                   │
                                                                  ┌────────────────┴────────────────┐
                                                                  ▼                                 ▼
                                                        [ PostgreSQL / DB ]              [ Cryptographic HSM / Key Store ]
```

### Trust Boundaries
1. **External / Candidate Boundary**: Untrusted exam delivery terminals, candidate personal devices (for web-based registration/results), and public networks.
2. **Exam Centre Boundary**: Semi-trusted local exam center networks, invigilator consoles, and local caching servers.
3. **Internal Application Boundary**: Fully trusted internal backend services, database clusters, and cryptographic Hardware Security Modules (HSM).

---

## 3. Asset Classification

| Asset | Sensitivity | Impact of Compromise |
|---|---|---|
| Question Bank & Item Repository | **RESTRICTED** | Complete exam invalidation, public trust breakdown |
| Final Generated Exam Papers | **SECRET** | Exam paper leak, cancellation of national-level exam |
| Candidate Biometrics & PII | **CONFIDENTIAL** | Privacy violation, legal liability (DPDP Act) |
| Candidate Response Sets | **RESTRICTED** | Score manipulation, fraud |
| Audit Logs & Integrity Chains | **RESTRICTED** | Anti-forensics, loss of accountability |

---

## 4. STRIDE Threat Analysis

### 4.1 Spoofing (Identity & Auth)
- **Threat**: Candidate impersonation at test center or remotely during web registration.
- **Threat Agent**: Malicious candidates, proxy test-takers.
- **Mitigation**: Multi-factor authentication (MFA), biometric matching at exam centers, signed JWT access tokens with strict expiration, and tenant-scoped session validation (`X-Tenant-Id`).

### 4.2 Tampering (Data Integrity)
- **Threat**: Modification of candidate response logs, exam scores, or question bank parameters in-transit or at-rest.
- **Threat Agent**: Malicious internal admins, database operators, exam center proctors.
- **Mitigation**: Cryptographic hashing (SHA-256) of response bundles, immutable audit logging, database field-level encryption, and strict RBAC (`EXAM_CONTROLLER`, `SECURITY_ADMIN`, `AUDITOR`).

### 4.3 Repudiation (Non-Deniability)
- **Threat**: Candidate or author denying question submission, score evaluation, or exam completion.
- **Threat Agent**: Candidates, evaluation staff.
- **Mitigation**: Digital signatures on submitted response bundles, tamper-evident audit logs tagged with `X-Request-Id`, user IDs, and timestamps.

### 4.4 Information Disclosure (Confidentiality)
- **Threat**: Early leakage of encrypted paper keys prior to exam start time.
- **Threat Agent**: Insiders, network eavesdroppers, compromised center nodes.
- **Mitigation**: Split-key cryptography, time-bound key distribution (keys released $T-15$ minutes before exam start via HSM/KMS), TLS 1.3 for all transport.

### 4.5 Denial of Service (Availability)
- **Threat**: DDoS attacks against API gateways during peak exam start windows causing exam disruption.
- **Threat Agent**: Distributed botnets, state-sponsored disruption.
- **Mitigation**: Rate limiting, Web Application Firewall (WAF), CDN edge caching for static assets, isolated delivery clusters per tenant.

### 4.6 Elevation of Privilege (Authorization)
- **Threat**: A candidate or reviewer elevating permissions to `EXAM_CONTROLLER` or `SUPER_ADMIN`.
- **Threat Agent**: External attackers, low-privileged users.
- **Mitigation**: Fine-grained role-based access control (`roleGuard` & Spring `@PreAuthorize`), strict API contract validation, tenant boundary enforcement.

---

## 5. Risk Rating Matrix

| Threat ID | Threat Description | Likelihood | Impact | Risk Level |
|---|---|---|---|---|
| T-01 | Pre-exam paper leakage via insider access | Medium | Critical | **HIGH** |
| T-02 | Man-in-the-Middle payload tampering during answer submission | Low | High | **MEDIUM** |
| T-03 | Candidate proxy attendance / identity spoofing | High | High | **HIGH** |
| T-04 | DDoS on central API server during scheduled exam start | High | Critical | **CRITICAL** |
| T-05 | Audit log deletion/alteration by compromised admin | Low | High | **MEDIUM** |
