# Open Digital Public Infrastructure (DPI) Platform
# Software Requirements Specification (SRS)

Version: 1.0
Status: Draft
License Target: Apache 2.0 / MIT
Project Type: Open Source

---

# 1. Introduction

## 1.1 Purpose

This document defines requirements for a highly secure, scalable, multilingual, open-source examination platform intended for:

- Government Recruitment Exams
- University Entrance Exams
- Certification Exams
- Public Sector Recruitment
- Banking Exams
- Railways Exams
- Defence Recruitment
- State and National Level Testing Agencies

## 1.2 Design Goals

- Open Source
- Cloud Native
- Vendor Neutral
- Multi Cloud
- Government Grade Security
- Massive Scale
- Accessibility Compliant
- Multilingual

---

# 2. System Scope

Major modules:

1. Identity & Registration
2. Candidate Management
3. Question Bank
4. Examination Configuration
5. Paper Generation Engine
6. Exam Delivery Platform
7. Proctoring
8. Evaluation
9. Results
10. Analytics
11. Security
12. Audit & Compliance
13. Administration

---

# 3. Stakeholders

## Internal

- Super Admin
- Security Admin
- Exam Controller
- Question Author
- Reviewer
- Translator
- Evaluator
- Auditor

## External

- Candidates
- Examination Authorities
- Universities
- Government Agencies

---

# 4. Functional Requirements

## FR-001 Identity Management

### Features

- Registration
- Login
- MFA
- Passwordless Login
- Aadhaar Integration
- DigiLocker Integration
- Face Verification
- Biometric Integration

### Candidate Profile

- Name
- DOB
- Gender
- Nationality
- Category
- Mobile
- Email
- Address
- Education History

---

## FR-002 Question Bank

### Metadata

- Subject
- Topic
- Sub Topic
- Chapter
- Difficulty
- Language
- Exam Mapping

### Question Types

- Single Correct MCQ
- Multi Correct MCQ
- Numerical
- Matrix Match
- Match Following
- Assertion Reason
- Descriptive
- Coding

### Rich Content

- Images
- SVG
- Audio
- Video
- LaTeX
- MathML
- Scientific Symbols

### Translation

Support all scheduled Indian languages.

### Workflow

Draft
→ Review
→ Approval
→ Publish
→ Archive

---

## FR-003 Question Security

### Requirements

- Encrypted storage
- Version history
- Digital signatures
- Exposure tracking
- Similarity detection

### Exposure Control

Question reuse configurable:

- Never
- 1 Year
- 2 Years
- Custom

---

## FR-004 Examination Configuration

### Exam Definition

- Name
- Duration
- Marks
- Sections
- Negative Marking

### Section Rules

- Question Count
- Marks
- Timing
- Topic Distribution

---

## FR-005 Question Paper Generation

### Blueprint Engine

Rules:

- Subject Ratio
- Topic Ratio
- Difficulty Ratio
- Language Ratio

### Multi Shift Support

- Shift A
- Shift B
- Shift C
- N Shifts

### Fairness Requirements

- Equivalent Difficulty
- Equivalent Distribution
- Statistical Validation

---

## FR-006 Exam Delivery

### Modes

- Online
- Hybrid
- Offline Sync

### Rendering

- One Question
- Multiple Questions
- Section Based

### Navigation

- Free Navigation
- Sequential Navigation
- Section Locking

---

## FR-007 Response Management

Store:

- Question ID
- Candidate Response
- Timestamp
- Time Spent
- Review Status

### Auto Save

- Every few seconds
- Navigation event
- Manual save

---

## FR-008 Evaluation

### Automatic Evaluation

MCQ and Numerical

### Manual Evaluation

Descriptive Answers

### Hybrid Evaluation

Automatic + Manual Review

---

## FR-009 Results

### Output

- Marks
- Rank
- Percentile
- Normalized Score

### Reports

- Subject Analysis
- Topic Analysis
- Difficulty Analysis

### Export

- PDF
- CSV
- API

---

## FR-010 Proctoring

### Live Monitoring

- Webcam
- Audio
- Screen Activity

### AI Monitoring

- Face Detection
- Multiple Face Detection
- Suspicious Behavior Detection

---

## FR-011 Analytics

### Dashboards

- Candidate Statistics
- Question Statistics
- Exam Statistics

### Question Analytics

- Difficulty Index
- Discrimination Index
- Reliability Metrics

---

# 5. Security Requirements

## Authentication

- MFA Mandatory
- WebAuthn
- FIDO2

## Authorization

- RBAC
- ABAC

## Encryption

### At Rest

AES-256

### In Transit

TLS 1.3

## Key Management

- HSM Support
- Key Rotation
- Key Escrow

## Audit

Immutable Audit Trail

## Insider Threat Controls

- Dual Approval
- Four Eyes Principle
- Segregation of Duties

---

# 6. Privacy Requirements

## PII Protection

- Data Classification
- Encryption
- Retention Policies

## Compliance

Support:

- DPDP Act (India)
- GDPR (Optional)
- ISO 27001
- SOC2 Alignment

---

# 7. Non Functional Requirements

## Availability

99.99%

## Reliability

RPO = 0

RTO < 15 Minutes

## Scalability

- 5 Million Registered Users
- 500K Concurrent Candidates
- 100 Million Questions

## Performance

Login < 2 Seconds

Question Load < 500 ms

Response Save < 200 ms

---

# 8. Accessibility

WCAG 2.2 AA

Support:

- Screen Readers
- Keyboard Navigation
- High Contrast Themes

---

# 9. Deployment Requirements

## Supported Models

- On Premise
- Private Cloud
- Public Cloud
- Hybrid Cloud

## Containerization

- Docker
- Kubernetes

## Database Support

- PostgreSQL
- MySQL

## Cache

- Redis

---

# 10. Observability

## Logging

- Structured Logs
- Audit Logs

## Metrics

- Prometheus Compatible

## Tracing

- OpenTelemetry

---

# 11. Disaster Recovery

## Backup

- Full Backup
- Incremental Backup

## Replication

- Multi Region
- Active Active

---

# 12. Open Source Governance

## Repository Structure

- frontend/
- backend/
- services/
- docs/
- infrastructure/

## Contribution Model

- RFC Process
- Pull Requests
- Security Review

## Licensing

Recommended:

- Apache 2.0
- MIT

---

# 13. Future Roadmap

Phase 1
- Core Examination Platform

Phase 2
- AI Proctoring

Phase 3
- Mobile Apps

Phase 4
- Offline Rural Exam Centers

Phase 5
- National Scale Deployment

---

# Appendix A - Suggested Microservices

- Identity Service
- Candidate Service
- Question Bank Service
- Translation Service
- Exam Service
- Paper Generation Service
- Delivery Service
- Response Service
- Evaluation Service
- Result Service
- Notification Service
- Audit Service
- Analytics Service
- Security Service

---

# Appendix B - Technology Recommendations

Backend:
- Java Spring Boot

Frontend:
- React

Database:
- PostgreSQL

Cache:
- Redis

Messaging:
- Kafka

Search:
- OpenSearch

Observability:
- Prometheus
- Grafana
- OpenTelemetry

Container:
- Kubernetes

Security:
- HashiCorp Vault
- HSM Integration

---

End of Document
