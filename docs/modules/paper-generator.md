# Module Specification: Paper Generator Service

## 1. Overview & Purpose

The **Paper Generator Service** performs automated question item selection, multi-set randomization (Set A, Set B, Set C), cryptographic AES-256 package assembly, and time-locked distribution.

---

## 2. Core Responsibilities

- Rule-driven random item selection matching topic distributions and difficulty curves.
- Option shuffling and question index randomization for anti-cheating paper sets.
- Cryptographic packaging: encrypting final paper packages using AES-256-GCM.
- Integration with KMS/HSM for Shamir's Secret Sharing key splitting (3-of-5 threshold).

---

## 3. Paper Package Envelope Schema

```json
{
  "packageId": "uuid",
  "examId": "uuid",
  "scheduleVersion": 1,
  "setIdentifier": "SET_A",
  "encryptedContent": "BASE64_AES_256_GCM_BLOB",
  "iv": "BASE64_96BIT_IV",
  "authTag": "BASE64_128BIT_TAG",
  "keyShareHashes": ["SHA256_SHARE1", "SHA256_SHARE2", "SHA256_SHARE3"],
  "createdAt": "2026-08-02T20:00:00Z"
}
```

---

## 4. REST API Reference

Base Path: `/api/v1/papers`

| Method | Path | Roles | Description |
|---|---|---|---|
| `POST` | `/generate` | EXAM_CONTROLLER | Trigger automated paper assembly |
| `GET` | `/packages/{examId}` | EXAM_CONTROLLER, SECURITY_ADMIN | List generated paper packages |
| `POST` | `/packages/{id}/unlock-request` | SECURITY_ADMIN | Submit threshold key share for unlock |
