# Penetration Testing & Red Teaming Policy — Government Examination Platform

## 1. Objectives & Scope

Regular third-party vulnerability assessment and penetration testing (VAPT) is mandatory prior to major release milestones and before conducting national-level examination cycles.

```
┌────────────────────────────────────────────────────────────────────────┐
│                      Scope of Penetration Testing                      │
├──────────────────────────────────┬─────────────────────────────────────┤
│ Web Application & REST APIs      │ Angular Frontend, Spring Boot REST   │
│ Multi-Tenant Context Isolation   │ Header manipulation, Tenant Bleed   │
│ Cryptographic Paper Protection   │ Key extraction, Pre-exam decryption │
│ Exam Center Infrastructure       │ Offline cache tampering, local spoof│
│ Load & DDoS Resilience           │ High-volume API flooding            │
└──────────────────────────────────┴─────────────────────────────────────┘
```

---

## 2. Test Execution Methodology

### 2.1 Black-Box Testing
Simulates external malicious actors with no prior knowledge of credentials or network architecture:
- External API gateway fuzzing and DDoS resilience testing.
- Public web portal vulnerability scanning (SQLi, XSS, CSRF, SSRF).

### 2.2 Grey-Box Testing (Role-Based Access)
Simulates insider threats and privilege escalation across user tiers:
- Candidate attempting to view paper questions before exam start.
- Question author attempting to view final aggregated paper package.
- Evaluator attempting to map anonymous evaluation tokens to candidate roll numbers.

### 2.3 Red Team Simulation (Exam Center Breach)
Simulates physical and local network compromise at an examination center node:
- Attempting to dump volatile RAM to extract time-locked paper decryption keys.
- Man-in-the-Middle payload alteration of submitted candidate answer scripts.

---

## 3. Environment Rules of Engagement

- **Dedicated Staging Environment**: Penetration testing MUST be conducted against isolated staging/testing environments. Testing against live production databases during an active exam is strictly prohibited.
- **Sanitized Test Data**: Staging environments must contain synthetic test data. Production candidate PII or actual question bank items must never be loaded into testing environments.
- **Audit Logging Validation**: Penetration testing activities must be verified in SOC monitoring dashboards to ensure security alerts trigger correctly.

---

## 4. Certification & Audit Sign-Off

Before any national examination goes live:
1. An accredited Indian Computer Emergency Response Team (CERT-In) empanelled security auditor must perform VAPT.
2. All **Critical** and **High** vulnerabilities identified in the VAPT report must be patched and re-tested.
3. A formal **Safe-to-Host Certificate** must be issued and signed by the Chief Security Officer (CSO).
