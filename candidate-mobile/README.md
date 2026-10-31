# NAG Candidate Mobile Application (`candidate-mobile`)

Official **NextGen Assessment Grid (NAG)** Candidate Mobile Application for **Android** (`API 26+` / Android 8.0 to Android 15+), built with **React Native (Hermes Engine)**, **Expo EAS**, and **Bridgeless Native TurboModules** in accordance with [ADR 0008](../docs/adr/0008-mobile-app-architecture-and-platform-selection.md), [Issue #158](https://github.com/natassgrid/nag/issues/158), and 100% feature parity with `candidate-delivery`.

---

## Architecture & Workspace Layout

```text
candidate-mobile/
├── packages/
│   └── candidate-core/               # Shared Domain Core (@nag/candidate-core)
│       └── src/
│           ├── models/               # Auth, Admit Card, Catalog, CBT & Security contracts
│           ├── validation/           # Registration, OTP/TOTP, 3-step Application validators
│           ├── crypto/               # ECDSA P-256 / Ed25519 Offline QR & SHA-256 Digest Verifier
│           ├── state/                # NTA 5-Status CBT State Machine & Local Grading Engine
│           └── api/                  # Shared API Client, JWT Refresh Lifecycle & Offline Sync Queue
└── apps/
    └── candidate-mobile/             # Android React Native + Expo Application
        ├── app.json                  # Expo Android config (Hermes AOT, New Architecture, Permissions)
        ├── eas.json                  # Expo Application Services (EAS) Android build profiles
        ├── android/                  # Native Android Gradle config & Kotlin TurboModules
        │   └── app/src/main/java/in/gov/nag/mobile/security/
        │       ├── KioskLockdownModule.kt        # Android LockTaskMode Kiosk controller
        │       ├── ScreenSecurityModule.kt       # Window FLAG_SECURE screenshot/recording blocker
        │       ├── HardwareKeystoreModule.kt     # Android Keystore StrongBox/TEE ECDSA P-256 signer
        │       ├── ProctoringCompanionModule.kt  # Ambient dB noise & camera/device orientation sensor
        │       └── NagSecurityPackage.kt         # TurboReactPackage registration
        └── src/
            ├── native/               # TypeScript Bridgeless TurboModule specs & bridges
            ├── services/             # Encrypted SecureStore/SQLite Wallet & FCM Push Notifications
            ├── features/
            │   ├── layout/           # Mobile Navigation Shell, Header & Collapsible Drawer
            │   ├── auth/             # Registration, Mobile OTP Login & 2FA TOTP Verification
            │   ├── dashboard/        # Candidate Dashboard, KPI Cards, Live/Upcoming Exams
            │   ├── wallet/           # Offline Hall Ticket Wallet & High-Density Signed QR Renderer
            │   ├── catalog/          # Exam Discovery with Real-Time Eligibility Filters
            │   ├── application/      # 3-Step Exam Application (3 Centre Choices, Shift, PwD/Scribe)
            │   ├── learn/            # Onboarding, AI Recommendations, DLP Roadmap & 5-Part Micro-Lessons
            │   ├── practice/         # Practice Hub, Custom Drill Builder, Result Panel & History
            │   ├── formula/          # LaTeX/KaTeX ($$...$$, \ce{}, \pu{}) & SMILES 2D SVG Renderer
            │   ├── cbt/              # Mobile CBT Mock Engine, NTA Palette & Local Performance Breakdown
            │   ├── review/           # Recorded Response Sheet Review & Answer Key Challenge Workflow
            │   ├── results/          # Multi-Shift Normalized Scorecards, AIR/Cutoffs & Verifiable QR
            │   └── profile/          # Personal/State-City Cascade, Education CRUD, DigiLocker & PwD
            └── App.tsx               # Root Mobile Navigation & Session Orchestrator
```

---

## Key Capabilities Delivered

### 1. Android Foundation, Auth & Offline Hall Ticket Wallet (`v0.8-alpha`)
- **Expo EAS & Android Gradle Configuration**: Targeting Android API 26–35 with Hermes bytecode compilation, R8/ProGuard shrinking, and split-screen suppression (`android:resizeableActivity="false"`).
- **Candidate Onboarding & Multi-Factor Auth**: Self-registration with DPDP consent, 6-digit Mobile OTP verification, 2FA TOTP authentication, and DigiLocker/Aadhaar profile binding.
- **Offline Hall Ticket Wallet**: Encrypted local storage backed by Android Keystore (`EncryptedSharedPreferences` / `expo-secure-store`) and offline SQLite caching with cryptographically signed QR codes (`ECDSA P-256` / `Ed25519`) operable with zero internet connectivity at exam gates.
- **Firebase Cloud Messaging (FCM)**: High-priority Android notification channels for gate closing countdowns, shift schedules, and admit card issuance.

### 2. Full `candidate-delivery` Feature Parity (`v0.8-beta`)
- **Candidate Dashboard (`features/dashboard/`)**: Live exam alert banner, KPI summary counters, enrolled exam status filters (`ALL`, `LIVE`, `UPCOMING`, `PRACTICE`, `COMPLETED`), and quick admit card / practice launches.
- **Exam Catalog & 3-Step Application Wizard (`features/catalog/`, `features/application/`)**: Real-time eligibility filters, 3 mutually distinct examination centre choices across India, shift preferences, PwD/Scribe accommodations, and instant Hall Ticket generation (`HT-YYYY-XXXXXX`).
- **Adaptive Learning & 5-Part Micro-Lessons (`features/learn/`)**: Learner diagnostic onboarding, AI Study Plan & weakness recommendations, Dynamic Learning Path (DLP) phased STAT milestones & Gatekeeper mock, and 5-part interactive Micro-Lesson player with AI Explanation IR steps.
- **Practice Hub & Analytics (`features/practice/`)**: Official mock papers, custom drill generator (by subject, difficulty, question count, timed/untimed mode), per-question solution review with deep links to weak-subtopic micro-lessons, and historical accuracy trends.
- **Mobile CBT Engine & Formula Renderer (`features/cbt/`, `features/formula/`)**: NTA 5-status question palette (`ANSWERED`, `NOT_ANSWERED`, `MARKED_FOR_REVIEW`, `ANSWERED_AND_MARKED_FOR_REVIEW`, `NOT_VISITED`), `$$ ... $$` KaTeX math, `\ce{...}` / `\pu{...}` mhchem chemistry, and `[SMILES: ...]` 2D molecular structure graphs.
- **Recorded Response Review & Answer Key Challenge (`features/review/`)**: Provisional answer key inspection, +4/-1 scoring summary, and formal objection submission with textbook citation and challenge fee receipt.
- **Official Scorecards & Normalized Results (`features/results/`)**: Raw vs Normalized marks, NTA Percentile, Z-Score, All-India Rank (AIR), Category-wise cutoff qualification, Merkle proof verification, and DigiLocker deposit.
- **Candidate Profile (`features/profile/`)**: Indian State-to-City cascading address selector, Education Qualifications CRUD, DigiLocker e-KYC verification, and weighted profile completion tracker.

### 3. Hardware Security, Kiosk Lockdown & Proctoring Companion (`v0.9`)
- **`LockTaskMode` Kiosk TurboModule**: Native Android Screen Pinning and Device Owner `LockTaskMode` enforcement preventing app switching, notifications, and system overlays.
- **`FLAG_SECURE` Window Guard**: Blocks screenshots, screen recordings, and wireless display casting during active CBT sessions.
- **Android Hardware Keystore Signing**: Hardware-backed `secp256r1` (`ECDSA P-256`) digital signatures over candidate response bundles and submission receipts using StrongBox Keymaster / TEE.
- **Companion Proctoring Sensors**: Real-time ambient noise decibel spike monitoring and device pitch/roll orientation anomaly checks.

---

## Verification & Commands

```bash
cd candidate-mobile
npm test
npm run typecheck
npm run lint
```
