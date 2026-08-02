# Sequence Diagrams — National Assessment Grid

## 1. Overview

This document illustrates the core end-to-end sequence flows across NAG microservices for key workflows:
1. Schedule Creation & Approval Workflow Transition
2. Cryptographic Paper Generation & Package Locking
3. Candidate Answer Submission & Signature Verification

---

## 2. Schedule Creation & Workflow Transition Sequence

```mermaid
sequenceDiagram
    autonumber
    actor Controller as Exam Controller
    participant UI as Angular SPA
    participant GW as API Gateway
    participant SchedSvc as Scheduling Service
    participant DB as PostgreSQL DB
    participant Kafka as Kafka Event Bus

    Controller->>UI: Fill Schedule Form (Name, Exam Date, TimeZone)
    UI->>GW: POST /api/v1/examinations/{examId}/schedules
    GW->>SchedSvc: Forward with X-Tenant-Id & Authorization
    SchedSvc->>SchedSvc: Validate DTO & User Permissions
    SchedSvc->>DB: INSERT INTO schedules (status='DRAFT', version=1)
    DB-->>SchedSvc: Schedule Object Saved
    SchedSvc-->>GW: Return ScheduleResponse (201 Created)
    GW-->>UI: Display DRAFT Schedule

    Controller->>UI: Click "Transition" -> Select targetStatus="SCHEDULER_REVIEW"
    UI->>GW: PUT /api/v1/examinations/{examId}/schedules/{scheduleId}/transition
    GW->>SchedSvc: Forward Request DTO
    SchedSvc->>SchedSvc: ScheduleWorkflowEngine.validateStateTransition("DRAFT", "SCHEDULER_REVIEW")
    SchedSvc->>DB: UPDATE schedules SET status='SCHEDULER_REVIEW'
    SchedSvc->>Kafka: Publish ScheduleTransitionEvent
    SchedSvc-->>GW: Return Updated ScheduleResponse
    GW-->>UI: Update Status Chip to SCHEDULER_REVIEW
```

---

## 3. Cryptographic Paper Generation & Distribution

```mermaid
sequenceDiagram
    autonumber
    actor Controller as Exam Controller
    participant UI as Angular SPA
    participant GW as API Gateway
    participant PaperSvc as Paper Generation Service
    participant QuestSvc as Question Bank Service
    participant HSM as KMS / HSM
    participant DB as PostgreSQL DB

    Controller->>UI: Click "Generate & Lock Exam Paper Package"
    UI->>GW: POST /api/v1/examinations/{examId}/papers/generate
    GW->>PaperSvc: Forward Request
    PaperSvc->>QuestSvc: GET /api/v1/questions?topic=&difficulty=
    QuestSvc-->>PaperSvc: Return Question Pool Metadata & Encrypted Bodies
    PaperSvc->>PaperSvc: Assemble Question Sets (Set A, Set B, Set C)
    PaperSvc->>HSM: Request Ephemeral AES-256 Package Key
    HSM-->>PaperSvc: Return Key & Split Shamir Shares (3-of-5)
    PaperSvc->>PaperSvc: Encrypt Package (AES-GCM-256)
    PaperSvc->>DB: Store Encrypted Package & Share Hashes
    PaperSvc-->>GW: Paper Generation Completed (Status: SEALED)
    GW-->>UI: Display Sealed Package Manifest
```

---

## 4. Candidate Answer Submission & Audit Signing

```mermaid
sequenceDiagram
    autonumber
    actor Candidate as Candidate
    participant Terminal as Exam Terminal / App
    participant GW as API Gateway
    participant DeliverySvc as Exam Delivery Service
    participant Redis as Redis Cache
    participant DB as PostgreSQL DB
    participant Kafka as Kafka Event Bus

    Candidate->>Terminal: Click "Submit Exam Response"
    Terminal->>Terminal: Compute Payload Hash: SHA-256(Responses + Timestamp)
    Terminal->>GW: POST /api/v1/examinations/{examId}/delivery/submit
    GW->>DeliverySvc: Forward Answer Bundle & Signature Header
    DeliverySvc->>DeliverySvc: Verify Candidate Session & Payload Signature
    DeliverySvc->>Redis: Cache Submission Receipt
    DeliverySvc->>DB: INSERT INTO candidate_responses (Status='SUBMITTED')
    DeliverySvc->>Kafka: Publish AnswerSubmittedEvent (Audit Stream)
    DeliverySvc-->>GW: Return Submission Acknowledgment (HTTP 200)
    GW-->>Terminal: Show Confirmation Screen & Hash Receipt
```
