# Test Curl Commands

Base URLs (local Docker deployment):
- Identity Service: `http://localhost:8081`
- Candidate Service: `http://localhost:8082`
- Question Bank: `http://localhost:8083`
- Translation: `http://localhost:8084`
- Examination: `http://localhost:8085`
- Paper Generator: `http://localhost:8086`
- Delivery: `http://localhost:8087`
- Response: `http://localhost:8088`
- Evaluation: `http://localhost:8089`
- Result: `http://localhost:8090`
- Audit: `http://localhost:8091`
- Notification: `http://localhost:8092`
- Admin: `http://localhost:8093`
- Analytics: `http://localhost:8094`
- API Gateway: `http://localhost:9000`

## Health Checks

```bash
# All services expose /actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health
curl http://localhost:8086/actuator/health
curl http://localhost:8087/actuator/health
curl http://localhost:8088/actuator/health
curl http://localhost:8089/actuator/health
curl http://localhost:8090/actuator/health
curl http://localhost:8091/actuator/health
curl http://localhost:8092/actuator/health
curl http://localhost:8093/actuator/health
curl http://localhost:8094/actuator/health
curl http://localhost:9000/actuator/health
```

## Identity Service (port 8081)

### Login (Password Auth)
```bash
curl -X POST http://localhost:8081/api/v1/identity/auth/token \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: exam-authority-1" \
  -d '{
    "username": "superadmin",
    "password": "Password@123"
  }'
```

### Login with MFA
```bash
curl -X POST http://localhost:8081/api/v1/identity/auth/token \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: exam-authority-1" \
  -d '{
    "username": "superadmin",
    "password": "Password@123",
    "otpCode": "123456",
    "deviceFingerprint": "test-device-fp"
  }'
```

### Register New User
```bash
curl -X POST http://localhost:8081/api/v1/identity/register \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: exam-authority-1" \
  -d '{
    "username": "newuser",
    "mobileNumber": "+919876543210",
    "identityDocType": "AADHAAR",
    "identityDocNumber": "123456789012"
  }'
```

### Get User Roles
```bash
curl http://localhost:8081/api/v1/identity/roles/a0000001-0000-0000-0000-000000000001 \
  -H "X-Tenant-Id: exam-authority-1" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

### Assign Role (SUPER_ADMIN only)
```bash
curl -X POST http://localhost:8081/api/v1/identity/roles/a0000006-0000-0000-0000-000000000006 \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: exam-authority-1" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "role": "EVALUATOR",
    "action": "ASSIGN"
  }'
```

## Question Bank Service (port 8083)

### Create Question
```bash
curl -X POST http://localhost:8083/api/v1/questions \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: exam-authority-1" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "subject": "Mathematics",
    "topic": "Algebra",
    "subtopic": "Linear Equations",
    "difficulty": "MEDIUM",
    "cognitiveLevel": "APPLICATION",
    "questionType": "MCQ",
    "content": "Solve: 2x + 3 = 7",
    "answerKey": "{\"correct\": \"B\", \"options\": [\"x=1\", \"x=2\", \"x=3\", \"x=4\"]}"
  }'
```

### List Questions by Subject
```bash
curl "http://localhost:8083/api/v1/questions?subject=Mathematics&state=DRAFT" \
  -H "X-Tenant-Id: exam-authority-1" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

## Examination Service (port 8085)

### Create Examination
```bash
curl -X POST http://localhost:8085/api/v1/examinations \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: exam-authority-1" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "name": "Civil Services Preliminary Exam 2026",
    "durationMinutes": 120,
    "totalMarks": 200,
    "negativeMarkingEnabled": true,
    "negativeMarkingValue": 0.33,
    "navigationPolicy": "FREE",
    "calculatorPolicy": "NONE",
    "reviewFlagEnabled": true,
    "status": "DRAFT"
  }'
```

## Candidate Service (port 8082)

### Get Candidate Profile
```bash
curl http://localhost:8082/api/v1/candidates/a0000006-0000-0000-0000-000000000006 \
  -H "X-Tenant-Id: exam-authority-1" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

## Admin Service (port 8093)

### Get System Config
```bash
curl http://localhost:8093/api/v1/admin/config \
  -H "X-Tenant-Id: exam-authority-1" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

### Update System Config
```bash
curl -X PUT http://localhost:8093/api/v1/admin/config \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: exam-authority-1" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "paramName": "max.concurrent.sessions",
    "paramValue": "5"
  }'
```

## Audit Service (port 8091)

### Query Audit Events
```bash
curl "http://localhost:8091/api/v1/audit/events?eventType=ROLE_CHANGE&limit=10" \
  -H "X-Tenant-Id: exam-authority-1" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

## Notification Service (port 8092)

### Get Notifications for User
```bash
curl http://localhost:8092/api/v1/notifications \
  -H "X-Tenant-Id: exam-authority-1" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

## Analytics Service (port 8094)

### Get Exam Analytics
```bash
curl http://localhost:8094/api/v1/analytics/exams/{examId} \
  -H "X-Tenant-Id: exam-authority-1" \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

---

## Seed Data Users

| Username | User ID | Role | Password |
|----------|---------|------|----------|
| superadmin | a0000001-...-000000000001 | SUPER_ADMIN | Password@123 |
| secadmin | a0000002-...-000000000002 | SECURITY_ADMIN | Password@123 |
| author1 | a0000003-...-000000000003 | QUESTION_AUTHOR | Password@123 |
| reviewer1 | a0000004-...-000000000004 | REVIEWER | Password@123 |
| controller1 | a0000005-...-000000000005 | EXAM_CONTROLLER | Password@123 |
| candidate1 | a0000006-...-000000000006 | CANDIDATE | Password@123 |
| translator1 | a0000007-...-000000000007 | TRANSLATOR | Password@123 |
| evaluator1 | a0000008-...-000000000008 | EVALUATOR | Password@123 |
| auditor1 | a0000009-...-000000000009 | AUDITOR | Password@123 |
| approver1 | a0000010-...-000000000010 | APPROVER | Password@123 |

Tenant: `exam-authority-1`

> Note: Authentication is managed via Keycloak (OAuth2/OIDC). The seed data creates user records in the identity-service database. For full JWT-based auth, users need to be provisioned in Keycloak first. The `/auth/token` endpoint delegates to the AuthenticationService which may verify against Keycloak.
