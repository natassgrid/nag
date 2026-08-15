# Data Privacy & Candidate Protection — Open Digital Public Infrastructure (DPI) Platform

## 1. Overview & Compliance Framework

NAG is designed in compliance with the **Digital Personal Data Protection Act (DPDP Act 2023)** and international privacy standards (GDPR). Candidate privacy, biometric security, and score confidentiality are guaranteed by architectural safeguards.

---

## 2. PII Inventory & Data Minimization

| Data Element | Sensitivity | Storage Encryption | Purpose | Retention Period |
|---|---|---|---|---|
| Full Name, Email, Phone | **PII** | AES-256-GCM | Identification & Communication | Exam cycle + 1 year |
| National ID / Aadhaar Hash | **SENSITIVE PII** | SHA-256 Salted Hash | Identity Verification | Non-reversible hash stored permanently |
| Candidate Biometrics (Fingerprint/Photo) | **RESTRICTED PII** | AES-256-GCM | On-site Attendance Authentication | Purged 90 days post-results publication |
| Exam Response Sheet | **RESTRICTED** | Cryptographic Digest | Grading & Evaluation | Permanent historical record |

---

## 3. Evaluation Anonymization (Pseudonymization)

To eliminate evaluator bias and protect candidate identity during grading of subjective answers:
1. **Fictitious Roll Codes**: Candidate Roll Numbers are replaced with randomized 128-bit evaluation tokens before being displayed to evaluators.
2. **PII Masking**: Personal details, center location, and state details are automatically stripped from evaluator interfaces.
3. **Decoupled Key**: Re-identification mapping between Candidate ID and Evaluation Token is stored in a isolated database accessible only to the `EXAM_CONTROLLER` after evaluation lock.

---

## 4. Candidate Rights & Consent Management

- **Notice & Consent**: Explicit consent is captured during registration stating purpose of biometric and personal data processing.
- **Right to Rectification**: Candidates can request corrections to profile details prior to exam notification lock.
- **Data Erasure**: Candidate personal records (excluding official exam score certificates) are subject to automated purging schedules following legal retention expiry.
