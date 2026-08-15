
# Open Digital Public Infrastructure (DPI) Platform
# Enterprise Software Requirements Specification (SRS) v2

Version: 2.0
Status: Community Draft
License: Apache 2.0 Recommended

---

# Document Control

## Purpose

This document defines the enterprise-grade requirements for a large-scale, open-source examination platform capable of supporting national, state, university, certification, and recruitment examinations.

Target scale includes:
- National Testing Agencies
- Public Service Commissions
- Banking Recruitment
- Railway Recruitment
- Universities
- Certification Bodies
- Corporate Assessments

---

# 1. Vision

Create the world's most secure, transparent, auditable, multilingual, open-source examination platform.

Core principles:

- Security First
- Privacy by Design
- Cloud Native
- Open Standards
- Vendor Neutral
- Massive Scale
- Accessibility
- Observability
- Extensibility

---

# 2. Business Goals

## BG-01

Support high-stakes examinations.

## BG-02

Reduce examination fraud.

## BG-03

Provide verifiable auditability.

## BG-04

Support all Indian languages.

## BG-05

Enable sovereign deployment.

## BG-06

Provide reusable open-source ecosystem.

---

# 3. Stakeholders

## Examination Authority

Creates and controls examinations.

## Candidate

Appears for examinations.

## Question Author

Creates content.

## Reviewer

Reviews content.

## Translator

Creates language variants.

## Exam Controller

Creates papers.

## Evaluator

Evaluates answers.

## Auditor

Audits activities.

## Security Administrator

Controls keys and security.

---

# 4. System Context

Major domains:

1. Identity Management
2. Candidate Management
3. Question Bank
4. Translation Management
5. Examination Management
6. Paper Generation
7. Exam Delivery
8. Proctoring
9. Response Management
10. Evaluation
11. Result Processing
12. Analytics
13. Audit
14. Security
15. Administration

---

# 5. Functional Requirements

## Identity and Access Management

### Registration

Candidate registration shall support:

- Aadhaar
- PAN
- Passport
- Voter ID
- Driving License

### Authentication

Support:

- Username Password
- MFA
- OTP
- WebAuthn
- FIDO2

### Authorization

Support:

- RBAC
- ABAC
- Fine-grained permissions

### Session Controls

- Device binding
- Concurrent session control
- Risk-based authentication

---

## Candidate Management

### Profile

Store:

- Personal details
- Contact details
- Education
- Category
- Reservation information

### Verification

Support:

- OTP verification
- DigiLocker
- Face verification
- Biometric verification

---

## Question Bank Management

### Question Types

- Single Correct MCQ
- Multiple Correct MCQ
- Numerical
- Descriptive
- Matrix Match
- Assertion Reason
- Coding Question
- Case Study

### Metadata

- Subject
- Topic
- Subtopic
- Chapter
- Learning Outcome
- Difficulty
- Exam Mapping

### Difficulty Levels

- Easy
- Medium
- Hard
- Very Hard

### Rich Content

Support:

- HTML5
- SVG
- PNG
- JPEG
- WEBP
- Audio
- Video
- LaTeX
- MathML

### Question Lifecycle

Draft
Review
Approved
Published
Archived

### Exposure Tracking

Track:

- Usage count
- Last usage
- Exam usage history
- Shift usage history

### Reuse Policy

Configurable:

- Never
- 1 year
- 2 years
- Custom

---

## Translation Management

### Supported Languages

All scheduled Indian languages.

### Translation Workflow

Author
Translator
Reviewer
Approver

### Consistency Validation

Language variants must maintain semantic equivalence.

---

## Examination Management

### Exam Definition

- Name
- Duration
- Marks
- Sections
- Negative marking

### Section Definition

- Questions
- Marks
- Time limits

### Rules

- Calculator policy
- Navigation policy
- Review policy

---

## Question Paper Generation

### Blueprint Engine

Supports:

- Subject ratios
- Topic ratios
- Difficulty ratios
- Cognitive level ratios

### Fairness Requirements

All generated papers shall remain statistically comparable.

### Shift Management

Support unlimited shifts.

### Validation

Generate:

- Difficulty score
- Distribution report
- Similarity report

---

## Exam Delivery

### Modes

- Online
- Offline
- Hybrid

### Rendering

- One question at a time
- Batch mode
- Section mode

### Navigation

- Sequential
- Flexible
- Restricted

### Offline Centers

Support encrypted offline delivery.

---

## Proctoring

### Live Proctoring

- Camera
- Microphone
- Screen capture

### AI Proctoring

- Face detection
- Multiple face detection
- Object detection
- Suspicious activity detection

---

## Response Management

### Capture

- Responses
- Timestamps
- Revisions
- Time spent

### Auto Save

Maximum data loss:

0 responses

---

## Evaluation

### Automatic

MCQ
Numerical

### Manual

Descriptive

### Hybrid

Configurable workflows.

---

## Result Processing

### Outputs

- Score
- Rank
- Percentile
- Normalized score

### Delivery

- Portal
- PDF
- Email
- API

---

# 6. User Stories

## Candidate

As a candidate
I want to appear for an examination
So that I can obtain a score.

## Author

As a question author
I want to create questions
So that examinations can be prepared.

## Exam Controller

As an exam controller
I want to generate statistically balanced papers
So that fairness is maintained.

## Auditor

As an auditor
I want immutable audit logs
So that investigations can be performed.

---

# 7. Security Requirements

## Zero Trust

All services must authenticate and authorize every request.

## Encryption

### Data at Rest

AES-256

### Data in Transit

TLS 1.3

### Backup Encryption

Mandatory

## Key Management

- HSM integration
- Key rotation
- Key revocation

## Audit

Every privileged action shall be audited.

## Threat Protection

- Brute force protection
- DDoS protection
- WAF integration
- Rate limiting

---

# 8. Compliance Requirements

Support alignment with:

- DPDP Act
- ISO 27001
- OWASP ASVS
- NIST CSF
- SOC2

---

# 9. Data Model Requirements

Core entities:

- Candidate
- Question
- QuestionVersion
- Translation
- Examination
- Paper
- PaperQuestion
- Response
- Evaluation
- Result
- AuditEvent

---

# 10. API Requirements

All APIs:

- REST compliant
- OpenAPI documented
- Versioned

Security:

- OAuth2
- OIDC
- JWT

---

# 11. Architecture Requirements

## Architectural Style

Microservices.

## Core Services

- Identity Service
- Candidate Service
- Question Bank Service
- Translation Service
- Examination Service
- Paper Generator Service
- Delivery Service
- Evaluation Service
- Result Service
- Notification Service
- Audit Service

---

# 12. Database Requirements

Primary Database:

PostgreSQL

Requirements:

- Partitioning
- Replication
- PITR
- Encryption

---

# 13. Search Requirements

Support:

- Full text search
- Subject search
- Topic search
- Similarity search

Recommended:

OpenSearch

---

# 14. Messaging Requirements

Recommended:

Kafka

Capabilities:

- Event streaming
- Audit event publishing
- Notification events

---

# 15. Observability Requirements

## Logging

Structured JSON logging.

## Metrics

Prometheus.

## Tracing

OpenTelemetry.

## Dashboards

Grafana.

---

# 16. Performance Requirements

Login:
< 2 seconds

Question load:
< 500 ms

Response save:
< 200 ms

Paper generation:
< 5 minutes

---

# 17. Scalability Requirements

Registered users:
5 million

Concurrent candidates:
500,000

Question bank:
100 million questions

Daily responses:
500 million

---

# 18. Availability Requirements

Availability:
99.99%

No single point of failure.

---

# 19. Reliability Requirements

RPO:
0

RTO:
15 minutes

---

# 20. Disaster Recovery

Active-active deployment.

Multi-region replication.

Automated failover.

---

# 21. Accessibility

WCAG 2.2 AA.

Support:

- Keyboard navigation
- Screen readers
- High contrast mode

---

# 22. Mobile Requirements

Responsive web application.

Future native support:

- Android
- iOS

---

# 23. DevSecOps Requirements

CI/CD mandatory.

Pipeline stages:

- Build
- Test
- Security Scan
- SAST
- DAST
- Deployment

---

# 24. Audit Requirements

Audit events:

- Login
- Logout
- Question creation
- Question modification
- Paper generation
- Result publication

Audit records shall be immutable.

---

# 25. Open Source Governance

## Repository Structure

/frontend
/backend
/services
/docs
/infrastructure

## Contribution Model

- RFCs
- Pull Requests
- Security Reviews

---

# 26. Roadmap

Phase 1
Core platform

Phase 2
Government scale

Phase 3
AI proctoring

Phase 4
Offline exam centers

Phase 5
Internationalization

---

# 27. Acceptance Criteria

System shall:

- Support 500K concurrent candidates
- Support all Indian languages
- Prevent unauthorized paper access
- Maintain immutable audits
- Recover within RTO/RPO targets

---

# 28. Future Extensions

- Adaptive testing
- AI-generated questions
- AI translation validation
- Digital credentialing
- Blockchain-based verification

---

End of Enterprise SRS v2
