# Module Specification: Workflow & State Machine Engine

## 1. Overview & Purpose

The **Workflow Module** manages state machine transitions, role-based authorization checks, and audit logging across all platform approval lifecycle objects (Exam Schedules, Question Items, Exam Papers).

---

## 2. Supported Workflows

### 2.1 Examination Schedule Approval State Machine
```
[ DRAFT ] ──> [ SCHEDULER_REVIEW ] ──> [ CONTROLLER_APPROVED ] ──> [ SECURITY_REVIEW ] ──> [ CHAIRMAN_APPROVED ] ──> [ PUBLISHED ]
    │                  │                        │                        │                     │
    └──────────────────┴────────────────────────┴────────────────────────┴─────────────────────┴──> [ CANCELLED ]
```

### 2.2 Question Item Review Workflow
```
[ DRAFT ] ──> [ IN_REVIEW ] ──> [ APPROVED ]
   │                 │
   └─────────────────┴────────> [ REJECTED ]
```

---

## 3. Workflow Rule Rules Engine

```typescript
interface WorkflowRule {
  currentStatus: string;
  targetStatus: string;
  allowedRoles: string[];
  requiresComment: boolean;
}
```

- Transition to `PUBLISHED` requires prior status to be `CHAIRMAN_APPROVED` AND user role `SUPER_ADMIN` or `EXAM_CONTROLLER`.
- Transition to `CANCELLED` requires a mandatory reason comment.
- Every state transition emits a immutable event to Kafka topic `workflow.transitions`.
