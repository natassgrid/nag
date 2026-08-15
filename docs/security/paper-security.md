# Exam Paper Security & Leakage Prevention — Open Digital Public Infrastructure (DPI) Platform

## 1. Overview

Paper leakage during national competitive examinations poses a existential threat to testing integrity. NAG implements end-to-end cryptographic and architectural countermeasures to safeguard question papers throughout their lifecycle: Authoring $\rightarrow$ Review $\rightarrow$ Selection $\rightarrow$ Assembly $\rightarrow$ Encryption $\rightarrow$ Delivery.

---

## 2. Automated Paper Assembly & Isolation

1. **Blind Question Selection**:
   - Question items are selected by automated algorithm based on difficulty curve, topic weightage, and bloom taxonomy rules.
   - Authors and reviewers never see the final combined paper set.
   - Questions in the bank are stored encrypted; item metadata (topic, difficulty) is accessible to the assembly engine without decrypting body text.

2. **Multi-Set Generation**:
   - For every examination shift, NAG generates multiple randomized sets (Set A, Set B, Set C, Set D) with jumbled question and option ordering.

---

## 3. Dynamic Watermarking Controls

To trace unauthorized leaks or screen captures:

### 3.1 Visual Watermarking
Every rendering of an exam paper or question item on a client terminal displays a dynamic, semi-transparent background overlay containing:
- Candidate Roll Number / User ID
- Center ID & IP Address
- Dynamic Timestamp & Session Token Hash

### 3.2 Steganographic Watermarking
For printable or PDF exports (e.g., offline paper delivery):
- Zero-width character insertion and subtle font kerning adjustments encode the target center ID and download timestamp into the text stream itself.
- Extracted images of leaked paper pages can be analyzed by NAG forensics tools to immediately pinpoint the origin center and user account.

---

## 4. Just-In-Time (JIT) Key Distribution

```
  [ Sealed Encrypted Paper Package ] ──( Distributed 24 Hours Early )──> [ Exam Center Local Node ]
                                                                                  │
                                                                   ( Locked — Awaiting Key )
                                                                                  │
  [ Threshold Approval (3 of 5) ] ───> [ KMS / HSM Key Release ] ──( T - 15 Mins )──┘
```

1. **Pre-Distribution**: Encrypted paper packages are safely distributed to test centers 24 hours prior to exam start. Without the decryption key, packages are computationally infeasible to decrypt (AES-256-GCM).
2. **Time-Locked Key Release**: At $T-15$ minutes before start time, once quorum approval (3-of-5 threshold) is satisfied, the decryption key is released over secure TLS channels directly to the volatile memory (RAM) of center proctor nodes. Keys are never written to disk.
3. **Automatic Purge**: Post-exam, decryption keys in node memory are zeroized.
