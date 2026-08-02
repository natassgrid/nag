# Cryptography Standards & Key Management — Government Examination Platform

## 1. Cryptographic Standards

NAG mandates modern, standardized cryptographic primitives aligned with FIPS 140-3 and NIST recommendations. Legacy algorithms (MD5, SHA-1, RSA-1024, DES, 3DES, AES-CBC without HMAC) are strictly prohibited.

| Application Domain | Algorithm / Primitive | Parameter / Standard | Usage Notes |
|---|---|---|---|
| Data at Rest Encryption | **AES-GCM** | 256-bit key, 96-bit IV | Authenticated encryption for database fields |
| Data in Transit | **TLS 1.3** | AES-256-GCM / CHACHA20-POLY1305 | PFS via ECDHE |
| Password Storage | **Argon2id** | $m=64\text{MB}, t=3, p=4$ | Salted password hashing |
| Digital Signatures | **Ed25519** / **ECDSA** | Curve25519 / P-384 | Audit log signing, Paper integrity |
| Paper Encryption Key | **AES-256-GCM** | Split via Shamir's Secret Sharing | Exam paper package encryption |
| Key Exchange | **ECDHE-P384** | Ephemeral Elliptic Curve Diffie-Hellman | Time-locked key releases |

---

## 2. Key Lifecycle Management

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ Key Creation │ ──> │ Key Storage  │ ──> │  Key Usage   │ ──> │ Key Rotation │
│ (HSM / KMS)  │     │ (Vault / KMS)│     │  (Time-bound)│     │  & Revocation│
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
```

1. **Generation**: Master Keys are generated inside FIPS 140-3 Level 3 compliant Hardware Security Modules (HSMs) or cloud Key Management Systems (KMS).
2. **Storage**: Data Encryption Keys (DEKs) are envelope-encrypted using Key Encryption Keys (KEKs) stored within KMS/Vault.
3. **Rotation**:
   - JWT Signing Keys: Rotated every 30 days.
   - Database Envelope Keys: Rotated annually.
   - Exam Paper Decryption Keys: Ephemeral; generated per schedule version and discarded post-exam evaluation.

---

## 3. Shamir's Secret Sharing (Split Key Scheme)

To prevent any single official from releasing an exam paper prematurely, the Master Paper Unlocking Key $K_{\text{paper}}$ is split using a $(k, n)$ threshold Shamir Secret Sharing scheme:
- $n = 5$ key shares distributed to independent authorities (Exam Controller, Security Admin, Chairman, Chief Proctor, External Auditor).
- $k = 3$ threshold required to reconstruct $K_{\text{paper}}$ $T-15$ minutes prior to scheduled exam start time.

$$\text{Reconstructed Key } K_{\text{paper}} = \sum_{i=1}^{k} \ell_i(0) \cdot S_i \pmod{p}$$

---

## 4. Response Bundle Digital Signatures

When a candidate completes an exam, their encrypted response payload $R$ is hashed and signed on the exam center node:

$$\text{Digest} = \text{SHA-256}(R \parallel \text{Timestamp} \parallel \text{CandidateID} \parallel \text{ShiftID})$$
$$\text{Signature} = \text{Sign}_{\text{CenterPrivateKey}}(\text{Digest})$$

The signature and hash are bundled into the final response manifest, ensuring tamper-evident delivery to central servers.
