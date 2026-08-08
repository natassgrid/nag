# Module Specification: Notification Service

## 1. Overview & Purpose

The **Notification Service** processes asynchronous event streams to deliver SMS alerts, email confirmations, hall tickets, registration OTPs, and schedule change announcements to candidates and administrators.

---

## 2. Supported Delivery Channels

- **SMS Gateway**: Transactional SMS for OTPs, seat allocations, and exam schedule alerts.
- **Email (SMTP / SES)**: HTML emails for hall tickets, registration confirmation, and official result scorecards.
- **In-App Notifications**: Real-time notifications rendered via Angular Material navigation bar.

---

## 3. Subscribed Kafka Topics

- `candidate.events` $\rightarrow$ Triggers registration confirmation & OTP emails.
- `workflow.transitions` $\rightarrow$ Triggers schedule change alerts to state exam controllers.
- `schedule.events` $\rightarrow$ Triggers hall ticket issuance notifications to candidates.
