# 0005: Keycloak for Centralized Identity & Access Management (IAM)

- **Status**: Accepted
- **Date**: 2026-08-02
- **Deciders**: Security Architecture & Frontend/Backend Leads

---

## 1. Context

NAG serves multiple distinct user personas across state tenants, including Candidates, Invigilators, Question Authors, Reviewers, Exam Controllers, Security Admins, and Auditors.

Key requirements:
- Centralized, standards-compliant OpenID Connect (OIDC) / OAuth 2.0 Identity Provider (IdP).
- Support for Multi-Factor Authentication (MFA), Single Sign-On (SSO), and biometric integration.
- Multi-realm / multi-tenant authentication segregation.
- Fine-grained role and attribute-based token claims (`roles`, `tenantId`, `userId`).

---

## 2. Decision

We choose **Keycloak** as the core Identity and Access Management (IAM) system:

1. **OIDC / JWT Token Issuance**: Keycloak issues short-lived, cryptographically signed JSON Web Tokens (JWT) using RS256 / Ed25519 asymmetric keys.
2. **Realm Isolation**: Separate Keycloak Realms provisioned per tenant state board or administrative tier.
3. **Role & Claim Mapping**: User roles (`EXAM_CONTROLLER`, `SUPER_ADMIN`, `CANDIDATE`, etc.) embedded directly into JWT access token claims.
4. **Spring & Angular Integration**: Frontend uses standard Angular OAuth2 / OIDC library; Spring Boot services validate JWT signatures using Keycloak JWKS endpoints.

---

## 3. Consequences

### Positive
- **Standards Compliance**: Native support for OIDC, OAuth 2.0, SAML 2.0, and FIDO2 / Passkeys.
- **Decoupled Auth Management**: Microservices avoid implementing custom password hashing, user registration, or session management logic.
- **Centralized Security Governance**: Security admins can instantly revoke user tokens or enforce global MFA policies across all modules.

### Negative
- **Single Point of Failure for Authentication**: Keycloak cluster requires high availability (HA) deployment and database clustering to handle peak authentication traffic.
