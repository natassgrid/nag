# Secure Deployment & Infrastructure Hardening — Government Examination Platform

## 1. Container & Kubernetes Hardening

All NAG microservices run in containerized environments (Docker / Kubernetes) enforcing immutable infrastructure patterns.

### Container Security Best Practices
- **Non-Root Execution**: Containers run under dedicated non-root UIDs (`USER 10001`).
- **Minimal Base Images**: Built on Distroless or Alpine minimal base images to minimize attack surface.
- **Read-Only Root Filesystem**: Root filesystem mounted read-only (`readOnlyRootFilesystem: true`); temporary storage mapped to memory (`tmpfs`).
- **Capabilities Removal**: All Linux capabilities dropped (`drop: ["ALL"]`).

```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 10001
  readOnlyRootFilesystem: true
  allowPrivilegeEscalation: false
  capabilities:
    drop:
      - ALL
```

---

## 2. CI/CD Security Pipeline (DevSecOps)

The automated deployment pipeline embeds security scans at every stage:

```
[ Code Commit ] ──> [ SAST (SonarQube/Semgrep) ] ──> [ SCA (Dependency-Check) ]
                                                            │
[ Prod Deployment ] <── [ DAST (ZAP) ] <── [ Container Image Scan (Trivy) ] ◄──┘
```

1. **SAST (Static Application Security Testing)**: Scans source code for vulnerabilities, OWASP Top 10, and code smells on every pull request.
2. **SCA (Software Composition Analysis)**: Checks third-party libraries (Maven/NPM dependencies) against CVE databases. Build fails on High/Critical vulnerabilities.
3. **Container Image Scanning**: Trivy scans container images for OS package vulnerabilities before pushing to production registries.
4. **Secrets Detection**: TruffleHog / GitLeaks prevents accidental hardcoded keys or passwords in commit history.

---

## 3. Network Isolation & Security Groups

- **VPC Segmentation**: Database and KMS nodes reside in isolated Private Subnets with no direct ingress from internet.
- **Egress Filtering**: Backend microservices restricted to explicit outbound whitelist destinations via NetworkPolicies.
- **Web Application Firewall (WAF)**: Edge WAF enforces OWASP Core Rule Set (CRS), blocking SQLi, XSS, and rate limiting requests by IP/Tenant.

---

## 4. Secrets Management

- **Zero Hardcoded Credentials**: API keys, database credentials, and TLS certificates are stored in HashiCorp Vault / AWS Secrets Manager.
- **Dynamic Credentials**: Database passwords dynamically rotated via Vault PostgreSQL engine.
- **Environment Injection**: Secrets injected at container runtime as ephemeral environment variables or mounted `tmpfs` volumes.
