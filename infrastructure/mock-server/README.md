# NAG Mock Third-Party DPI API Server

[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

A lightweight, zero-dependency external gateway simulator for **DigiLocker**, **Aadhaar e-KYC & UIDAI 2.5 Auth**, and **MSG91 SMS Gateway** built for the **National Assessment Grid (NAG)** Open Digital Public Infrastructure (DPI) Platform.

---

## 🎯 Purpose & Features

- **DigiLocker OAuth2 & OpenID Connect Simulation**:
  - **Interactive Consent Screen**: Web browser UI at `/digilocker/oauth/authorize` allowing candidate persona selection, PIN bypass, or allow/deny consent actions with automatic redirect.
  - **OpenID Connect Discovery & JWKS**: `/.well-known/openid-configuration` and `/digilocker/oauth/jwks.json`.
  - **PKCE Token Exchange**: Full support for `grant_type=authorization_code`, `code_challenge` (S256), `code_verifier`, and `refresh_token`, issuing cryptographically signed JWT `id_token` and Bearer `access_token`.
  - **Citizen Profile**: `/digilocker/oauth/userinfo` with Bearer token authentication.
  - **Document Repository**: `/digilocker/v1/user/documents` listing Class X, XII, B.Tech degree, OBC certificates.
  - **Binary Downloads**: Valid streaming PDF & XML certificates (`/digilocker/v1/document/download/:docId`).
  - **Document Verification & Scorecard Push**: `/digilocker/v1/verify` and `/digilocker/v1/credential/push`.

- **Aadhaar UIDAI 2.5 Authentication & e-KYC Simulation**:
  - **UIDAI Auth 2.5 Protocol (`/aadhaar/v2.5/auth`)**:
    - `DEMO` Auth: Match resident Name, DOB, and Gender against official records.
    - `OTP` Auth: Validate transaction or master static OTP `000000`.
    - `BIO` / `FACE` Auth: Biometric score validation for exam center entry and proctoring.
    - Supports both JSON response and official UIDAI XML response (`<AuthRes ret="y" code="00" ... />`).
  - **UIDAI e-KYC 2.5 Protocol (`/aadhaar/v2.5/kyc`)**:
    - Returns digitally signed UIDAI e-KYC XML package (`<KycRes>...<UidData>...</UidData></KycRes>`) or parsed JSON.
  - **OTP Generation & Verification**:
    - `/aadhaar/v1/otp/generate` and `/aadhaar/v1/otp/verify`.
    - Resident personas: Active general, female candidate, OBC/EWS, Biometric locked (`333333333333`), and Suspended (`999999999999`).

- **MSG91 SMS Gateway Simulation**:
  - OTP dispatch (`/api/v5/otp` or `/msg91/api/v5/otp`) matching Spring `Msg91SmsService.java` contracts.
  - In-memory test outbox for end-to-end test verification (`/mock/sms/latest`, `/mock/sms/all`, `/mock/sms/clear`).

- **Chaos & Fault Injection**:
  - Configure latency delays or simulate 500/503 upstream outages via `/mock/chaos` for resilience tests.

---

## 🚀 Quick Start

### 1. Running Standalone

```bash
cd infrastructure/mock-server
npm install
npm test
npm start
```

Default listening port: `8099`.

### 2. Running with Docker Compose

```bash
# Start infrastructure stack with mock server included
docker compose -f infrastructure/docker-compose/docker-compose.yml up -d mock-server
```

---

## 📚 API Endpoints Summary

### DigiLocker Endpoints
| Method | Path | Description |
|---|---|---|
| `GET` | `/digilocker/.well-known/openid-configuration` | OpenID Connect discovery metadata |
| `GET` | `/digilocker/oauth/jwks.json` | Public RSA JSON Web Key Set |
| `GET/POST` | `/digilocker/oauth/authorize` | Interactive Web UI consent or direct OAuth2 redirect |
| `POST` | `/digilocker/oauth/consent` | Processes candidate interactive consent decision |
| `POST` | `/digilocker/oauth/token` | Exchanges authorization code with PKCE for JWT id_token & access_token |
| `GET` | `/digilocker/oauth/userinfo` | Fetches linked citizen profile with Bearer token |
| `GET` | `/digilocker/v1/user/documents` | Lists candidate available certificates & documents |
| `GET` | `/digilocker/v1/document/download/:docId` | Downloads certificate as valid PDF binary or XML |
| `POST` | `/digilocker/v1/verify` | Validates document cryptographic signatures |
| `POST` | `/digilocker/v1/hmac/verify` | Verifies Partner API HMAC-SHA256 signatures |
| `POST` | `/digilocker/v1/credential/push` | Publishes candidate exam scorecard into DigiLocker |

### Aadhaar e-KYC & UIDAI 2.5 Endpoints
| Method | Path | Description |
|---|---|---|
| `POST` | `/aadhaar/v2.5/auth` | UIDAI Auth 2.5 (Demographic, OTP, or Biometric/Face matching) |
| `POST` | `/aadhaar/v2.5/kyc` | UIDAI e-KYC 2.5 returning signed XML or JSON resident demographic payload |
| `POST` | `/aadhaar/v1/otp/generate` | Generates e-KYC OTP (defaults to static `000000`) |
| `POST` | `/aadhaar/v1/otp/verify` | Validates OTP and returns resident demographic payload |
| `GET` | `/aadhaar/personas` | Lists available deterministic test resident personas |

### MSG91 SMS Gateway Endpoints
| Method | Path | Description |
|---|---|---|
| `GET/POST` | `/api/v5/otp` or `/msg91/api/v5/otp` | Sends SMS verification code |
| `GET/POST` | `/api/v5/otp/verify` | Verifies candidate OTP |
| `POST` | `/api/v5/flow` | Dispatches flow campaign SMS |

### Test & Inspection Endpoints
| Method | Path | Description |
|---|---|---|
| `GET` | `/health` | Healthcheck and active mock state counts |
| `GET` | `/mock/sms/latest?mobile=<num>` | Retrieves latest sent SMS for E2E assertions |
| `GET` | `/mock/sms/all` | Lists all dispatched SMS messages in current test run |
| `DELETE` | `/mock/sms/clear` | Flushes test outbox |
| `POST` | `/mock/chaos` | Injects network latency or HTTP error status codes |
| `POST` | `/mock/reset` | Resets all mock databases and fault configurations |

---

## 🧪 Test Resident Personas

| Aadhaar Number | Persona Name | Status / Behavior |
|---|---|---|
| `123456789012` | Aditya Sharma | Active General Category candidate, complete demographic data |
| `111122223333` | Priya Patel | Active Female candidate |
| `777788889999` | Rahul Verma | Active OBC / EWS Category candidate |
| `333333333333` | Locked Biometrics | Returns `423 Locked` (`K-200`) simulating biometric lock |
| `999999999999` | Unknown Citizen | Returns `404 Not Found` (`K-100`) simulating invalid UID |

---

## 🧪 Running Automated Tests

```bash
cd infrastructure/mock-server
npm test
```
