# NAG Roadmap — NextGen Assessment Grid

> Building the world's most trusted open Digital Public Infrastructure (DPI) for assessments.

---

# Vision

Deliver an enterprise-grade, cloud-native, secure, sovereign AI-ready assessment platform that governments, public service commissions, universities, certification bodies, and enterprises can deploy at national scale without vendor lock-in.

---

# Milestone Overview

| Milestone | Target Scope | Status |
|---|---|---|
| **Version 0.1** | Foundation & Tri-Mode Architecture | ✅ **Completed** |
| **Version 0.2** | Core Examination Management & Scheduling | ✅ **Completed** |
| **Version 0.3** | Item Authoring, Taxonomy & Question Bank | ✅ **Completed** |
| **Version 0.4** | High-Concurrency CBT Examination Delivery | ✅ **Completed** |
| **Version 0.5** | Automated Evaluation, Scoring & Results | ✅ **Completed** |
| **Version 0.6** | Zero-Trust Cryptography, Time-Lock & Audit | ✅ **Completed** |
| **Version 0.7** | Sovereign AI Platform & Question Generation | 🔄 **Active (Phase 1–5 Delivered)** |
| **Version 0.8** | Mobile Candidate Platform (Android & iOS) | 🚧 **In Progress (ADR 0008)** |
| **Version 1.0** | National Scale Hardening & High Availability | 🎯 **Target (1M Concurrent)** |

---

# Version 0.1 — Foundation & Tri-Mode Architecture
- [x] Multi-module repository structure with unified codebase
- [x] Tri-Mode Deployment Topologies (Microservices, Macro-services, and Single-JVM Monolith)
- [x] Pluggable messaging abstraction (`KafkaEventPublisher`, `RabbitEventPublisher`, `SpringEventPublisher`)
- [x] Multi-schema isolated PostgreSQL datastores with Flyway migrations
- [x] Keycloak Identity & Access Management (OIDC/OAuth2, RBAC, JWT validation)
- [x] API Gateway with global rate limiting, tenant header resolution, and TLS termination
- [x] Docker Compose deployment automation scripts (`redeploy-micro.sh`, `redeploy-macro.sh`, `redeploy-monolith.sh`)

**Status:** ✅ **Completed**

---

# Version 0.2 — Core Examination Management & Scheduling
- [x] Examination lifecycle management (Creation, Draft, Review, Published, Archived)
- [x] Versioned schedule management and multi-shift configuration
- [x] Examination center registry, test rooms, and capacity management
- [x] Automated candidate seat and test center allocation algorithms
- [x] Candidate profile registration, eligibility verification, and DigiLocker integration
- [x] Multi-channel notification engine (Email, SMS, Webhook, and Server-Sent Events)

**Status:** ✅ **Completed**

---

# Version 0.3 — Item Authoring, Taxonomy & Question Bank
- [x] Rich text question authoring with Quill editor and SVG diagrams
- [x] KaTeX mathematical formula rendering and `mhchem` chemical equation support (`\ce{...}`)
- [x] Relational taxonomy hierarchy (`Subject` $\rightarrow$ `Topic` $\rightarrow$ `Subtopic`) with strict ID-based resolution
- [x] Question lifecycle Finite State Machine (`DRAFT` $\rightarrow$ `REVIEW` $\rightarrow$ `APPROVED` / `REJECTED` $\rightarrow$ `PUBLISHED` $\rightarrow$ `RETIRED`)
- [x] Immutable append-only question version history (`question_version`) with snapshot diffing
- [x] Asset Service integration for multimedia uploads (images, SVGs, audio, video)
- [x] Multi-set blueprint rule matching for balanced exam paper assembly

**Status:** ✅ **Completed**

---

# Version 0.4 — High-Concurrency Examination Delivery
- [x] High-throughput candidate CBT test delivery engine (`delivery-service`)
- [x] Real-time response heartbeat ingestion and answer bundling (`response-service`)
- [x] Local offline response buffering (IndexedDB) with automatic reconnect synchronization
- [x] Monotonic packet sequence verification and tamper-evident SHA-256 HMAC signing
- [x] Candidate session recovery across network disconnects and terminal restarts
- [x] Dynamic question palette with NTA/GATE exam navigation standards

**Status:** ✅ **Completed**

---

# Version 0.5 — Automated Evaluation, Scoring & Results
- [x] Automated objective grading engine for single and multiple-choice questions
- [x] Anonymized rubric-based subjective scoring workflow for descriptive items
- [x] Multi-shift score normalization (Percentile, Z-score, and Equi-percentile algorithms)
- [x] Automated merit list generation and category-wise reservation ranking
- [x] Cryptographically verifiable digital scorecards with signed QR codes

**Status:** ✅ **Completed**

---

# Version 0.6 — Zero-Trust Cryptography & Security
- [x] Automated question set randomization (Set A, Set B, Set C)
- [x] Paper package envelope encryption using AES-256-GCM
- [x] Shamir's Secret Sharing (3-of-5 threshold) ephemeral key splitting for time-locked unlocking
- [x] HashiCorp Vault and AWS KMS integration for envelope key protection
- [x] Immutable, append-only hash-chained audit logging (`audit-service`) for legal compliance
- [x] Strict Row-Level Security (RLS) and tenant data isolation in PostgreSQL

**Status:** ✅ **Completed**

---

# Version 0.7 — Sovereign AI Platform & Question Generation

- [x] **Spring AI Integration**: Decoupled LLM gateway via LiteLLM supporting Ollama (Qwen3, Llama 3) and AWS Bedrock (Nova Micro, Nova Lite)
- [x] **Intelligent Dual-Path Pipeline**:
  - **Single-Model Fast Path**: Sub-2-second response latency for low-complexity items
  - **ComplexityEvaluator**: "Agent Only When Needed" conditional triage based on cognitive demand, question types, and sample analysis
- [x] **5-Agent Collaborative Committee**:
  - `RequirementAnalystAgent`: Generation blueprint synthesis and prompt elicitation
  - `QuestionAuthorAgent`: Few-shot demonstrations and RAG context assembly
  - `PsychometricCriticAgent`: Distractor plausibility, sole correct answer, and Bloom's alignment
  - `RefinementAgent`: Single-cycle issue resolution
  - `SimilarityAuditorAgent`: Cosine similarity audit against existing items (<0.85 threshold)
- [x] **Tiered Multimodal Document Ingestion** (`SampleDocumentParserService`):
  - Tier 0: Local PDFBox text stream extraction ($0.00 vision token spend)
  - Tier 1: Embedded raster image isolation (`PDImageXObject`) with downsampling (max 1024px)
  - Tier 2: Scanned document rasterization at 150 DPI (75% token/memory reduction)
- [x] **Pre-Generation RAG Deduplication**:
  - 768-dimensional `nomic-embed-text` embeddings stored in PostgreSQL `pgvector`
  - Top-5 similar items retrieved prior to generation as negative constraints
- [x] **KaTeX & Chemistry Syntax Normalization**:
  - Delimiter normalizer converting `\(` and `\[` to standard `$$...$$`
  - `\ce{...}` and `\pu{...}` support for chemistry equations and physical units
- [x] **Asynchronous Batch Generation**: AWS Bedrock Batch Inference (`BedrockBatchService`) for bulk generation
- [x] **Neural Machine Translation**: Asynchronous multi-language translation via IndicTrans2
- [ ] **AI-Powered Proctoring**: On-device anomaly detection, face orientation analysis, and gaze tracking
- [ ] **Adaptive Formative Assessment**: Dynamic difficulty adjustment based on live candidate psychometric performance

**Status:** 🔄 **Active Development (Core Generation Engine Completed)**

---

# Version 0.8 — Mobile Candidate Platform (Android & iOS)

- [x] Mobile architecture evaluation and technology selection ([ADR 0008](docs/adr/0008-mobile-app-architecture-and-platform-selection.md))
- [ ] Native candidate self-service application (React Native / Expo with shared contracts)
- [ ] Offline Digital Hall Ticket / Admit Card Wallet with cryptographic QR verification
- [ ] Low-bandwidth application submission and examination center preference selection
- [ ] Real-time push notifications (Shift schedules, gate reporting times, venue navigation)
- [ ] Offline practice / mock test runtime with KaTeX math and SMILES chemistry support
- [ ] Kiosk / Single-App Lockdown Mode and mobile proctoring guard

**Status:** 🚧 **Planned / Architecture Approved**

---

# Version 1.0 — Production Ready & National Scale

- [ ] Production deployment on multi-region Kubernetes clusters (EKS / GKE / OpenShift)
- [ ] High-Availability multi-node PostgreSQL with read-replica failover
- [ ] Multi-region disaster recovery runbooks and automated database failover
- [ ] Comprehensive accessibility compliance (WCAG 2.1 AA / GIGW certification)
- [ ] Enterprise performance benchmarking:
  - **1,000,000+** Concurrent candidate test sessions
  - **10,000,000+** Registered candidates
  - **99.99%** Uptime SLA during active examination shifts

**Status:** 🎯 **Target**

---

# Future Horizons

- **W3C Verifiable Credentials**: Tamper-proof, cryptographically signed digital diplomas and certificates
- **Computer Adaptive Testing (CAT)**: Item Response Theory (IRT) with live Bayesian difficulty adaptation
- **Cross-Border Multilingual Expansion**: Support for global examination frameworks and localized dialects
- **Decentralized Audit Proofs**: Zero-knowledge cryptographic proofs for transparent public verification
- **Ecosystem Plugin Marketplace**: Extensible question types, custom proctoring engines, and third-party evaluation rubrics
