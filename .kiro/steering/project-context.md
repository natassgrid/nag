# Open Digital Public Infrastructure (DPI) Platform — Project Context

## Project Overview

This is a large-scale, secure, multilingual, cloud-native examination platform designed to support national/state/university/certification/recruitment examinations across India. It targets 500,000 concurrent candidates, 5 million registered users, and 100 million questions with 99.99% availability.

## Architecture

- **Architecture Style**: Domain-driven microservices monorepo
- **Backend**: Java 21 / Spring Boot 4.1.0 with virtual threads (Project Loom)
- **Frontend**: Angular 21 SPA (WCAG 2.2 AA compliant)
- **Database**: PostgreSQL 16 with per-service schemas (single cluster)
- **Cache**: Redis Cluster (session/rate-limiting/hot state)
- **Messaging**: Apache Kafka (domain events, RPO=0)
- **IAM**: Keycloak (OAuth2/OIDC, FIDO2/WebAuthn, RBAC/ABAC)
- **HSM**: HashiCorp Vault Transit Engine (key management, signing)
- **Search**: OpenSearch (100M question full-text search)
- **Observability**: Prometheus + Grafana + OpenTelemetry (Jaeger) + Loki
- **Container**: Docker + Kubernetes (Helm), vendor-neutral

## Build System

- **Build Tool**: Gradle (multi-module, `settings.gradle` at root)
- **Java Toolchain**: Java 21, Temurin distribution
- **Spring Boot**: 4.1.0
- **Spring Cloud**: 2025.0.2
- **Spring AI**: 2.0.0 (OpenAI-compatible client → LiteLLM gateway)
- **Lombok**: FreeFair plugin 9.5.0
- **Test Framework**: JUnit 5 + jqwik (property-based testing)
- **CI**: GitHub Actions (build → unit test → integration test → SAST → DAST → container build → deploy)

## Monorepo Structure

```
/
├── backend/
│   ├── shared-lib/              # Common: ApiResponse, BaseEntity, enums, security configs
│   ├── identity-service/        # Auth, MFA, RBAC/ABAC, session, rate limiting
│   ├── candidate-service/       # PII (AES-256), DigiLocker, face verification
│   ├── question-bank-service/   # CRUD, versioning, lifecycle FSM, similarity, exposure
│   ├── translation-service/     # 22 languages, translation workflow
│   ├── examination-service/     # Exam config, sections, marking, navigation
│   ├── paper-generator/         # Blueprint assembly, HSM encryption, stats
│   ├── delivery-service/        # Session, question serving, proctoring, offline
│   ├── response-service/        # Sub-200ms persistence, auto-save, offline sync
│   ├── evaluation-service/      # Auto-eval MCQ, partial marks, dual-evaluator
│   ├── result-service/          # Scores, ranks, normalization, PDF, DigiLocker
│   ├── audit-service/           # Append-only HSM-signed events, 7-year retention
│   ├── notification-service/    # Email/push, retry, in-app notifications
│   ├── admin-service/           # Multi-tenancy, user mgmt, config API
│   ├── analytics-service/       # Difficulty/discrimination indices, dashboards
│   └── api-gateway/             # Spring Cloud Gateway, OAuth2, rate limit, WAF
├── frontend/                    # Angular SPA
├── infrastructure/
│   ├── docker-compose/          # Local dev stack (Postgres, Kafka, Redis, Keycloak, Vault, monitoring)
│   ├── helm/                    # Kubernetes Helm charts
│   ├── kafka/                   # Kafka configuration
│   ├── observability/           # Monitoring configs
│   ├── postgres/                # DB migrations/configs
│   └── redis/                   # Redis configs
└── docs/                        # Project documentation
```

## Shared Library (`backend/shared-lib`)

Package: `com.examplatform.shared`

Key classes:
- `api/ApiResponse.java` — Standard envelope: `{ status, data, error, pagination }`
- `api/ExamPlatformProblemDetail.java` — RFC 7807 error responses
- `audit/AuditEventType.java` — Enum of all audit event types
- `entity/BaseEntity.java` — JPA `@MappedSuperclass` (UUID PK, timestamps)
- `lifecycle/` — State enums: `QuestionState`, `PaperState`, `SessionState`, `TranslationState`, `EvaluationState`
- `tenant/TenantContext.java` — ThreadLocal tenant isolation
- `security/JwtAuthConfig.java` — OAuth2 Resource Server configuration
- `security/DevJwtConfig.java` — Development JWT bypass
- `auth/ServiceAccountTokenProvider.java` — Zero Trust service-to-service JWT
- `error/ProblemDetailBuilder.java` — Fluent builder for RFC 7807 errors

## Key Conventions

1. **Package Structure**: `com.examplatform.<domain>` (e.g., `com.examplatform.identity`, `com.examplatform.admin`)
2. **Database Schemas**: Each service has its own schema (e.g., `identity_service`, `candidate_service`, `question_service`)
3. **Migrations**: Flyway, placed in `src/main/resources/db/migration/`
4. **API Prefix**: All REST endpoints start with `/api/v1/<service-prefix>/`
5. **Kafka Topics**: Namespaced as `exam.<domain>.events` (e.g., `exam.audit.events`, `exam.session.events`)
6. **Audit**: Every significant action publishes to `exam.audit.events` Kafka topic
7. **Encryption**: PII and question content encrypted at column level with AES-256, keys managed by Vault
8. **Error Handling**: RFC 7807 ProblemDetail responses
9. **Response Envelope**: `ApiResponse<T>` with status, data, error, pagination
10. **Multi-tenancy**: `X-Tenant-Id` header, `tenantId` column on entities
11. **Authentication**: OAuth2 JWT tokens, validated by each service via Keycloak JWKS
12. **Profiles**: `application.yml` (default), `application-docker.yml` (Docker Compose)

## Frontend Conventions (Angular 21)

1. **Component style**: Standalone components only — NO NgModules
2. **State management**: RxJS Observables only — NO signals, NO NgRx
3. **UI library**: Angular Material 21
4. **File structure**: Every component MUST have separate files for template, styles, and logic. Use `templateUrl: './component-name.component.html'` and `styleUrls: ['./component-name.component.scss']`. NEVER use inline `template:` or `styles:[]` in the `@Component` decorator. Each component directory contains: `component-name.component.ts`, `component-name.component.html`, `component-name.component.scss`.
5. **List pages**: Always use the shared `PaginatedTableComponent` with a `fetcher: PaginatedDataFetcher<T>` function. The fetcher returns `Observable<PaginatedResponse<T>>`. The paginated table handles loading, empty state, search, pagination, and change detection internally. Never use manual `loading` flags or `ChangeDetectorRef` for list data — let the table handle it.
5. **Service pattern**: `@Injectable({ providedIn: 'root' })`. All API responses are wrapped in `ApiResponse<T>` by the backend. For paginated list endpoints, the backend returns `Page<T>` (Spring Data) which serializes as `{ content: [], totalElements, totalPages, size, number }`. Services unwrap via `.pipe(map(res => res?.data?.content ?? res?.data ?? []))` for arrays. Always pass `page`, `size`, and `search` query params to the backend — never do client-side pagination. The fetcher in the component passes `req.page`, `req.size`, `req.search` directly to the service method.
6. **Server-side pagination (backend)**: All list/search endpoints MUST accept `?page=0&size=20&search=` query params and return Spring `Page<T>`. Use `PageRequest.of(page, size, Sort.by(...))` in the service layer with Spring Data JPA paginated repository methods (`findBy...(... , Pageable pageable)` returning `Page<T>`). The controller returns `ApiResponse<Page<ResponseDTO>>`. This ensures the browser Network tab always shows pagination query params.
7. **Form dialogs**: Use `MatDialog` with `MAT_DIALOG_DATA` injection. Dialog closes with `dialogRef.close(formValue)`. Parent component subscribes to `afterClosed()` and calls the service, then `this.table.reload()`.
8. **Confirmation dialogs**: Never use browser `confirm()`. Use the shared `ConfirmDialogComponent` at `shared/components/confirm-dialog/confirm-dialog.component.ts`. It accepts `ConfirmDialogData { title, message, confirmText, cancelText, color, icon }` and returns `boolean` on close.
9. **Snackbar feedback**: `this.snackBar.open('Message', 'OK', { duration: 3000 })` on success; `this.snackBar.open(err?.error?.message || 'Error', 'Dismiss', { duration: 4000 })` on error.
8. **DatePicker**: Always set `[min]="minDate"` where `minDate = new Date()` to allow only future dates. Always convert to ISO string before sending: `d instanceof Date ? d.toISOString().split('T')[0] : d`.
9. **Cascading dropdowns**: Use `valueChanges` subscription on parent control → clear child + load options → auto-set denormalized name field. See `centre-form-dialog.component.ts` for reference.
10. **Routing**: Lazy-load features via `loadChildren` or `loadComponent`. Use `roleGuard` with `data: { roles: [...] }`. All scheduling routes are under `/exam/scheduling/`.
11. **Proxy**: Angular dev server proxies `/api` → `http://localhost:9000` (API gateway). All service base URLs are relative (e.g., `/api/v1/examinations`).
12. **Imports**: Every standalone component explicitly imports all Material modules it uses in its `imports` array.

## Local Development

- **Start Infrastructure**: `docker compose -f infrastructure/docker-compose/docker-compose.yml up -d`
- **Start Services**: `docker compose -f infrastructure/docker-compose/docker-compose.services.yml up -d`
- **Build All**: `./gradlew build -x test`
- **Run Tests**: `./gradlew test`
- **Run Single Service**: `./gradlew :backend:<service-name>:bootRun`

## Infrastructure Ports (Local Dev)

| Service     | Port  |
|-------------|-------|
| PostgreSQL  | 5432  |
| Kafka       | 29092 (external) / 9092 (internal) |
| Redis       | 6379  |
| Keycloak    | 8080  |
| Vault       | 8200  |
| Prometheus  | 9090  |
| Grafana     | 3000  |
| Jaeger UI   | 16686 |
| OTLP gRPC   | 4317  |
| OTLP HTTP   | 4318  |

## Security Principles

- **Zero Trust**: All inter-service calls use service-account JWTs
- **HSM Key Hierarchy**: Root → Platform Master Key → Service Keys → Per-Entity DEKs
- **Audit Tamper Evidence**: SHA-256 hash + ECDSA P-256 HSM signature on every event
- **Encryption at Rest**: AES-256 for PII, question content, paper packages
- **Rate Limiting**: Redis token-bucket at gateway and per-service
- **RBAC/ABAC**: 10 named roles with attribute-based fine-grained access control

## Remaining Tasks (Property Tests)

The following property tests are pending implementation (marked `[ ]*` in tasks.md):
- 2.9: Authentication rate limiting property test
- 3.7: Candidate PII encryption at rest property test
- 4.5: Question lifecycle state machine property test
- 4.8: Question similarity rejection property test
- 6.3: Examination section marks validation property test
- 7.3: Blueprint constraint satisfaction property test
- 7.5: Shift paper statistical comparability property test
- 7.8: Paper serialization round-trip property test
- 7.9: Paper schema validation rejects invalid documents property test
- 10.3: Response persistence round-trip property test
- 10.7: Response revision history preservation property test
- 11.4: Partial marking arithmetic correctness property test
- 12.3: Result score decomposition invariant property test
- 13.3: Audit event tamper detection property test
