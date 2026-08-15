# Requirements Document

## Introduction

The Open Digital Public Infrastructure (DPI) Platform is a large-scale, secure, multilingual, cloud-native examination system designed to support national, state, university, certification, and recruitment examinations across India. It is built on Java Spring Boot with PostgreSQL, an Angular frontend, Docker-based development, and a vendor-neutral deployment model.

The platform serves Examination Authorities, Candidates, Question Authors, Reviewers, Translators, Exam Controllers, Evaluators, Auditors, and Security Administrators. It targets 500,000 concurrent candidates, 5 million registered users, and a question bank of 100 million questions with 99.99% availability.

Core principles are: Security First, Privacy by Design, Open Standards, Vendor Neutral, Massive Scale, Accessibility, Observability, and Extensibility.

---

## Glossary

- **Platform**: The Open Digital Public Infrastructure (DPI) Platform as a whole.
- **Identity_Service**: The Spring Boot microservice responsible for authentication, authorization, and session management.
- **Candidate_Service**: The Spring Boot microservice managing candidate profiles and verification.
- **Question_Bank_Service**: The Spring Boot microservice managing question creation, versioning, and lifecycle.
- **Translation_Service**: The Spring Boot microservice managing multi-language content and translation workflows.
- **Examination_Service**: The Spring Boot microservice managing exam definitions, sections, and rules.
- **Paper_Generator**: The Spring Boot microservice that assembles question papers from blueprints.
- **Delivery_Service**: The Spring Boot microservice that serves exam content to candidates at runtime.
- **Response_Service**: The Spring Boot microservice that captures and persists candidate responses.
- **Evaluation_Service**: The Spring Boot microservice that evaluates responses and computes scores.
- **Result_Service**: The Spring Boot microservice that calculates and publishes results.
- **Notification_Service**: The Spring Boot microservice that delivers notifications via email and push channels.
- **Audit_Service**: The Spring Boot microservice that records and exposes immutable audit events.
- **Angular_App**: The Angular single-page application used by all user roles via a browser.
- **PostgreSQL_DB**: The primary relational data store (PostgreSQL) shared across services via separate schemas.
- **Blueprint**: A structured specification defining the subject, topic, difficulty, and cognitive level distribution for a question paper.
- **Shift**: A scheduled time slot in which a specific paper variant is administered to a group of candidates.
- **RBAC**: Role-Based Access Control — access decisions based on assigned user roles.
- **ABAC**: Attribute-Based Access Control — access decisions based on user and resource attributes.
- **MFA**: Multi-Factor Authentication — requiring two or more verification factors.
- **WebAuthn**: Web Authentication API standard for passwordless and hardware-key authentication.
- **FIDO2**: Fast Identity Online 2 standard encompassing WebAuthn and CTAP protocols.
- **HSM**: Hardware Security Module — a physical device for secure cryptographic key management.
- **EARS**: Easy Approach to Requirements Syntax — the pattern language used for all acceptance criteria.
- **PII**: Personally Identifiable Information.
- **DPDP**: Digital Personal Data Protection Act (India).
- **WCAG**: Web Content Accessibility Guidelines.
- **RPO**: Recovery Point Objective — maximum acceptable data loss measured in time.
- **RTO**: Recovery Time Objective — maximum acceptable downtime after a failure.
- **OTP**: One-Time Password.
- **DigiLocker**: Indian government digital document storage and verification service.
- **LaTeX**: A document preparation system used to render mathematical notation.
- **MathML**: Mathematical Markup Language for representing mathematical notation in XML.
- **MCQ**: Multiple Choice Question — a question type (Single_Correct_MCQ) where exactly one option is marked correct.
- **MSQ**: Multiple Select Question — a question type (Multiple_Correct_MCQ) where one or more options may be marked correct.
- **Option**: A selectable answer choice associated with an MCQ or MSQ question, carrying an Option_Id, display text, and an `isCorrect` boolean flag.
- **Option_Id**: The single uppercase letter (A, B, C, D, E, or F) that uniquely identifies an Option within a question.
- **answerKey**: The JSON field in a question's content record that stores the serialized list of Options for MCQ and MSQ questions.
- **OpenTelemetry**: A vendor-neutral observability framework for traces, metrics, and logs.
- **JWT**: JSON Web Token — a compact, self-contained token format for OAuth2/OIDC.
- **SAST**: Static Application Security Testing.
- **DAST**: Dynamic Application Security Testing.
- **WAF**: Web Application Firewall.


---

## Requirements

### Requirement 1: Candidate Registration and Identity Verification

**User Story:** As a candidate, I want to register on the platform using a government-issued identity document, so that my identity is verified before I am permitted to appear for any examination.

#### Acceptance Criteria

1. WHEN a candidate submits a registration request with a supported identity document type (Aadhaar, PAN, Passport, Voter ID, or Driving License), THE Identity_Service SHALL create a pending candidate account and return an acknowledgement within 2 seconds.
2. WHEN a candidate completes OTP verification on a registered mobile number, THE Identity_Service SHALL activate the candidate account and issue a JWT access token.
3. WHEN a candidate initiates DigiLocker verification, THE Candidate_Service SHALL validate the document data returned by DigiLocker and update the candidate profile verification status to "document-verified".
4. WHEN a candidate submits a face verification request, THE Candidate_Service SHALL compare the submitted photograph against the identity document photograph and reject the request IF the similarity score is below the configured threshold.
5. IF a candidate attempts registration with an identity document number already associated with an existing active account, THEN THE Identity_Service SHALL reject the request with an error code indicating duplicate identity.
6. THE Candidate_Service SHALL store candidate PII (name, date of birth, gender, nationality, category, mobile, email, address, education history, and reservation category) encrypted at rest using AES-256.
7. WHEN a candidate profile is created, THE Audit_Service SHALL record an immutable audit event containing the candidate identifier, identity document type, timestamp, and IP address.

---

### Requirement 2: Authentication and Multi-Factor Security

**User Story:** As a registered user of any role, I want to authenticate securely using multiple factors, so that unauthorized access to the platform is prevented.

#### Acceptance Criteria

1. WHEN a user submits valid username and password credentials, THE Identity_Service SHALL respond with a session token within 2 seconds.
2. WHEN MFA is enabled on an account, THE Identity_Service SHALL require a valid OTP or hardware token before issuing a JWT access token.
3. WHEN a user authenticates using WebAuthn or FIDO2, THE Identity_Service SHALL verify the authenticator assertion and issue a JWT access token without requiring a password.
4. WHEN a user fails authentication five consecutive times within a 10-minute window, THE Identity_Service SHALL lock the account and notify the registered email address.
5. WHILE a candidate session is active, THE Identity_Service SHALL enforce device binding so that the session token is invalid when used from a different device fingerprint.
6. WHEN the risk engine detects an anomalous login signal (new device, unusual geography, or unusual time), THE Identity_Service SHALL require step-up authentication before granting access.
7. THE Identity_Service SHALL enforce a maximum of one concurrent active session per candidate during an active examination shift.
8. WHEN a user session exceeds the configured idle timeout, THE Identity_Service SHALL invalidate the session token and require re-authentication.
9. WHEN a privileged user (Exam Controller, Security Administrator, Auditor) authenticates, THE Audit_Service SHALL record an immutable login event containing the user identifier, role, timestamp, IP address, and device fingerprint.


---

### Requirement 3: Role-Based and Attribute-Based Authorization

**User Story:** As a Security Administrator, I want fine-grained access control over every platform function, so that each user role can access only the resources and actions permitted by their role and context.

#### Acceptance Criteria

1. THE Identity_Service SHALL enforce RBAC for all API endpoints, denying requests from users whose assigned role does not include the required permission.
2. THE Identity_Service SHALL enforce ABAC rules that restrict access based on resource attributes such as exam ownership, question authorship, and subject specialization.
3. WHEN a user requests an action not permitted by their role, THE Identity_Service SHALL return an HTTP 403 response and log a denied-access audit event.
4. THE Identity_Service SHALL support the following named roles: Super_Admin, Security_Admin, Question_Author, Reviewer, Approver, Exam_Controller, Translator, Evaluator, Auditor, and Candidate.
5. WHEN a Super_Admin assigns or revokes a role from a user, THE Audit_Service SHALL record an immutable role-change event containing the actor, target user, role, and timestamp.
6. THE Identity_Service SHALL apply the least-privilege principle, granting each role only the minimum permissions required for its defined responsibilities.

---

### Requirement 4: Question Bank Management

**User Story:** As a Question Author, I want to create, version, and manage questions with rich content and metadata, so that a high-quality, well-classified question bank is available for paper generation.

#### Acceptance Criteria

1. WHEN a Question_Author submits a new question with required metadata (subject, topic, subtopic, chapter, difficulty level, cognitive level, and question type), THE Question_Bank_Service SHALL create a question record in Draft state and return the assigned question identifier.
2. THE Question_Bank_Service SHALL support the following question types: Single_Correct_MCQ, Multiple_Correct_MCQ, Numerical, Descriptive, Matrix_Match, Assertion_Reason, Coding_Question, and Case_Study.
3. THE Question_Bank_Service SHALL accept rich content in HTML5, SVG, PNG, JPEG, WEBP, Audio, Video, LaTeX, and MathML formats within question text and answer options.
4. THE Question_Bank_Service SHALL maintain a version history for every question, storing each revision with its author identifier, change timestamp, and a diff of modified fields.
5. WHEN a question is saved, THE Question_Bank_Service SHALL store the question content encrypted with AES-256 using a per-question encryption key managed by the HSM.
6. THE Question_Bank_Service SHALL enforce question lifecycle transitions: Draft → Review → Approved → Published → Archived, rejecting any transition that skips a state.
7. WHEN a Question_Author submits a question with content that exceeds a similarity threshold of 80% against an existing published question, THE Question_Bank_Service SHALL reject the submission and return the identifier of the similar question.
8. THE Question_Bank_Service SHALL track exposure metadata per question: total usage count, last used date, list of exam identifiers and shift identifiers in which the question was used.
9. THE Question_Bank_Service SHALL enforce configurable reuse policies per question: Never, 1_Year, 2_Years, or Custom_Duration, rejecting paper generation requests that would violate the policy.
10. WHEN a question transitions to Published state, THE Audit_Service SHALL record an immutable event containing the question identifier, approver identifier, and timestamp.


---

### Requirement 5: Question Review and Approval Workflow

**User Story:** As a Reviewer, I want to review questions submitted by authors and either approve or return them for revision, so that only quality-validated questions enter the published bank.

#### Acceptance Criteria

1. WHEN a Question_Author transitions a question to Review state, THE Question_Bank_Service SHALL assign the question to an available Reviewer based on subject specialization and notify the Reviewer via the Notification_Service.
2. WHEN a Reviewer approves a question, THE Question_Bank_Service SHALL transition the question to Approved state and notify the Question_Author.
3. WHEN a Reviewer returns a question with review comments, THE Question_Bank_Service SHALL transition the question back to Draft state, attach the comments to the question record, and notify the Question_Author.
4. WHEN an Approver approves a question in Approved state, THE Question_Bank_Service SHALL transition the question to Published state.
5. THE Question_Bank_Service SHALL enforce a four-eyes principle: the Reviewer and Approver for a given question SHALL be different users.
6. WHEN a question review or approval action is performed, THE Audit_Service SHALL record an immutable event containing the action type, actor identifier, question identifier, and timestamp.

---

### Requirement 6: Translation Management

**User Story:** As a Translator, I want to create and manage language variants of questions, so that candidates can attempt examinations in their preferred scheduled Indian language.

#### Acceptance Criteria

1. THE Translation_Service SHALL support translation of question content into all 22 scheduled Indian languages listed in the Eighth Schedule of the Indian Constitution.
2. WHEN a Translation_Service translation request is created for a Published question, THE Translation_Service SHALL initiate a translation workflow: Author → Translator → Reviewer → Approver.
3. WHEN a Translator submits a translated question variant, THE Translation_Service SHALL store the translation in draft status linked to the source question identifier and language code.
4. WHEN a Translation_Reviewer approves a translated variant, THE Translation_Service SHALL mark the translation as approved and make it available for paper generation in that language.
5. WHEN a Translation_Reviewer identifies a semantic inconsistency between the source question and a translated variant, THE Translation_Service SHALL reject the translation, attach reviewer comments, and notify the Translator.
6. THE Translation_Service SHALL store all translated content in UTF-8 encoding and render it correctly in the Angular_App using the corresponding Unicode font.
7. WHEN a source question is modified after translation approval, THE Translation_Service SHALL mark all approved translations as stale and notify assigned Translators to review the changes.


---

### Requirement 7: Examination Configuration

**User Story:** As an Exam Controller, I want to define examinations with sections, marking schemes, and navigation rules, so that the exam structure precisely matches the official syllabus and conduct rules.

#### Acceptance Criteria

1. WHEN an Exam_Controller creates an examination, THE Examination_Service SHALL persist the exam record with: name, scheduled duration in minutes, total marks, negative marking flag, negative marks per wrong answer, and a list of section definitions.
2. THE Examination_Service SHALL support defining multiple sections per examination, each with: section name, number of questions, marks per question, optional per-section time limit in minutes, and subject-topic distribution rules.
3. THE Examination_Service SHALL support the following navigation policies per examination: Sequential (candidates must answer in order), Flexible (free navigation within the section), and Restricted (no return to answered questions).
4. THE Examination_Service SHALL support configuring a calculator availability policy per examination: None, Basic, or Scientific.
5. THE Examination_Service SHALL support configuring a review-and-flag policy per examination, allowing candidates to mark questions for later review.
6. WHEN an Examination configuration is saved, THE Examination_Service SHALL validate that the sum of section marks equals the declared total marks, and reject the configuration with a descriptive error if validation fails.
7. WHEN an Exam_Controller publishes an examination configuration, THE Audit_Service SHALL record an immutable event containing the exam identifier, actor identifier, and timestamp.

---

### Requirement 7b: Examination Scheduling

**User Story:** As an Exam Controller, I want to plan, publish, and manage examination schedules including dates, shifts, centres, and seat allocations, so that candidates receive accurate admit cards and the examination is conducted in an organised, auditable manner.

#### Acceptance Criteria

1. WHEN an Exam_Controller creates an examination schedule, THE Examination_Service SHALL persist the schedule with: schedule name, version number, government notification reference number, examination date, reserve date, time zone (default IST), and status (Draft / Approved / Published).
2. THE Examination_Service SHALL support one or more shifts per examination date, each with: shift number, shift name, reporting time, gate closing time, candidate login start time, exam start time, exam end time, candidate exit time, duration in minutes, and buffer time before the next shift.
3. THE Examination_Service SHALL enforce the following shift timing validation rules, rejecting any schedule where a rule is violated with a descriptive error identifying the failing field:
   a. Reporting time must precede gate closing time.
   b. Gate closing time must precede candidate login start time.
   c. Candidate login start time must precede exam start time.
   d. Exam start time must precede exam end time.
   e. Shift duration in minutes must equal the difference between exam start time and exam end time.
   f. No two shifts on the same examination date may have overlapping time windows.
4. THE Examination_Service SHALL support multi-day, multi-phase, and multi-session scheduling, allowing an examination to span multiple dates, each with independent shift configurations.
5. THE Examination_Service SHALL support examination centre management per schedule, recording for each centre: region, state, district, city, centre name, building, floor, laboratory identifier, total capacity, and active status.
6. FOR EVERY shift, THE Examination_Service SHALL manage seat allocation records containing: total seats, available seats, reserved seats, PwD (Persons with Disabilities) seats, emergency buffer seats, female reserved seats, and special category seats; and SHALL reject any candidate allocation that would cause available seats to drop below zero.
7. WHEN a schedule is submitted for publication, THE Examination_Service SHALL enforce a multi-stage approval workflow: Draft → Scheduler Review → Exam Controller Approval → Security Review → Chairman Approval → Published; each transition SHALL require an authenticated actor in the appropriate role and SHALL record the approver identifier and timestamp.
8. WHEN an amendment to a Published schedule is required, THE Examination_Service SHALL require a mandatory amendment reason, route the change through Exam Controller Approval and Chairman Approval, republish the schedule with an incremented version number, and trigger a candidate notification event on Kafka `exam.notifications.outbound`.
9. THE Examination_Service SHALL maintain a complete version history for every schedule, storing for each version: version number, created-by identifier, creation timestamp, modified-by identifier, modification timestamp, approval timestamp, previous version reference, change reason, and effective-from date.
10. THE Examination_Service SHALL validate that reserve dates do not conflict with any already-scheduled examination date in the same tenant, rejecting conflicting reserve date assignments with a descriptive error.
11. THE Examination_Service SHALL validate that no examination centre is allocated to overlapping shifts across different examinations on the same date, returning an error identifying the conflicting centre and shift identifiers.
12. WHEN a schedule is published or amended, THE Notification_Service SHALL deliver notifications to all allocated candidates via email, SMS, mobile push, and the candidate portal, and trigger regeneration of updated admit cards.
13. WHEN any scheduling action occurs (create, approve, amend, publish, cancel), THE Audit_Service SHALL record an immutable audit event containing the schedule identifier, version number, actor identifier, action type, previous value, new value, and timestamp.

---

### Requirement 8: Question Paper Generation

**User Story:** As an Exam Controller, I want to generate statistically balanced question papers from a blueprint, so that all exam shifts are fair and equivalent in difficulty and topic coverage.

#### Acceptance Criteria

1. WHEN an Exam_Controller submits a blueprint specifying subject ratios, topic ratios, difficulty level ratios, and cognitive level ratios, THE Paper_Generator SHALL select questions from the Question_Bank_Service that satisfy all blueprint constraints.
2. THE Paper_Generator SHALL generate papers for an unlimited number of shifts within a single examination, ensuring that each shift paper satisfies the same blueprint constraints.
3. WHEN the Paper_Generator assembles a shift paper, THE Paper_Generator SHALL enforce all active question reuse policies and exclude questions that violate their configured reuse window.
4. WHEN a paper is generated, THE Paper_Generator SHALL compute and attach a difficulty score, a topic distribution report, and a similarity report comparing the paper against previously used papers in the same exam cycle.
5. WHEN the Paper_Generator cannot satisfy blueprint constraints due to insufficient questions in the question bank, THE Paper_Generator SHALL return a descriptive gap report listing which subject-topic-difficulty combinations are under-stocked, without generating a partial paper.
6. THE Paper_Generator SHALL complete paper generation for a single shift within 5 minutes of receiving the blueprint request.
7. WHEN a paper is generated and approved, THE Paper_Generator SHALL encrypt the paper package using AES-256 with a shift-specific key managed by the HSM, and store only question identifiers (not question content) in the paper definition.
8. WHEN a paper generation event occurs, THE Audit_Service SHALL record an immutable event containing the exam identifier, shift identifier, actor identifier, paper identifier, and timestamp.
9. FOR ALL generated shift papers within the same examination, the Paper_Generator SHALL ensure the mean difficulty score difference between any two shift papers does not exceed 2% of the total paper score (statistical comparability invariant).


---

### Requirement 9: Exam Delivery

**User Story:** As a candidate, I want to take my examination through a secure, responsive browser interface, so that I can answer questions in my chosen language without interruption.

#### Acceptance Criteria

1. WHEN a candidate opens their assigned examination within the scheduled shift window, THE Delivery_Service SHALL authenticate the session, decrypt the shift paper using the shift-specific HSM-managed key, and serve the first question within 500ms.
2. THE Delivery_Service SHALL support the following rendering modes: One_Question (one question visible at a time), Section_Mode (all questions in the current section visible), and Batch_Mode (a configurable number of questions visible per page).
3. THE Delivery_Service SHALL serve questions in the language selected by the candidate from the set of approved language variants available for the paper.
4. THE Angular_App SHALL render LaTeX expressions, MathML, SVG diagrams, PNG, JPEG, and WEBP images correctly in all supported browsers without requiring additional plugins.
5. WHILE an examination session is active, THE Delivery_Service SHALL enforce the navigation policy configured for the examination (Sequential, Flexible, or Restricted) and reject navigation requests that violate the policy.
6. WHILE an examination session is active, THE Angular_App SHALL operate in a full-screen locked mode and disable clipboard access, browser developer tools, and print functions.
7. THE Delivery_Service SHALL support encrypted offline delivery for designated offline exam centers, where the exam package is pre-loaded and decrypted locally using a center-specific time-limited key.
8. WHEN a candidate's examination session is due to expire, THE Angular_App SHALL display a warning 5 minutes before the end of the allotted time and automatically submit the current response state when the timer reaches zero.

---

### Requirement 10: Response Capture and Auto-Save

**User Story:** As a candidate, I want my responses saved automatically and continuously, so that I do not lose my answers due to network interruptions or application errors.

#### Acceptance Criteria

1. WHEN a candidate selects or modifies an answer, THE Response_Service SHALL persist the response record containing: question identifier, selected option identifiers or entered value, timestamp, cumulative time spent on the question, and revision sequence number within 200ms.
2. THE Response_Service SHALL auto-save all in-progress responses at a minimum interval of every 30 seconds, independent of any candidate action.
3. THE Response_Service SHALL also auto-save responses on every navigation event (moving to next question, previous question, or section change).
4. IF a network interruption occurs during an active examination session, THEN THE Angular_App SHALL buffer response changes locally and THE Response_Service SHALL reconcile buffered responses with the server-side state upon reconnection, with zero response data loss.
5. THE Response_Service SHALL store the full revision history for each response, enabling reconstruction of the sequence of answers provided by the candidate during the session.
6. WHEN an examination session ends (by timeout or manual submission), THE Response_Service SHALL finalize and lock the response set, preventing further modification.
7. WHEN a response is saved, THE Audit_Service SHALL record an event capturing the candidate identifier, question identifier, session identifier, and timestamp (audit sampling at maximum every 60 seconds per candidate to manage volume).


---

### Requirement 11: Proctoring

**User Story:** As an Exam Controller, I want live and AI-assisted proctoring of online examinations, so that examination integrity is maintained and suspicious behavior is detected and recorded.

#### Acceptance Criteria

1. WHILE an online examination session is active, THE Delivery_Service SHALL capture webcam snapshots at a configurable interval (minimum every 30 seconds) and transmit them to the proctoring subsystem.
2. WHILE an online examination session is active, THE Delivery_Service SHALL capture screen activity recordings and transmit them to the proctoring subsystem.
3. WHEN the AI proctoring module analyzes a webcam snapshot and detects zero faces, THE Audit_Service SHALL record a suspicious-activity event with the candidate identifier, session identifier, timestamp, and event type "no-face-detected".
4. WHEN the AI proctoring module detects more than one face in a webcam snapshot, THE Audit_Service SHALL record a suspicious-activity event with event type "multiple-faces-detected".
5. WHEN the AI proctoring module detects a prohibited object (mobile phone, external display, or printed material) in a webcam snapshot, THE Audit_Service SHALL record a suspicious-activity event with event type "prohibited-object-detected".
6. WHEN the Angular_App detects a full-screen exit event during an active examination session, THE Delivery_Service SHALL record the event and display a warning to the candidate; after three such events, THE Delivery_Service SHALL flag the session for manual review.
7. THE proctoring subsystem SHALL NOT transmit or store candidate audio or video data beyond the configured retention window without explicit consent recorded in the candidate's profile.

---

### Requirement 12: Evaluation

**User Story:** As an Evaluator, I want the platform to evaluate objective responses automatically and support manual evaluation of descriptive responses, so that results are computed accurately and efficiently.

#### Acceptance Criteria

1. WHEN an examination session is finalized, THE Evaluation_Service SHALL automatically evaluate all Single_Correct_MCQ, Multiple_Correct_MCQ, and Numerical question responses using the answer key stored in the paper definition.
2. THE Evaluation_Service SHALL apply the marking scheme defined in the paper: positive marks for correct answers, configurable negative marks for incorrect answers, and zero marks for unattempted questions.
3. THE Evaluation_Service SHALL support partial marking for Multiple_Correct_MCQ questions, awarding marks proportional to the number of correct options selected when no incorrect options are selected.
4. WHEN all automatic evaluations for an exam are complete, THE Evaluation_Service SHALL notify assigned Evaluators via the Notification_Service to begin manual evaluation of Descriptive question responses.
5. WHEN an Evaluator submits a score for a Descriptive response, THE Evaluation_Service SHALL record the score along with the evaluator identifier, evaluation timestamp, and any attached comments.
6. WHERE a hybrid evaluation workflow is configured for an examination, THE Evaluation_Service SHALL route each Descriptive response to two independent Evaluators and flag responses where the score difference exceeds the configured tolerance for a third-party review.
7. WHEN all evaluations for a candidate are complete, THE Evaluation_Service SHALL compute and store the candidate's total raw score and section-wise scores.
8. WHEN an evaluation event is recorded, THE Audit_Service SHALL log an immutable event containing the question identifier, response identifier, evaluator identifier, score awarded, and timestamp.


---

### Requirement 13: Result Processing and Publication

**User Story:** As an Exam Controller, I want to publish verified results including scores, ranks, percentiles, and normalized scores, so that candidates receive accurate, tamper-proof scorecards.

#### Acceptance Criteria

1. WHEN an Exam_Controller initiates result processing, THE Result_Service SHALL compute for each candidate: total score, section-wise scores, overall rank, overall percentile, and normalized score where shift normalization is configured.
2. THE Result_Service SHALL compute shift-wise normalization using the configured normalization formula when an examination spans multiple shifts with different candidate pools.
3. WHEN results are computed, THE Result_Service SHALL make the scorecard available to the candidate via the Angular_App portal, including subject-wise and topic-wise performance breakdowns.
4. WHEN a result is published, THE Result_Service SHALL generate a PDF scorecard protected by a password derived from the candidate's registered date of birth and candidate identifier.
5. WHEN a result is published, THE Notification_Service SHALL send a notification to the candidate's registered email and mobile number containing a download link for the scorecard.
6. THE Result_Service SHALL expose a versioned REST API endpoint allowing authorized third-party systems to retrieve candidate results using OAuth2 access tokens.
7. WHEN a result publication event occurs, THE Audit_Service SHALL record an immutable event containing the exam identifier, actor identifier, total candidate count, and publication timestamp.
8. WHERE DigiLocker integration is enabled for an examination, THE Result_Service SHALL push the issued scorecard to the candidate's DigiLocker account upon publication.

---

### Requirement 14: Notification Service

**User Story:** As a user of any role, I want to receive timely notifications about actions relevant to my work, so that I can act on examination events without polling the platform.

#### Acceptance Criteria

1. WHEN a system event requires user notification (review assignment, approval, result publication, session warning, or account lock), THE Notification_Service SHALL deliver the notification to the user's registered email address within 60 seconds of the triggering event.
2. WHEN a notification delivery to email fails after three consecutive attempts, THE Notification_Service SHALL log the failure and mark the notification as undelivered in the notification log.
3. THE Angular_App SHALL display in-app notifications to authenticated users for platform events relevant to their role, sourced from the Notification_Service.
4. THE Notification_Service SHALL NOT include PII or examination question content in notification message bodies; notifications SHALL contain only identifiers and action links.


---

### Requirement 15: Audit Trail and Immutability

**User Story:** As an Auditor, I want an immutable, tamper-evident audit trail of all privileged and examination-critical actions, so that I can conduct investigations and produce legally admissible evidence.

#### Acceptance Criteria

1. THE Audit_Service SHALL record an immutable audit event for each of the following actions: user login, user logout, question creation, question modification, question state transition, paper generation, paper approval, exam session start, exam session submission, evaluation submission, and result publication.
2. WHEN an audit event is written, THE Audit_Service SHALL sign the event payload with a private key managed by the HSM and store the signature alongside the event record to enable tamper detection.
3. THE Audit_Service SHALL expose a read-only query API that allows Auditors to search and retrieve audit events by user identifier, exam identifier, action type, and time range.
4. IF an attempt is made to modify or delete an existing audit record through any API, THEN THE Audit_Service SHALL reject the request with HTTP 403 and log the attempted modification as a new audit event.
5. THE Audit_Service SHALL retain audit records for a minimum of 7 years from the date of the associated examination to satisfy legal evidence retention requirements.
6. THE PostgreSQL_DB tables backing the Audit_Service SHALL be configured with append-only access grants, preventing UPDATE and DELETE operations at the database level.

---

### Requirement 16: Data Encryption and Key Management

**User Story:** As a Security Administrator, I want all sensitive data encrypted at rest and in transit with HSM-managed keys, so that data breaches and key compromise risks are minimized.

#### Acceptance Criteria

1. THE Platform SHALL encrypt all data at rest in the PostgreSQL_DB using AES-256, applied at the column level for PII and question content fields.
2. THE Platform SHALL enforce TLS 1.3 for all data in transit between the Angular_App, backend services, and the PostgreSQL_DB.
3. THE Identity_Service SHALL integrate with an HSM for generating, storing, and rotating cryptographic keys, ensuring private keys are never exposed outside the HSM boundary.
4. WHEN a cryptographic key reaches its configured rotation interval, THE Identity_Service SHALL automatically initiate key rotation and re-encrypt affected data using the new key without service interruption.
5. WHEN a key revocation event is triggered by the Security_Admin, THE Identity_Service SHALL revoke the specified key within 60 seconds and log a key-revocation audit event.
6. THE Platform SHALL encrypt all database backups using AES-256 before writing them to the backup storage destination.
7. THE Platform SHALL use per-question encryption keys for question content, derived from a master key managed by the HSM, so that compromise of one question key does not expose the full question bank.


---

### Requirement 17: Threat Protection and Network Security

**User Story:** As a Security Administrator, I want the platform protected against brute force attacks, DDoS, and injection attacks, so that examination integrity and availability are not compromised by adversaries.

#### Acceptance Criteria

1. THE Identity_Service SHALL enforce rate limiting on authentication endpoints, rejecting requests that exceed 10 authentication attempts per IP address per minute with HTTP 429.
2. THE Platform SHALL integrate with a WAF that inspects all inbound HTTP requests and blocks requests matching OWASP Top 10 attack patterns before they reach backend services.
3. THE Platform SHALL integrate DDoS mitigation at the network edge, with automatic traffic absorption and alerting when inbound request rates exceed 10,000 requests per second from a single origin.
4. THE Platform SHALL use parameterized queries or JPA/Hibernate ORM for all PostgreSQL_DB interactions, preventing SQL injection attacks.
5. THE Platform SHALL validate and sanitize all user-supplied input at the API gateway layer before routing requests to backend services.
6. THE Identity_Service SHALL implement a Zero Trust architecture where every inter-service API call is authenticated using a service identity token, not a shared secret.
7. WHEN a security threat event is detected (brute force, injection attempt, or anomalous traffic), THE Audit_Service SHALL record a security alert event and THE Notification_Service SHALL alert the Security_Admin within 60 seconds.

---

### Requirement 18: Performance Requirements

**User Story:** As a candidate, I want the platform to respond quickly under all load conditions, so that examination time is not wasted waiting for the system.

#### Acceptance Criteria

1. WHEN a valid authentication request is submitted, THE Identity_Service SHALL respond with a session token within 2 seconds at the 99th percentile under a concurrent load of 500,000 active sessions.
2. WHEN a candidate requests a question during an active examination session, THE Delivery_Service SHALL serve the question content within 500ms at the 99th percentile under a concurrent load of 500,000 active sessions.
3. WHEN a candidate submits a response, THE Response_Service SHALL persist the response and acknowledge within 200ms at the 99th percentile under a concurrent load of 500,000 active sessions.
4. WHEN an Exam_Controller submits a paper generation request, THE Paper_Generator SHALL complete paper generation for a single shift within 5 minutes.
5. THE Angular_App SHALL achieve a Lighthouse performance score of 80 or above on desktop and 70 or above on mobile under a standard broadband connection of 10 Mbps.


---

### Requirement 19: Scalability

**User Story:** As an Examination Authority, I want the platform to scale horizontally to support national-scale examinations, so that no performance degradation occurs during peak examination windows.

#### Acceptance Criteria

1. THE Platform SHALL support a registered user base of 5 million candidates without degradation in authentication or profile retrieval response times.
2. THE Platform SHALL support 500,000 concurrently active examination sessions, each independently processing responses, proctoring captures, and auto-saves.
3. THE Question_Bank_Service SHALL support a question bank of 100 million questions with full-text search results returned within 2 seconds at the 95th percentile.
4. THE Response_Service SHALL support ingestion of 500 million daily response saves across all active examination sessions.
5. THE Platform SHALL support horizontal scaling of all microservices by adding container instances without requiring code changes or service restarts, using stateless service design backed by PostgreSQL_DB and a distributed cache.
6. THE PostgreSQL_DB SHALL be configured with table partitioning for the response, audit_event, and question tables to maintain query performance as data volumes grow.

---

### Requirement 20: Availability and Disaster Recovery

**User Story:** As an Examination Authority, I want the platform to meet strict availability and recovery targets, so that an in-progress examination is never lost due to infrastructure failure.

#### Acceptance Criteria

1. THE Platform SHALL maintain 99.99% availability, equivalent to a maximum of 52 minutes of unplanned downtime per year, measured as the percentage of time all critical services (Identity, Delivery, Response, Audit) are reachable.
2. THE Platform SHALL have no single point of failure: every microservice, database, and infrastructure component SHALL have at least one redundant instance.
3. THE Platform SHALL achieve an RPO of zero for examination response data, meaning no committed response SHALL be lost in a failure event.
4. THE Platform SHALL achieve an RTO of 15 minutes, meaning the full platform SHALL be restored to operational status within 15 minutes of a complete site failure.
5. THE PostgreSQL_DB SHALL be configured with synchronous replication to at least one standby instance and support Point-in-Time Recovery (PITR).
6. WHEN an automated failover event occurs, THE Audit_Service SHALL record a failover event containing the affected component, timestamp, and recovery status.
7. THE Platform SHALL perform automated database backups at a minimum frequency of every 6 hours, with backups verified for restorability on a weekly basis.


---

### Requirement 21: Observability

**User Story:** As a Security Administrator or platform operator, I want comprehensive structured logging, metrics, and distributed tracing, so that I can detect incidents and diagnose performance issues in real time.

#### Acceptance Criteria

1. THE Platform SHALL emit structured JSON logs for every API request and response, including: service name, trace identifier, span identifier, HTTP method, path, status code, response time in milliseconds, and user identifier (masked).
2. THE Platform SHALL expose Prometheus-compatible metrics endpoints from every microservice, including: request throughput, error rate, response latency percentiles (p50, p95, p99), JVM heap usage, and database connection pool utilization.
3. THE Platform SHALL instrument all inter-service calls with OpenTelemetry traces, enabling end-to-end distributed tracing from Angular_App request to database response.
4. THE Platform SHALL provide pre-built Grafana dashboards for: active examination sessions, response throughput, service error rates, authentication events, and proctoring alert rates.
5. WHEN any service's error rate exceeds 1% of requests over a 5-minute rolling window, THE observability stack SHALL trigger an alert delivered to the configured on-call channel within 2 minutes.
6. THE Platform SHALL retain metrics data for a minimum of 90 days and log data for a minimum of 365 days in the configured observability storage backend.

---

### Requirement 22: Accessibility

**User Story:** As a candidate with a disability, I want the platform's examination interface to be fully accessible, so that I can participate in examinations without barriers.

#### Acceptance Criteria

1. THE Angular_App SHALL conform to WCAG 2.2 Level AA for all examination-related pages, including login, candidate profile, exam instructions, question delivery, and result pages.
2. THE Angular_App SHALL support full keyboard navigation across all interactive elements without requiring a mouse, following standard keyboard interaction patterns (Tab, Shift+Tab, Enter, Space, Arrow keys).
3. THE Angular_App SHALL be compatible with screen readers (NVDA, JAWS, and VoiceOver) and provide meaningful ARIA labels and roles for all dynamic content including question counters, timers, and answer selection states.
4. THE Angular_App SHALL provide a high-contrast mode that meets WCAG 2.2 AA contrast ratios (minimum 4.5:1 for normal text and 3:1 for large text) when enabled by the user.
5. THE Angular_App SHALL render all text content at the user's configured browser font size without horizontal scrolling or content truncation on screens with a minimum width of 320 CSS pixels.
6. WHERE a candidate has registered a disability requiring extended time, THE Examination_Service SHALL apply the configured time extension to the candidate's session before the session starts.


---

### Requirement 23: REST API and Integration Standards

**User Story:** As a third-party system integrator (university portal, government agency), I want well-documented, versioned REST APIs protected by OAuth2, so that I can integrate result and candidate data into my own systems reliably.

#### Acceptance Criteria

1. THE Platform SHALL expose all public and internal service interactions via REST APIs conforming to the HTTP/1.1 specification and returning JSON-formatted responses.
2. THE Platform SHALL publish OpenAPI 3.0 specification documents for all REST APIs, available at a versioned documentation endpoint (e.g., `/api/v1/docs`).
3. THE Platform SHALL version all REST APIs using a URL path prefix (e.g., `/api/v1/`, `/api/v2/`) and maintain backward compatibility within the same major version.
4. THE Platform SHALL implement OAuth2 with OIDC for all API authentication, issuing JWT access tokens with a maximum lifetime of 15 minutes and refresh tokens with a maximum lifetime of 8 hours.
5. WHEN an API request carries an expired or invalid JWT, THE Identity_Service SHALL reject the request with HTTP 401 before the request reaches the target microservice.
6. THE Platform SHALL enforce API rate limits per client identifier: 1000 requests per minute for standard integration clients, and a configurable higher limit for approved high-volume partners.

---

### Requirement 24: DevSecOps and CI/CD Pipeline

**User Story:** As a platform contributor, I want a fully automated CI/CD pipeline with integrated security scanning, so that every code change is validated for correctness and security before deployment.

#### Acceptance Criteria

1. THE Platform SHALL include a CI/CD pipeline configuration that executes the following stages in order: Build, Unit Test, Integration Test, SAST (Static Application Security Testing), DAST (Dynamic Application Security Testing), Container Image Build, and Deployment.
2. WHEN a SAST scan detects a high-severity vulnerability in a pull request, THE CI/CD pipeline SHALL block the merge and report the vulnerability details to the submitting contributor.
3. WHEN a DAST scan detects an OWASP Top 10 vulnerability in a deployed staging environment, THE CI/CD pipeline SHALL create an automated issue in the project issue tracker and notify the security team.
4. THE Platform SHALL provide Docker Compose configuration files for local development that start all microservices, the PostgreSQL_DB, a message broker, and observability tooling with a single command.
5. THE Platform SHALL publish container images to a configured container registry as part of the CI/CD pipeline, tagged with the commit SHA and semantic version.
6. THE Platform deployment configuration SHALL be vendor-neutral, using Kubernetes manifests and Helm charts compatible with any CNCF-conformant Kubernetes distribution, with no dependency on a specific cloud provider's managed services.


---

### Requirement 25: Compliance — DPDP Act and Privacy

**User Story:** As an Examination Authority, I want the platform to be compliant with the Digital Personal Data Protection Act and ISO 27001, so that candidate data is handled lawfully and the platform is auditable for compliance.

#### Acceptance Criteria

1. THE Platform SHALL classify all stored data fields as PII, Sensitive, or Non-sensitive, and apply appropriate access controls and retention policies per classification.
2. THE Candidate_Service SHALL retain candidate PII for the period required by the applicable examination authority's data retention policy, and SHALL provide a mechanism for data erasure upon receipt of a verified erasure request in accordance with the DPDP Act.
3. THE Angular_App SHALL present a clear, plain-language consent notice to candidates before collecting biometric verification data, and SHALL record the candidate's explicit consent with a timestamp.
4. THE Platform SHALL not transfer candidate PII to any third-party system that is not listed in the examination authority's approved data processor registry.
5. THE Platform SHALL produce an ISO 27001-aligned information security evidence package including: asset inventory, risk register reference, access control records, and audit log exports, accessible to the Auditor role.
6. THE Platform SHALL enforce data residency by storing candidate PII and examination responses exclusively in the configured deployment region's PostgreSQL_DB instances, with no cross-region replication of PII unless explicitly configured by the Security_Admin.

---

### Requirement 26: Analytics and Reporting

**User Story:** As an Exam Controller, I want statistical analytics on examination performance, question quality, and candidate behavior, so that future examinations can be improved based on evidence.

#### Acceptance Criteria

1. WHEN an examination is finalized, THE Result_Service SHALL compute per-question analytics: difficulty index (percentage of candidates who answered correctly), discrimination index (correlation between question score and total score), and response distribution across all options.
2. THE Angular_App SHALL display a per-examination analytics dashboard to authorized Exam_Controller users showing: total registered candidates, total appeared candidates, score distribution histogram, section-wise average scores, and top and bottom 10th percentile score thresholds.
3. THE Platform SHALL export examination analytics reports in CSV and PDF formats from the Angular_App.
4. THE Analytics data SHALL be computed from finalized, locked response sets and SHALL NOT reflect in-progress or unsubmitted sessions.
5. WHEN a Question_Author views a question's analytics, THE Question_Bank_Service SHALL display the question's historical difficulty index, discrimination index, and usage count across all past examinations.


---

### Requirement 27: Open Source Governance and Contribution Model

**User Story:** As an open-source contributor or government agency, I want a well-structured repository and contribution process, so that I can contribute to and audit the platform source code with confidence.

#### Acceptance Criteria

1. THE Platform source code SHALL be organized in a monorepo with the following top-level directories: `/frontend` (Angular_App), `/backend` (Spring Boot services), `/infrastructure` (Kubernetes, Helm, Docker Compose), and `/docs` (architecture and API documentation).
2. THE Platform SHALL be licensed under the Apache 2.0 license, with the LICENSE file present in the repository root.
3. WHEN a contributor submits a pull request, THE CI/CD pipeline SHALL run all build, test, and SAST stages automatically and report results on the pull request before it can be merged.
4. THE Platform SHALL maintain a CONTRIBUTING.md file documenting the RFC process for significant changes, pull request guidelines, coding standards, and security disclosure procedures.
5. THE Platform SHALL include a published security vulnerability disclosure policy (SECURITY.md) instructing contributors on how to report security vulnerabilities responsibly, without public disclosure before a fix is available.

---

### Requirement 28: Question Paper and Content Serialization (Round-Trip)

**User Story:** As an Exam Controller, I want question paper definitions to be serializable and deserializable without data loss, so that paper packages can be securely transferred, stored, and reconstructed exactly.

#### Acceptance Criteria

1. THE Paper_Generator SHALL serialize a generated paper definition into a JSON document conforming to the platform's published Paper Schema.
2. THE Paper_Generator SHALL parse a serialized Paper JSON document back into a complete Paper object with all question identifiers, section structure, blueprint metadata, and shift configuration intact.
3. THE Paper_Generator SHALL format a Paper object back into a valid Paper JSON document that conforms to the platform's published Paper Schema.
4. FOR ALL valid Paper objects, parsing a serialized Paper JSON document, then formatting it back to JSON, then parsing it again SHALL produce a Paper object equivalent to the first parsed result (round-trip property).
5. IF a Paper JSON document contains a field value that violates the Paper Schema (invalid question type, negative marks, or missing required field), THEN THE Paper_Generator SHALL return a descriptive validation error identifying the violating field and value.


---

### Requirement 29: Administration and Platform Configuration

**User Story:** As a Super Administrator, I want centralized platform configuration and user management, so that I can onboard examination authorities, manage service settings, and respond to operational incidents without code changes.

#### Acceptance Criteria

1. THE Angular_App SHALL provide a Super_Admin console that allows creating, updating, deactivating, and listing users of all roles.
2. THE Platform SHALL support multi-tenancy at the examination authority level, where each authority's examinations, questions, and candidate data are logically isolated from other authorities' data.
3. WHEN a Super_Admin deactivates a user account, THE Identity_Service SHALL immediately invalidate all active sessions for that user and prevent new authentication.
4. THE Platform SHALL expose all configurable parameters (session timeout duration, rate limit thresholds, auto-save interval, paper generation concurrency limits) via a configuration API accessible only to Super_Admin and Security_Admin roles.
5. WHEN a platform configuration parameter is changed via the configuration API, THE Audit_Service SHALL record an immutable configuration-change event containing the parameter name, old value, new value, actor identifier, and timestamp.

---

### Requirement 30: MCQ/MSQ Option Management in the Question Bank

**User Story:** As a Question Author, I want to define, edit, and validate the answer options for MCQ and MSQ questions through the question form dialog, so that each question has a well-formed, correctly marked set of choices that the platform can evaluate automatically.

#### Acceptance Criteria

1. THE Question_Bank_Service SHALL accept an `options` list in every `CreateQuestionRequest` and `QuestionResponse` for Single_Correct_MCQ and Multiple_Correct_MCQ question types, where each entry contains: `id` (Option_Id), `text` (non-empty string), and `isCorrect` (boolean).
2. THE Question_Bank_Service SHALL enforce that an MCQ or MSQ question contains between 2 and 6 Options inclusive, rejecting requests that fall outside this range with a descriptive validation error.
3. WHEN a `CreateQuestionRequest` is submitted for a Single_Correct_MCQ question, THE Question_Bank_Service SHALL validate that exactly one Option has `isCorrect` set to `true`; IF the count is zero or greater than one, THEN THE Question_Bank_Service SHALL reject the request with a descriptive error identifying the violation.
4. WHEN a `CreateQuestionRequest` is submitted for a Multiple_Correct_MCQ question, THE Question_Bank_Service SHALL validate that at least one Option has `isCorrect` set to `true`; IF no Option is marked correct, THEN THE Question_Bank_Service SHALL reject the request with a descriptive error.
5. THE Question_Bank_Service SHALL assign a unique Option_Id drawn from the ordered set {A, B, C, D, E, F} to each Option, corresponding to its position in the submitted list (first Option → A, second → B, and so on up to a maximum of F).
6. WHEN a question with options is saved, THE Question_Bank_Service SHALL serialize the Options list as a JSON array into the `answerKey` field of the question content record, preserving the `id`, `text`, and `isCorrect` values for each Option.
7. WHEN a question record is retrieved, THE Question_Bank_Service SHALL deserialize the `answerKey` JSON field back into the structured Options list and include it in the `QuestionResponse`, with no data loss or field corruption (round-trip property).
8. THE Angular_App question form dialog SHALL allow a Question_Author to add a new Option to a Single_Correct_MCQ or Multiple_Correct_MCQ question; WHILE the current option count is 6, THE Angular_App SHALL disable the "Add Option" control and display a message indicating the maximum has been reached.
9. THE Angular_App question form dialog SHALL allow a Question_Author to remove an existing Option; WHILE the current option count is 2, THE Angular_App SHALL disable all individual "Remove Option" controls to preserve the minimum required count.
10. WHEN a Question_Author enters or modifies the text for an Option in the Angular_App question form dialog, THE Angular_App SHALL update the in-memory Option immediately and mark the form as unsaved.
11. WHEN a Question_Author selects a correct-answer indicator for a Single_Correct_MCQ question in the Angular_App question form dialog, THE Angular_App SHALL enforce single-selection by deselecting any previously selected Option and marking only the newly selected Option as `isCorrect`.
12. WHEN a Question_Author toggles a correct-answer indicator for a Multiple_Correct_MCQ question in the Angular_App question form dialog, THE Angular_App SHALL allow multiple Options to be simultaneously marked as `isCorrect` without deselecting previously marked Options.
13. IF a Question_Author attempts to submit the question form dialog with no Option marked as `isCorrect`, THEN THE Angular_App SHALL display an inline validation error and prevent submission.
14. THE Angular_App question form dialog SHALL display each Option with its Option_Id label (A, B, C, …) alongside the text input and correct-answer indicator, updated dynamically as Options are added or removed.
15. WHEN the `answerKey` field is deserialized from a stored question record and the resulting Options list does not match the original serialized list (different count, changed `id`, changed `isCorrect`, or changed `text`), THE Question_Bank_Service SHALL treat the record as corrupted and return an error response identifying the affected question identifier.

---

*End of Requirements Document*
