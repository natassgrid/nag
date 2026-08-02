# Security Architecture — Government Examination Platform

## 1. Core Principles & Defense-in-Depth

The **National Assessment Grid (NAG)** security architecture is engineered around the principle of **Defense-in-Depth**, **Zero Trust Networks**, and **Least Privilege Security**. Every layer of the stack—from edge ingress to backend persistence—enforces independent validation controls.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        1. Perimeter Layer                              │
│              (WAF, DDoS Protection, CDN, Rate Limiter)                 │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
┌──────────────────────────────────▼─────────────────────────────────────┐
│                        2. Transport & Gateway                          │
│               (TLS 1.3, API Gateway, CORS, Auth Guard)                 │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
┌──────────────────────────────────▼─────────────────────────────────────┐
│                        3. Application Layer                            │
│     (Spring Security, Role Guards, Tenant Context, Input Sanitation)   │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
┌──────────────────────────────────▼─────────────────────────────────────┐
│                        4. Data & Persistence                           │
│        (AES-256 GCM Field Encryption, KMS/HSM, Audit Logging)          │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Multi-Tenant Security & Context Isolation

NAG uses logical multi-tenancy enforced through mandatory request headers:
- `X-Tenant-Id`: Uniquely identifies the tenant state agency or university.
- `Authorization`: Bearer JWT token encapsulating principal identity, tenant membership, and granted roles.
- `X-Request-Id`: Globally unique correlation ID for end-to-end audit tracing.

### Tenant Context Pipeline
1. **HttpInterceptor (Angular)**: Injects `Authorization`, `X-Tenant-Id`, and `X-Request-Id` headers into all outgoing HTTP calls.
2. **Security Filter (Spring Boot)**: Extracts and verifies JWT signatures against tenant key rings, placing the `TenantContext` into thread-local storage (`SecurityContextHolder`).
3. **Database Isolation**: All queries automatically append `WHERE tenant_id = :tenantId` or enforce PostgreSQL Row-Level Security (RLS).

---

## 3. Authentication & Authorization Framework

### 3.1 Authentication Flow
- **Authentication Standard**: OAuth 2.0 / OpenID Connect (OIDC) via stateless JWT tokens signed with RS256 / Ed25519 asymmetric keys.
- **Session Tokens**: Access tokens (short-lived, 15 minutes), Refresh tokens (HttpOnly, Secure, SameSite=Strict cookies).

### 3.2 Role-Based Access Control (RBAC Matrix)

| Role | Target Capabilities |
|---|---|
| `SUPER_ADMIN` | Global platform configuration, audit log viewing, tenant provisioning |
| `SECURITY_ADMIN` | Key management, security policy enforcement, threat monitoring |
| `EXAM_CONTROLLER` | Exam creation, paper generation, schedule workflow transitions, center allocation |
| `QUESTION_AUTHOR` | Question drafting, subject tagging, item entry |
| `REVIEWER` / `APPROVER` | Question bank review, quality control, item approval |
| `EVALUATOR` | Post-exam subjective answer evaluation and grading |
| `CANDIDATE` | Profile management, online exam delivery session, score viewing |
| `AUDITOR` | Read-only access to immutable audit log streams |

---

## 4. Front-End Security Architecture (Angular)

1. **Standalone Guard Architecture**: `authGuard` and `roleGuard` enforce client-side view permissions before route rendering.
2. **Content Security Policy (CSP)**: Strict headers disabling `unsafe-eval` and restricting script/style sources.
3. **XSS Mitigation**: Angular's built-in DomSanitizer automatically sanitizes dynamic HTML bindings.
4. **Clickjacking Protection**: `X-Frame-Options: DENY` sent on all HTTP responses.

---

## 5. Back-End Security Architecture (Spring Boot)

1. **Stateless Security**: Spring Security configured with `SessionCreationPolicy.STATELESS`.
2. **Input Validation**: `@Valid` and JSR-380 annotations validate DTO inputs at controller boundaries.
3. **Method-Level Security**: `@PreAuthorize("hasRole('EXAM_CONTROLLER')")` guards service methods against unauthorized invocations.
4. **Exception Handling**: Global exception handlers strip internal stack traces from API responses to prevent information leakage.
