# Compliance & Regulatory Standards — Open Digital Public Infrastructure (DPI) Platform

## 1. Compliance Mapping Framework

NAG complies with national government cybersecurity frameworks and international IT security standards:

| Compliance / Regulation | Mandatory Controls | Platform Implementation |
|---|---|---|
| **MeitY & CERT-In Guidelines** | Mandatory 180-day log retention, 6-hour incident reporting | Log streaming to WORM storage; CERT-In compliant SOC triggers |
| **GIGW 3.0** (Guidelines for Indian Government Websites) | Accessibility, security, usability standards | WCAG 2.1 AA compliance, keyboard navigation, clean semantic HTML |
| **ISO/IEC 27001:2022** | Information Security Management System (ISMS) | Risk assessment matrix, asset classification, access controls |
| **FIPS 140-3** | Cryptographic Module Validation | HSM-backed key storage for exam paper encryption |

---

## 2. Audit Trail & Log Integrity Requirements

1. **Mandatory Audit Event Fields**:
   - `timestamp` (ISO-8601 UTC)
   - `tenantId`
   - `userId` & `roles`
   - `clientIp` & `userAgent`
   - `action` (e.g. `TRANSITION_SCHEDULE`, `CREATE_CENTRE`, `PUBLISH_EXAM`)
   - `resourceId`
   - `resultStatus` (`SUCCESS` / `FAILURE`)

2. **Tamper-Proof Audit Stream**:
   - Audit entries are appended to a hash-chained Merkle Tree stream where each block header includes the hash of the preceding block:

$$H_i = \text{SHA-256}(H_{i-1} \parallel \text{LogEntry}_i)$$

3. **Retention**: All security logs are retained in active cold-storage for a minimum of 180 days per CERT-In directives.

---

## 3. Third-Party Vendor Risk & Open Source Governance

- **Software Bill of Materials (SBOM)**: Generated in CycloneDX format for every release artifact.
- **License Compliance**: All NPM and Maven dependencies are scanned to ensure compatibility with open-source licensing (Apache 2.0 / MIT). Copyleft GPL dependencies are prohibited.
