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

## Rebase checklist (run before opening PR)

- [ ] `git fetch origin && git rebase origin/main`
- [ ] Resolve conflicts; prefer real Process 2 implementation over mock.
- [ ] `git grep "TODO(P2-dep"` — address every hit.
- [ ] Create a real `@Service` impl of `AdvisorRequestDetailService` (no `@Profile`) once P2's `AdvisorRequest` entity is stable.
- [ ] Delete `backend/src/main/java/com/spms/backend/mocks/MockAdvisorRequestDetailService.java`.
- [ ] Remove `spring.profiles.active=dev-mock` from local `backend/application.properties` (or verify it was never committed).
- [ ] Run `mvn test` (backend) and `npm run build` (frontend).
- [ ] Delete this `DEPENDENCIES.md` file, commit, then open the PR.
