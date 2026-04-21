# DEPENDENCIES — feature/issue-159-advisor-request-detail

## Upstream issues this branch blocks on

- P2-API-14 to P2-API-22 (advisor-request workflow with auto-reject) — GitHub issues #14–22 area

## Active TODO(P2-dep) markers

- `backend/src/main/java/com/spms/backend/mocks/MockAdvisorRequestDetailService.java:15` — mock service declaration
- `backend/src/main/java/com/spms/backend/controller/AdvisorRequestController.java:30` — call-site in getAdvisorRequestDetail

## Active mock configurations

- `backend/src/main/java/com/spms/backend/mocks/MockAdvisorRequestDetailService.java`
  — replaces `AdvisorRequestDetailService` until P2 advisor-request workflow merges.
  — Uses `AdvisorRequestRepository` and `GroupRepository` (both available on this branch).

## Database note — `advisor_requests` table

This branch created the `advisor_requests` table manually in the shared Supabase dev DB
because the endpoint needs the table to exist and P2's workflow is not yet merged.

**P2 team:** the table already exists. When implementing the advisor-request creation
workflow (P2-API-14–22), extend this table with any additional columns you need rather than
recreating it. Coordinate schema changes with this branch's owner if needed.

**After P2 merges (rebase):** verify the `AdvisorRequest` entity on this branch is compatible
with P2's final schema. Apply any missing columns in Supabase directly.

## Rebase checklist (run before opening PR)

- [ ] `git fetch origin && git rebase origin/main`
- [ ] Resolve conflicts; prefer real Process 2 implementation over mock.
- [ ] `git grep "TODO(P2-dep"` — address every hit.
- [ ] Create a real `@Service` impl of `AdvisorRequestDetailService` (no `@Profile`) once P2's `AdvisorRequest` entity is stable.
- [ ] Delete `backend/src/main/java/com/spms/backend/mocks/MockAdvisorRequestDetailService.java`.
- [ ] Remove `@ActiveProfiles("dev-mock")` from `AdvisorRequestDetailApiTest` and `AdvisorRequestDetailServiceTest`.
- [ ] Verify `AdvisorRequest` entity fields match P2's final schema; update if needed.
- [ ] Re-examine `AdvisorRequestDetailApiTest` and `AdvisorRequestDetailServiceTest` — update
      any fixture or assertion that relied on the mock or the temporary schema.
- [ ] Remove `spring.profiles.active=dev-mock` from local `backend/application.properties` (or verify it was never committed).
- [ ] Run `mvn test` (backend) and `npm run build` (frontend).
- [ ] Delete this `DEPENDENCIES.md` file, commit, then open the PR.
