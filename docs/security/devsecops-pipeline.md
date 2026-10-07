# DevSecOps Pipeline - Source, SAST, Secrets and SCA Layers

Part of the layered DevSecOps framework tracked in #279. This page covers the first layers (#282-#285).

| Layer | Tool | Where | Gate |
| :--- | :--- | :--- | :--- |
| Source & code review | Branch protection, CODEOWNERS, PR template | `.github/CODEOWNERS`, `.github/pull_request_template.md` | Required reviews + status checks |
| SAST | Semgrep (Java, Spring, TypeScript, OWASP Top 10) | `ci.yml` job `semgrep` | Fails on `ERROR` severity; SARIF uploaded to code scanning |
| Secrets detection | Gitleaks | `.github/workflows/gitleaks.yml`, `.gitleaks.toml`, `.pre-commit-config.yaml` | Fails on any detected secret in the PR / pushed commits |
| SCA | OWASP Dependency-Check + Dependabot | `ci.yml` job `dependency-check`, `.github/dependabot.yml` | Fails on CVSS >= 7; weekly update PRs |

## 1. Branch protection (#282)

Branch protection is a repository setting and cannot be committed. Apply it once (admin required) for `main` and `develop`:

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
gh api -X POST repos/natassgrid/nag/branches/$BRANCH/protection/required_signatures \
  -H "Accept: application/vnd.github+json"
done
```

Contributors must configure commit signing (GPG or SSH): <https://docs.github.com/authentication/managing-commit-signature-verification>.

## 2. SAST - Semgrep (#283)

- Rulesets: `p/java`, `p/spring`, `p/typescript`, `p/javascript`, `p/owasp-top-ten`, `p/secrets`.
- Excluded paths: see `.semgrepignore` (tests, generated code, build output, docs).
- Run locally: `docker run --rm -v "$PWD:/src" semgrep/semgrep semgrep scan --config p/java --config p/typescript --severity ERROR /src`.
- Suppress a false positive inline with `// nosemgrep: <rule-id>` and a justification comment.

## 3. Secrets detection - Gitleaks (#284)

- CI scans the commit range of a PR / push (full history on manual `workflow_dispatch` with `full_history=true`).
- Local pre-commit hook:
  ```bash
  pip install pre-commit && pre-commit install
  ```
- Config: `.gitleaks.toml` extends the default rules and allows lock files, `.env.example`, build output and obvious placeholders.
- If a secret is detected: **rotate it first**, then remove it from the code. Only add a `.gitleaks.toml` allowlist entry for confirmed false positives.

## 4. SCA - Dependency-Check + Dependabot (#285)

- Dependency-Check fails the build at CVSS >= 7 and uploads SARIF + HTML/JSON reports.
- Suppressions live in `.github/dependency-check-suppressions.xml`; each needs a `<notes>` justification and an `until` expiry (max 90 days).
- Dependabot opens weekly PRs against `develop` for Gradle, npm (frontend workspace, mock server), GitHub Actions and Docker base images.
- Optionally set the `NVD_API_KEY` repository secret to speed up Dependency-Check NVD updates.

## Required repository settings (manual)

- Enable **Dependabot alerts / security updates** and **Secret scanning + push protection** (Settings -> Code security).
