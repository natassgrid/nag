# DevSecOps Pipeline - Source, SAST, Secrets and SCA Layers

Part of the layered DevSecOps framework tracked in #279. This page covers the first four layers (#282-#285).

| Layer | Issue | Tool | Where | Gate |
| :--- | :--- | :--- | :--- | :--- |
| Source & code review | #282 | Branch protection, CODEOWNERS, PR template | `.github/CODEOWNERS`, `.github/pull_request_template.md` | Required reviews + signed commits + status checks |
| SAST | #283 | Semgrep (Java, Spring, TypeScript, OWASP Top 10) | `ci.yml` job `semgrep`, `frontend-ci.yml` job `semgrep-frontend` | Fails on `ERROR` severity; SARIF uploaded to code scanning |
| Secrets detection | #284 | Gitleaks | `.github/workflows/gitleaks.yml`, `.gitleaks.toml`, `.pre-commit-config.yaml` | Fails on any detected secret in the PR / pushed commits |
| SCA | #285 | OWASP Dependency-Check + Dependabot | `ci.yml` job `dependency-check`, `.github/dependabot.yml` | Fails on CVSS ≥ 7; weekly update PRs |

## 1. Branch Protection (#282)

Branch protection is a repository setting and cannot be committed as a file. Apply it once (admin required) for both `main` and `develop`:

```bash
for BRANCH in main develop; do
  gh api -X PUT repos/natassgrid/nag/branches/$BRANCH/protection --input - <<'EOF'
  {
    "required_status_checks": {
      "strict": true,
      "contexts": [
        "Build",
        "Unit & Integration Tests",
        "SAST (SpotBugs)",
        "SAST (Semgrep)",
        "SAST – Semgrep (TypeScript / Angular)",
        "OWASP Dependency-Check",
        "Gitleaks",
        "Lint, Test & Build Frontend Workspace"
      ]
    },
    "enforce_admins": false,
    "required_pull_request_reviews": {
      "required_approving_review_count": 1,
      "require_code_owner_reviews": true,
      "dismiss_stale_reviews": true
    },
    "restrictions": null,
    "required_linear_history": false,
    "allow_force_pushes": false,
    "allow_deletions": false,
    "required_conversation_resolution": true
  }
EOF
  # Require signed commits
  gh api -X POST repos/natassgrid/nag/branches/$BRANCH/protection/required_signatures \
    -H "Accept: application/vnd.github+json"
done
```

### Enabling Secret Scanning & Push Protection

Enable in **Settings → Code security and analysis**:
- Dependabot alerts: **On**
- Dependabot security updates: **On**
- Secret scanning: **On**
- Push protection: **On** (blocks pushes containing detected secrets)

### Commit Signing

All contributors must sign commits with GPG or SSH. See
[GitHub docs – commit signature verification](https://docs.github.com/authentication/managing-commit-signature-verification).

Quick-start with SSH signing:
```bash
git config --global gpg.format ssh
git config --global user.signingkey ~/.ssh/id_ed25519.pub
git config --global commit.gpgsign true
```

### CODEOWNERS

`.github/CODEOWNERS` assigns `@sheelprabhakar` as the default reviewer and mandatory reviewer for security-sensitive paths (`.github/`, `identity-service/`, `shared-lib/`, `infrastructure/`, Dockerfiles, and DevSecOps config files).

## 2. SAST – Semgrep (#283)

Two Semgrep jobs run on every push and PR:

| Job | Workflow | Scope | Rulesets |
| :--- | :--- | :--- | :--- |
| `semgrep` | `ci.yml` | Entire repository (Java + TypeScript) | `p/java`, `p/spring`, `p/typescript`, `p/javascript`, `p/owasp-top-ten`, `p/secrets` |
| `semgrep-frontend` | `frontend-ci.yml` | `nag-frontend-workspace/` only | `p/typescript`, `p/javascript`, `p/owasp-top-ten`, `p/secrets` |

Both jobs use the pinned `semgrep/semgrep:1.97.0` container image, fail on `ERROR` severity (`--severity ERROR --error`), and upload SARIF results to GitHub code scanning.

### Local scan

```bash
docker run --rm -v "$PWD:/src" semgrep/semgrep:1.97.0 \
  semgrep scan \
    --config p/java --config p/spring \
    --config p/typescript --config p/owasp-top-ten \
    --severity ERROR /src
```

### Suppressing false positives

Add `// nosemgrep: <rule-id>` on the offending line with a justification comment:
```java
String query = "SELECT * FROM users WHERE id = " + id; // nosemgrep: java.lang.security.audit.sqli.jdbc-sqli.jdbc-sqli (parameterized query used in calling code)
```

Excluded paths are listed in `.semgrepignore` (test files, generated code, build output, docs).

## 3. Secrets Detection – Gitleaks (#284)

### CI gate (`gitleaks.yml`)

- **Pull request**: scans only the commits in the PR range (`base..head`).
- **Push to main/develop**: scans only the pushed commit range (`before..sha`).
- **Manual dispatch with `full_history=true`**: scans the entire git history.
- Pinned version: `8.21.2`.
- SARIF results are uploaded to GitHub code scanning.
- Config: `.gitleaks.toml` extends the default ruleset and allows lock files, `.env.example`, build/dist output, and obvious placeholder strings.

### Pre-commit hook (developer local)

Install once per clone:
```bash
pip install pre-commit
pre-commit install
```

On every `git commit`, Gitleaks scans the staged changes. The hook uses the same `.gitleaks.toml` config.

### Secret detected — response procedure

1. **Rotate the credential immediately** (revoke the old value at the service provider).
2. Remove the secret from the code / history (use `git filter-repo` or BFG Repo Cleaner).
3. Only add a `.gitleaks.toml` `allowlist` entry for *confirmed false positives*, with a comment explaining why.

## 4. SCA – Dependency-Check + Dependabot (#285)

### OWASP Dependency-Check CI job

- Runs on every push/PR via `ci.yml` `dependency-check` job.
- **Fail threshold**: CVSS ≥ 7 (`--failOnCVSS 7`).
- Suppression file: `.github/dependency-check-suppressions.xml`.
  - Every suppression requires a `<notes>` justification and an `until` expiry date (max 90 days). See [vulnerability-management.md](vulnerability-management.md).
- Reports (HTML, JSON, SARIF) are uploaded as artifacts and SARIF is pushed to GitHub code scanning.
- Set the optional `NVD_API_KEY` repository secret to speed up NVD database updates.

### Dependabot

`.github/dependabot.yml` opens weekly PRs against `develop` for:

| Ecosystem | Directory | Groups |
| :--- | :--- | :--- |
| Gradle | `/` | `spring`, `minor-and-patch` |
| npm | `/nag-frontend-workspace` | `angular`, `nx`, `minor-and-patch` |
| npm | `/infrastructure/mock-server` | — |
| GitHub Actions | `/` | — |
| Docker | `/backend` | — |
| Docker | `/nag-frontend-workspace` | — |

Dependabot PRs are labelled `dependencies` and/or `security` and must pass all status checks before merge.

## 5. Verification Evidence

| Layer | How to verify |
| :--- | :--- |
| Branch protection | `gh api repos/natassgrid/nag/branches/main/protection` |
| Signed commits | `git log --show-signature -1` |
| Semgrep | Run job `semgrep` on any PR; check *Security → Code scanning* tab |
| Gitleaks | Run `gitleaks.yml` workflow (manual dispatch); check *Actions* tab |
| Dependency-Check | Run `dependency-check` job; download artifact `dependency-check-report` |
| Dependabot | Check *Insights → Dependency graph → Dependabot* tab after enabling |
