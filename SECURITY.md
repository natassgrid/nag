# Security Policy

# Security at NAG

Security is the foundation of the **Next-generation Assessment Grid (NAG)**.

NAG is designed as an Open Digital Public Infrastructure (DPI) platform for conducting secure, scalable, transparent, and AI-ready assessments, entrance examinations, certifications, and recruitment processes.

Because NAG may be used for high-stakes examinations, we take security vulnerabilities seriously and appreciate responsible disclosure from the community.

---

# Supported Versions

Only actively maintained releases receive security updates.

| Version | Supported |
|----------|-----------|
| 1.x | ✅ |
| 0.x | Best Effort |

---

# Reporting a Security Vulnerability

**Please DO NOT create a public GitHub Issue for security vulnerabilities.**

Instead, report vulnerabilities privately.

Include:

- Description
- Impact
- Steps to reproduce
- Proof of Concept (if available)
- Suggested mitigation
- Contact information

---

# Response Timeline

| Activity | Target |
|----------|---------|
| Initial acknowledgement | 48 Hours |
| Initial assessment | 5 Business Days |
| Risk classification | 7 Business Days |
| Fix development | Depends on severity |
| Public disclosure | After fix is available |

---

# Severity Classification

## Critical

Examples

- Authentication bypass
- Authorization bypass
- Remote Code Execution
- Question paper exposure
- Encryption key compromise
- Database compromise
- Privilege escalation
- Digital signature compromise

Target Fix

Within 72 hours whenever possible.

---

## High

Examples

- Sensitive information disclosure
- SQL Injection
- SSRF
- XXE
- Stored XSS
- JWT validation bypass

Target

Within 14 days.

---

## Medium

Examples

- Reflected XSS
- Missing security headers
- Configuration issues
- Rate limiting weaknesses

Target

Within 30 days.

---

## Low

Examples

- Information leakage
- Minor configuration improvements
- Security hardening recommendations

Target

Next scheduled release.

---

# Security Principles

NAG follows the following principles:

- Zero Trust Architecture
- Least Privilege
- Defense in Depth
- Secure by Default
- Privacy by Design
- Fail Secure
- Principle of Least Knowledge
- Continuous Verification
- Immutable Audit Logging
- Secure Software Supply Chain

---

# Authentication

Supported mechanisms include:

- OAuth 2.1
- OpenID Connect (OIDC)
- Multi-Factor Authentication (MFA)
- Passkeys (Future)
- WebAuthn (Future)
- SAML 2.0 (Enterprise)

---

# Authorization

NAG supports:

- Role-Based Access Control (RBAC)
- Attribute-Based Access Control (ABAC)
- Fine-grained Permissions
- Multi-level Approval Workflows
- Separation of Duties (SoD)

---

# Cryptography

Recommended algorithms:

## Data at Rest

- AES-256-GCM

## Data in Transit

- TLS 1.3

## Password Hashing

- Argon2id

## Digital Signatures

- RSA-4096
- ECDSA P-384 (Optional)

## Hashing

- SHA-256
- SHA-512

---

# Secrets Management

Sensitive credentials should never be committed to Git.

Recommended solutions:

- HashiCorp Vault
- Cloud Secret Managers
- Kubernetes Secrets
- Hardware Security Modules (HSM)

---

# Secure Development

Developers should:

- Enable dependency scanning
- Enable secret scanning
- Enable CodeQL
- Review pull requests
- Write security tests
- Follow OWASP ASVS
- Follow OWASP Top 10

---

# Examination Security

NAG is designed with support for:

- End-to-End Question Paper Encryption
- Multi-party Approval
- Time-Locked Paper Release
- Digital Signatures
- Secure Question Distribution
- Candidate Identity Verification
- Immutable Audit Trails
- Secure Browser Integration
- AI-assisted Proctoring
- Anti-Tampering Controls

---

# Infrastructure Security

Recommended deployment:

- Kubernetes
- Network Policies
- Service Mesh (Istio/Linkerd)
- mTLS
- API Gateway
- WAF
- DDoS Protection
- Image Signing
- Runtime Security
- Container Scanning

---

# Logging & Monitoring

Security monitoring should include:

- Authentication Events
- Authorization Failures
- Administrative Actions
- Examination Lifecycle Events
- Configuration Changes
- API Access Logs
- Audit Logs
- Suspicious Activity Detection

Recommended tools:

- OpenTelemetry
- Prometheus
- Grafana
- Jaeger
- SIEM Integration

---

# Responsible Disclosure

Researchers acting in good faith will not be subject to legal action for responsibly reporting vulnerabilities.

We request that you:

- Give us reasonable time to investigate.
- Avoid accessing or modifying user data.
- Avoid disrupting production systems.
- Keep vulnerability details confidential until a fix is released.

---

# Security Roadmap

Upcoming security enhancements include:

- Hardware Security Module (HSM) Integration
- Threshold Cryptography
- Confidential Computing
- FIDO2 Authentication
- Passkey Support
- Secure Remote Proctoring
- AI-powered Threat Detection
- Tamper-Evident Audit Ledger
- Software Bill of Materials (SBOM)
- SLSA-Compliant Build Pipeline

---

# Security Standards

NAG aims to align with industry best practices including:

- OWASP Top 10
- OWASP ASVS
- OWASP API Security Top 10
- CWE Top 25
- NIST Secure Software Development Framework (SSDF)
- CIS Benchmarks

---

# Contact

For security-related questions or responsible disclosure, please contact the project maintainers through the project's private security reporting channel.

---

> **Security is not a feature—it is a core design principle of NAG.**