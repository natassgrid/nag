# Security Policy

## Supported Versions

The following versions of the Open Source Government Examination Platform receive security updates:

| Version | Supported          |
| ------- | ------------------ |
| 1.x (latest) | ✅ Active support |
| < 1.0   | ❌ No longer supported |

Security patches are backported only to the **latest minor release** on the `main` branch.
Older releases are not patched; users are encouraged to upgrade.

---

## Reporting a Vulnerability

**Do not report security vulnerabilities as public GitHub Issues.**
Public disclosure of a vulnerability before a patch is available puts all users at risk.

### Private Disclosure Process

1. **Email the security team** at `security@examplatform.example.com` with the subject line:
   `[SECURITY] <Short description of the issue>`

2. Include the following information in your report:
   - Description of the vulnerability and its potential impact
   - Affected component(s) and version(s)
   - Step-by-step reproduction instructions
   - Any proof-of-concept code or screenshots (remove real PII before attaching)
   - Your suggested remediation, if any

3. **Encrypt sensitive details** using the project's PGP public key (published at
   `https://examplatform.example.com/.well-known/security.txt`).

4. You will receive an acknowledgement within **48 hours** confirming receipt.

---

## Response Timeline

| Stage | Target Time |
| ----- | ----------- |
| Acknowledgement of report | 48 hours |
| Initial triage and severity assessment | 5 business days |
| Patch development begins (Critical/High) | 7 business days |
| Patch development begins (Medium/Low) | 30 business days |
| Coordinated public disclosure | After patch is released and users have had time to upgrade (minimum 90 days from report) |

We follow [Responsible Disclosure](https://en.wikipedia.org/wiki/Responsible_disclosure) practices.
If you believe a critical vulnerability is being exploited in the wild, please indicate this clearly
in your report so we can expedite the response.

---

## Severity Classification

We use the [CVSS v3.1](https://www.first.org/cvss/calculator/3.1) scoring system to classify severity:

| CVSS Score | Severity |
| ---------- | -------- |
| 9.0 – 10.0 | Critical |
| 7.0 – 8.9  | High     |
| 4.0 – 6.9  | Medium   |
| 0.1 – 3.9  | Low      |

---

## Security Contacts

| Role | Contact |
| ---- | ------- |
| Security Lead | `security@examplatform.example.com` |
| Maintainers | See [CODEOWNERS](.github/CODEOWNERS) |

For general (non-security) bug reports, use the [GitHub Issues](../../issues) tracker.

---

## Scope

The following are **in scope** for security reports:

- All backend services under `backend/`
- API Gateway authentication and authorization
- HSM/Vault key management integration
- Candidate PII handling and encryption
- Authentication flows (OAuth2/OIDC, MFA, WebAuthn)
- Audit trail tamper detection
- CI/CD pipeline security

The following are **out of scope**:

- Social engineering attacks targeting project maintainers
- Denial-of-service attacks requiring physical access
- Issues in third-party dependencies (report these to the upstream project directly)
- Theoretical vulnerabilities without a working proof-of-concept

---

## Security Best Practices for Contributors

- Never commit secrets, credentials, or private keys to the repository.
- Use `vault` or environment variables for all sensitive configuration.
- Follow secure coding guidelines in [CONTRIBUTING.md](CONTRIBUTING.md).
- Run `./gradlew spotbugsMain` and `./gradlew dependencyCheckAnalyze` before submitting a PR.

---

*This security policy is adapted from the [GitHub Security Advisory](https://docs.github.com/en/code-security/security-advisories) best practices.*
