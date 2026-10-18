# Unified Exam Spine: Subagent Execution Plan

Branch: `feat/unified-exam-spine` (from `develop`). Design source: [unified-exam-spine-plan.md](unified-exam-spine-plan.md).

All agents share one working tree, so each wave assigns disjoint directories. Agents commit only their own paths and must not run `git checkout`, `git stash` or `git reset`. They build only their own modules. Wave exit requires a green build of the touched modules before the next wave starts.

| Wave | Agent (model) | Plan phases | Owned paths |
|---|---|---|---|
| 1 | W1-contracts (Pro) | 0 | `backend/shared-lib`, `docs/adr` |
| 2 | W2-taxonomy-questions (Pro) | 1, 2, 5 (backend) | `backend/question-bank-service` |
| 2 | W2-exam-blueprint (Pro) | 3, 7 (paper) | `backend/examination-service`, `backend/paper-generator` |
| 2 | W2-learner-engine (Pro) | 4, 6 (backend) | `backend/candidate-service`, `backend/recommendation-service`, `backend/practice-service` |
| 3 | W3-delivery-eval (Flash) | 7 (delivery, evaluation, result) | `backend/delivery-service`, `backend/evaluation-service`, `backend/result-service`, macro/monolith app wiring |
| 4 | W4-frontend-candidate (Pro) | 4, 5, 6, 8 (candidate) | `nag-frontend-workspace/apps/candidate-delivery` and its libs |
| 4 | W4-frontend-admin (Pro) | 8 (admin) | `nag-frontend-workspace/apps/admin-portal` and `libs/{questions,examinations}` |
| 5 | W5-verify (Pro) | 9 | tests, `npx nx run-many -t build --skip-nx-cache`, Gradle build, Docker builds with `--no-cache` |

Model choice: Pro for schema and domain design work, Flash for mechanical propagation of ids through DTOs.
