# Contributing to the Open Source Government Examination Platform

Thank you for your interest in contributing! This document describes the process for reporting issues, proposing changes, and submitting pull requests.

## Code of Conduct

All contributors are expected to uphold the [Contributor Covenant Code of Conduct](https://www.contributor-covenant.org/version/2/1/code_of_conduct/). Be respectful, inclusive, and constructive.

## How to Contribute

### Reporting Bugs

1. Search existing [GitHub Issues](../../issues) to confirm the bug has not already been reported.
2. Open a new issue using the **Bug Report** template and include:
   - Steps to reproduce
   - Expected behaviour
   - Actual behaviour
   - Logs, screenshots, or stack traces (remove any PII or secrets before posting)
   - Environment details (OS, Java version, Spring Boot version)

### Proposing Features

1. Open a **Feature Request** issue describing the problem and proposed solution.
2. Discuss the proposal with maintainers before starting work to avoid duplicated effort.

### Submitting Pull Requests

1. Fork the repository and create a feature branch from `main`:
   ```
   git checkout -b feature/short-description
   ```
2. Follow the coding standards described below.
3. Write or update unit tests and property-based tests for all changed logic.
4. Ensure all CI checks pass locally before pushing:
   ```
   mvn verify -pl backend/<module> -am
   ```
5. Open a pull request against `main` with a clear description of the change, linked issues, and testing evidence.
6. At least one maintainer approval is required before merge.

## Coding Standards

- **Java 21 / Spring Boot 3.3.x** — use virtual threads (`spring.threads.virtual.enabled=true`) for I/O-bound work.
- Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
- All public APIs must carry Javadoc.
- No commented-out code in merged PRs.
- Maximum method length: 40 lines; maximum class length: 400 lines.
- All secrets and credentials must be injected via environment variables or HashiCorp Vault — never hardcoded.

## Security Vulnerabilities

Do **not** report security vulnerabilities as public GitHub Issues. Follow the instructions in [SECURITY.md](SECURITY.md).

## Licensing

By submitting a contribution you agree that your work is licensed under the [Apache License 2.0](LICENSE) and that you have the right to grant this license.
