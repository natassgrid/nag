# 0006: REST API Versioning Strategy

- **Status**: Accepted
- **Date**: 2026-08-02
- **Deciders**: API Design & Frontend/Backend Leads

---

## 1. Context

As the platform evolves over multiple state deployments and version cycles, backend REST APIs will undergo updates, schema additions, and structural enhancements. Breaking changes without a clear versioning strategy would disrupt live Angular frontend clients and third-party integration connectors.

Key goals:
- Predictable, explicit API contract versioning.
- Support for concurrent version coexistence during major migration windows.
- Clear deprecation lifecycle and communication policies.

---

## 2. Decision

We adopt **URI Path Versioning** (`/api/v1/...`) for all external and internal REST endpoints:

1. **URI Path Convention**: Every REST endpoint must prefix its base path with explicit major version identifier (e.g., `/api/v1/examinations`, `/api/v1/questions`, `/api/v1/centres`).
2. **Backward Compatible Minor Changes**: Field additions, optional parameters, and new endpoints within the same major version must maintain backward compatibility and do not bump the path version.
3. **Major Version Bumps**: Breaking changes (e.g., field removals, structural schema refactoring, altered parameter semantics) require introducing a new path version (e.g., `/api/v2/...`).
4. **Deprecation Header Policy**: Deprecated endpoints return HTTP headers `Sunset: <Date>` and `Deprecation: true` for a minimum 180-day grace period before removal.

---

## 3. Consequences

### Positive
- **Explicit & Cache-Friendly**: URI path versioning is explicitly visible in HTTP logs, API documentation (OpenAPI / Swagger), and web caches.
- **Client Stability**: Front-end applications and external state connectors remain stable when minor API extensions are deployed.
- **Clear Migration Paths**: Multiple API versions can coexist in backend controllers during transition windows.

### Negative
- **Code Duplication During Transitions**: Maintaining parallel `/v1/` and `/v2/` controller endpoints requires adapter mapping layers until legacy endpoints are fully sunset.
