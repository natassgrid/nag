# Open Digital Public Infrastructure (DPI) Platform - Requirements Specification

## Overview
Enterprise-scale examination platform for Indian Government examinations such as UPSC, SSC, IBPS, RRB, NTA, State PSCs, Banking, Defence, Universities, and Recruitment Boards.

---

# 1. Question Bank Management System (QBMS)

## Functional Requirements

### Question Classification
- Subject
- Topic
- Sub-topic
- Chapter
- Learning Outcome / Competency
- Exam Type Mapping
- Difficulty Level (Easy, Medium, Hard, Very Hard)
- Cognitive Level (Recall, Understanding, Application, Analysis)
- Question Type
  - Single Correct MCQ
  - Multi Correct MCQ
  - Numerical Answer
  - Match Following
  - Assertion Reason
  - Matrix Match
  - Comprehension Based
  - Case Study Based
  - Descriptive
  - Coding Question

### Multi-Language Support
- Support all 22+ Indian languages
- Unicode UTF-8 storage
- Independent language versions
- Translation workflow and approval process

### Question Content
- Question ID
- Version ID
- Language ID
- Question Text
- Instructions
- Marks
- Negative Marks
- Recommended Time

### Option Support
- Unique Option UUID
- Up to 10 options
- Single or multiple correct answers
- Client-side randomization
- Option-specific explanations
- Language-specific option content

### Rich Content Support
- HTML5
- SVG
- PNG
- JPEG
- WEBP
- LaTeX
- MathML
- Scientific symbols
- Graphs
- Geometry figures
- Chemical structures
- Circuit diagrams

### Question Security
- AES-256 encryption at rest
- Per-question encryption keys
- HSM-backed key management
- Key rotation
- Encrypted backups

### Metadata
- Author
- Reviewer
- Approver
- Creation Date
- Last Modified Date
- Usage Count
- Exposure Count
- Last Used Date

### Lifecycle
Draft -> Review -> SME Approval -> Language Validation -> Security Approval -> Published -> Archived

### Advanced Features
- Duplicate detection
- AI similarity detection
- Exposure management
- Reuse control (1 year, 2 years, never)

---

# 2. Candidate Registration System

## Candidate Profile
### Identity
- Aadhaar
- PAN
- Passport
- Driving License
- Voter ID

### Personal Information
- Name
- DOB
- Gender
- Nationality
- Category

### Contact
- Mobile Number
- Email

### Address
- Permanent Address
- Correspondence Address

### Education
- Class X
- Class XII
- Graduation
- Post Graduation
- Certifications

### Reservation
- SC/ST/OBC
- EWS
- PwD
- Ex-Serviceman

### Verification
- OTP Verification
- Aadhaar Verification
- Face Verification
- DigiLocker Integration
- Document Verification

---

# 3. Examination Configuration Engine

## Examination Setup
- Exam Name
- Recruitment Cycle
- Duration
- Total Marks
- Negative Marking
- Sectional Cutoff
- Overall Cutoff

## Section Configuration
- Subject-wise distribution
- Topic-wise distribution
- Marks allocation

## Language Configuration
- English
- Hindi
- Regional Languages
- Multi-language support

## Candidate Controls
- Allowed browsers
- Allowed devices
- Full-screen enforcement
- Camera requirements
- Remote proctoring settings

---

# 4. Rule-Based Question Paper Generator

## Blueprint Rules
- Subject-wise ratios
- Topic-wise ratios
- Difficulty ratios
- Language distribution
- New vs old question ratios

## Constraints
- Question reuse restrictions
- Exposure control
- Exam-specific eligibility rules

## Multi-Shift Support
- Multiple papers per examination
- Equal difficulty across shifts
- Equal topic distribution
- Statistical balancing

## Statistical Validation
- Difficulty Index
- Discrimination Index
- Reliability Index
- Question Weighting

---

# 5. Secure Examination Delivery

## Paper Security
- Store only question IDs in paper definition
- Encrypted exam packages
- Digital signatures
- Shift-based encryption keys

## Decryption Controls
- Decrypt only during examination
- Client-side controlled rendering
- Server authorization required

## Security Controls
### Browser Security
- Disable copy/paste
- Disable print
- Restrict developer tools
- Watermarking

### Device Controls
- Device fingerprinting
- OS validation
- Browser validation

### Location Controls
- IP validation
- Exam center validation
- GPS validation (where permitted)

---

# 6. Response Management

## Response Capture
- Question ID
- Selected options
- Timestamps
- Time spent
- Revision history

## Auto Save
- Every few seconds
- On navigation
- On answer modification

## Audit Trail
- Login
- Logout
- Tab switch
- Network interruption
- Answer changes

---

# 7. Evaluation System

## Evaluation Types
- Objective evaluation
- Descriptive evaluation
- Hybrid evaluation

## Scoring Rules
- Positive marks
- Negative marks
- Partial marks
- Multi-correct scoring

---

# 8. Results and Scorecards

## Outputs
- Scorecard
- Detailed Scorecard
- Rank
- Percentile
- Subject-wise Analysis
- Topic-wise Analysis

## Distribution
- Portal download
- Email delivery
- DigiLocker integration

## PDF Protection
- Aadhaar + DOB
- Candidate ID + DOB
- Random password

---

# 9. Audit and Compliance

## Auditable Activities
- Question creation
- Question modification
- Approvals
- Paper generation
- Exam execution
- Evaluation
- Result publication

## Compliance
- Immutable audit logs
- WORM storage
- Digital signatures
- Legal evidence retention

---

# 10. Roles and Access Control

## Roles
- Super Admin
- Security Admin
- Question Author
- Reviewer
- Approver
- Exam Controller
- Evaluation Officer
- Auditor

## Access Model
- RBAC
- ABAC
- Least privilege principle

---

# Security Requirements

## Authentication
- MFA
- FIDO2/WebAuthn
- Hardware token support

## Encryption
- AES-256 at rest
- TLS 1.3 in transit

## Key Management
- HSM integration
- Key rotation
- Key escrow policies

## Monitoring
- SIEM integration
- Threat detection
- Insider threat monitoring
- Real-time alerts

## Architecture
- Zero Trust Architecture
- Network segmentation
- Secure secrets management

---

# Non-Functional Requirements

## Scalability
- 5 Million registered users
- 500,000 concurrent candidates
- 100 Million question repository

## Availability
- 99.99% uptime
- No single point of failure
- Multi-region deployment

## Performance
- Login < 2 seconds
- Question load < 500 ms
- Response save < 200 ms

## Reliability
- RPO = 0
- RTO < 15 minutes

## Durability
- 99.999999999% response durability

## Disaster Recovery
- Active-Active DR
- Geo-redundant deployment

## Observability
- Metrics
- Logs
- Traces
- Security telemetry

---

# Additional Government Examination Features

- Biometric attendance
- Face recognition
- CCTV integration
- AI cheating detection
- Shift normalization
- Percentile calculation
- Question leakage detection
- Dual approval workflows
- Four-eyes principle
- Emergency paper replacement
- Offline center synchronization
- Bulk admit card generation
- Candidate grievance management
- Court evidence preservation
- Long-term archival retention

---

# Conclusion

This specification targets a highly secure, enterprise-grade, government-scale examination platform capable of supporting national-level examinations with strong security, compliance, scalability, auditability, and operational resilience.
