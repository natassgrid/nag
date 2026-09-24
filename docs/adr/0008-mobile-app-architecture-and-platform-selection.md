# ADR 0008: Mobile Application Architecture & Framework Evaluation (Android & iOS)

- **Status**: Proposed
- **Date**: 2026-09-20
- **Authors**: NAG Core Engineering Team
- **Scope**: Candidate Self-Service, Admit Card/Hall Ticket Wallet, Secure CBT Delivery & Offline Proctoring Companion
- **Target OS**: Android (API 26+) & iOS (16.0+)

---

## 1. Context & Business Requirements

The **National Assessment Grid (NAG)** is designed as India's open, high-consequence Digital Public Infrastructure (DPI) for conducting massive-scale examinations (e.g., NTA, UPSC, SSC, state service commissions). While the current candidate portal is a responsive web application (`candidate-frontend` in React 19 + Tailwind), a native mobile application is required to satisfy key public sector and field deployment demands:

1. **Digital Public Infrastructure (DPI) & Candidate Accessibility**:
   - Millions of students access exam notifications, application submissions, and admit cards exclusively through mobile devices.
   - Low-bandwidth, intermittent 4G/2G connectivity in Tier-2/3/rural districts requires offline caching, resilient retry sync, and ultra-low APK/bundle sizes (< 25 MB).
2. **Offline Digital Hall Ticket & Verification Wallet**:
   - Secure offline storage of admit cards with cryptographic QR verification (ECDSA/Ed25519) and biometric passkeys (DigiLocker / Aadhaar identity alignment).
   - Live exam day alerts (gate open/close, shift changes, venue navigation via GPS).
3. **High-Security Mobile Delivery / Test Practice & Proctoring Companion**:
   - **Kiosk / Lockdown Mode**: Single-App Mode (iOS Guided Access / Autonomous Single App Mode; Android Lock Task Mode / Device Owner Kiosk) to prevent app switching, screen recording, overlays, and split screen.
   - **Hardware Security Module / Biometrics**: Android Keystore (StrongBox Keymaster) and iOS Secure Enclave for zero-trust cryptographic signature generation per response packet.
   - **On-Device Proctoring Sensors**: Low-overhead camera preview, background noise level check, face orientation inference (TensorFlow Lite / Apple CoreML / MediaPipe).
4. **Codebase Reusability & Shared Business Logic**:
   - Maintain parity with existing web contracts (`candidate-frontend` REST/SSE APIs, KaTeX math formulas, SMILES chemistry rendering, NTA question palette).

---

## 2. Platform & Framework Analysis

We evaluated the four primary engineering approaches against NAG's strict technical pillars:

| Evaluation Dimension | 1. Flutter (Dart) | 2. React Native + Expo (TS) | 3. Kotlin Multiplatform (KMP) | 4. Full Native (Kotlin + Swift) |
| :--- | :--- | :--- | :--- | :--- |
| **Code Sharing** | High (~90% UI & logic) | High (~85% UI & logic) | High Logic (~70%), Separate UIs | 0% Sharing |
| **NAG Web Ecosystem Parity** | Low (New Dart language, custom engine) | **Highest** (Shares React 19/TS, hooks, validation zod schemas, API types) | Moderate (Shares API contracts if using Ktor) | None (Duplicate effort) |
| **Kiosk / Lockdown & OS APIs** | High (via MethodChannels) | High (Native Modules / TurboModules) | **Highest** (Direct Kotlin & Swift native interop) | **Maximum** (Direct API access) |
| **Math & Chemistry Rendering (KaTeX / SMILES)** | Requires WebView or custom Canvas parser | High (`react-native-render-html` or shared WebView bridge) | Native WebViews per platform | Native WebViews per platform |
| **Binary Size & Cold Startup** | ~18-25 MB (Impeller/Skia engine) | ~15-22 MB (Hermes engine + ProGuard) | **Smallest** (~8-12 MB binary) | **Smallest** (~8-12 MB binary) |
| **Hardware Keystore / Biometrics** | Strong (via plugins) | Strong (LocalAuthentication + Keychain/Keystore TurboModules) | Native direct integration | Native direct integration |
| **Talent Pool & Development Speed** | Moderate | **Fastest** (Leverages existing web team's React/TypeScript expertise) | Moderate (Requires Swift + Kotlin devs) | Slowest (Requires 2 separate teams) |

---

## 3. Decision: Hybrid React Native (with Expo EAS / TurboModules)

### **Primary Framework: React Native with New Architecture (Hermes Engine & Bridgeless Mode)**

#### Rationale:
1. **Developer Velocity & Logic Reuse**:
   - The NAG frontend team already operates on React 19, TypeScript, Tailwind, and React Hook Form with Zod schemas.
   - Core domain logic (e.g., CBT auto-save pipeline, offline revision sequencing, NTA status palette state machine, API clients) can be extracted into a monorepo workspace package (`@nag/candidate-core`) and shared 1:1 between `candidate-frontend` and `candidate-mobile`.
2. **Hermes Bytecode Engine**:
   - Fast TTI (Time to Interactive) < 1.2s on budget Android devices.
   - Low memory footprint (~35MB baseline RAM) critical for budget devices during long 3-hour exam sessions.
3. **Bridgeless TurboModules for Security & Kiosk**:
   - For high-consequence kiosk delivery (Android `LockTaskMode`, iOS `AutonomousSingleAppMode`, camera proctoring, StrongBox Keystore signatures), native C++/Kotlin/Swift TurboModules provide zero-latency access to low-level hardware security APIs without the legacy JS bridge overhead.

---

## 4. Architectural Blueprint for `candidate-mobile`

### 4.1 System Components
```
[ candidate-mobile (React Native + Expo) ]
  ├── Presentation Layer (Tailwind/NativeWind, Lucide Icons, KaTeX WebView)
  │     ├── Auth & Registration (OTP, Aadhaar/DigiLocker)
  │     ├── Admit Card & Offline Wallet (Offline QR + Cryptographic Pass)
  │     ├── Exam Catalog & Application Multi-Step Form
  │     └── Mock Test & Practice CBT Engine
  │
  ├── Core Shared Logic (@nag/candidate-core)
  │     ├── API Client (Axios/Ky + JWT Interceptor with SecureStore)
  │     ├── Exam Application Validation (Zod Schemas)
  │     ├── Question State Machine (NTA 5-status palette)
  │     └── Offline Buffering & Sync Queue (WatermelonDB / SQLite)
  │
  └── Native Platform Security Layer (TurboModules)
        ├── Kiosk Controller (Android LockTask / iOS Guided Access)
        ├── Hardware Cryptography (Android StrongBox Keystore / iOS Secure Enclave)
        └── Proctoring Guard (Screen capture detection, overlay suppression)
```

### 4.2 Data Persistence & Offline Resilience
- **Encrypted Local Storage**: `expo-secure-store` / `react-native-keychain` for JWT refresh tokens and candidate private keys.
- **Offline Structured Database**: SQLite via `expo-sqlite` or `WatermelonDB` for offline question bundles, encrypted admit cards, and cached test submissions.
- **Sync Protocol**: Incremental event-driven batch upload with exponential backoff when connectivity resumes.

---

## 5. Implementation Phases & Roadmap

### **Phase 1: Foundation, Auth & Hall Ticket Wallet (v0.8-alpha)**
- Initialize React Native project (`apps/candidate-mobile` in monorepo).
- Implement Candidate Self-Registration, Mobile OTP verification, and DigiLocker / Aadhaar profile binding.
- Offline Admit Card download with cryptographically signed QR code verification (operable with zero internet at exam center gates).
- Push notifications via Firebase Cloud Messaging (FCM) & Apple APNs for application deadlines, centre allocations, and shift schedules.

### **Phase 2: Exam Application & Practice Testing (v0.8-beta)**
- Multi-step exam application with 3 centre preferences, shift preferences, and PwD/Scribe assistance requests.
- Full offline mock examination / CBT practice engine:
  - Multi-section timer and question navigation palette.
  - KaTeX mathematical formula and SMILES chemical structure rendering.
  - Local response grading and instant performance analytics.

### **Phase 3: High-Security Kiosk Delivery & Proctoring Companion (v0.9)**
- Native TurboModules for Android Lock Task Mode (Device Owner / Screen Pinning) and iOS Single App Mode.
- Screen recording and screenshot suppression (`FLAG_SECURE` on Android, UIScreen capture listener on iOS).
- On-device lightweight anomaly detection (head movement, background audio spikes, dual face detection).

---

## 6. Consequences & Trade-offs

### Positive:
- **Unified TypeScript Ecosystem**: Enables 80%+ code reuse for business rules, validation, and API contracts between web and mobile.
- **Fast Candidate Adoption**: Single codebase serves both Android (95%+ of Indian student base) and iOS.
- **Reliable Offline-First**: Native SQLite + SecureStore ensures admit cards and practice tests work without connectivity.

### Risks & Mitigations:
- **Risk**: Native kiosk lockdowns and deep hardware access may vary across heavily skinned Android OSs (MIUI, ColorOS, FuntouchOS).
  - *Mitigation*: Fall back gracefully: for practice exams, use soft-lockdown with app-switch penalties; for official kiosk centers, deploy via standard MDM (Device Owner mode) or dedicated test center hardware.
- **Risk**: Complex math/chemistry rendering in React Native can be sluggish.
  - *Mitigation*: Use high-performance offscreen Canvas / lightweight Skia rendering or cached SVG pre-compilation for KaTeX and SMILES structures.
