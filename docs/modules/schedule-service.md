# Module Specification: Schedule Service

## 1. Overview & Purpose

The **Schedule Service** manages schedule versions, shift timings, test center registries, and seat allocations for examinations.

---

## 2. Core Responsibilities

- Schedule version tracking (`scheduleVersion`, `previousVersionId`, `changeReason`).
- Approval workflow state transitions (`DRAFT` $\rightarrow$ `SCHEDULER_REVIEW` $\rightarrow$ `CONTROLLER_APPROVED` $\rightarrow$ `SECURITY_REVIEW` $\rightarrow$ `CHAIRMAN_APPROVED` $\rightarrow$ `PUBLISHED` / `CANCELLED`).
- Shift creation with strict timing validation ($reporting < gateClose < loginStart < examStart < examEnd$).
- Test center registry management and seat quota allocation (total, available, PwD, buffer).

---

## 3. REST API Reference

Base Path: `/api/v1/examinations`

| Method | Path | Roles | Description |
|---|---|---|---|
| `POST` | `/{examId}/schedules` | EXAM_CONTROLLER | Create DRAFT schedule version |
| `GET` | `/{examId}/schedules` | EXAM_CONTROLLER, SUPER_ADMIN | List all schedule versions |
| `PUT` | `/{examId}/schedules/{scheduleId}/transition` | EXAM_CONTROLLER, SUPER_ADMIN | Transition workflow state |
| `PUT` | `/{examId}/schedules/{scheduleId}/amend` | EXAM_CONTROLLER, SUPER_ADMIN | Amend published schedule |
| `POST` | `/{examId}/schedules/{scheduleId}/shifts` | EXAM_CONTROLLER | Add shift |
| `GET` | `/centres` | EXAM_CONTROLLER, SUPER_ADMIN | List centres (filters: state, city) |
| `POST` | `/{examId}/schedules/{sId}/shifts/{shId}/allocations` | EXAM_CONTROLLER | Upsert seat allocations |

---

## 4. Business Validation Rules

1. **Shift Timing Rules**:
   - `reportingTime` MUST be earlier than `gateClosingTime`.
   - `gateClosingTime` MUST be earlier than `loginStartTime`.
   - `loginStartTime` MUST be earlier than `examStartTime`.
   - `examStartTime` MUST be earlier than `examEndTime`.
   - `durationMinutes` MUST equal $examEndTime - examStartTime$.
2. **Seat Quotas**:
   - `availableSeats` MUST not be negative.
   - Sum of reserved + pwd + buffer seats MUST NOT exceed `totalCapacity`.
