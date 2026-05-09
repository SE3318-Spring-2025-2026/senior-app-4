# Contributing to S.P.M.S.



First of all, thank you for contributing to the "Senior Project Management System" project <3. This document is prepared to standardize our development processes within the team, improve our code quality, and ensure a smooth Review process.

Please read these guidelines carefully before writing any code or opening a Pull Request (PR).

## 1. Branch Strategy

To maintain a clean and understandable Git history in our project, we use the following naming standards:

* **New Features:** `feature/short-feature-name` (e.g., `feature/github-oauth`, `feature/professor-login`)
* **Bug Fixes:** `bugfix/short-bug-name` (e.g., `bugfix/jwt-token-error`)
* **Documentation:** `docs/updated-document` (e.g., `docs/api-yaml-update`)
* **Hotfixes:** `hotfix/critical-error` (Only for emergencies occurring on the main branch)
* **Test:** `test/short-test-name` (e.g., `test/submission-e2e`)

Please do not push code directly to the `main` or `develop` branches.

## 2. Commit Message Standards

We adopt the [Conventional Commits](https://www.conventionalcommits.org/) standard so that our commit messages can be easily read by our AI integration and advisor reviews:

* `feat:` When a new feature is added (e.g., `feat: add professor login page`)
* `fix:` When a bug is resolved (e.g., `fix: resolve db connection timeout`)
* `docs:` When only documentation is changed (e.g., `docs: update setup instructions`)
* `refactor:` Code changes that neither fix a bug nor add a feature
* `style:` Formatting changes that do not affect the execution of the code (white-space, missing semi-colons, etc.)

## 3. Pull Request (PR) and Review Process

This process is of critical importance as our system's grading mechanism (Evaluation Rubric) and AI analysis tools work integrated with PR comments.

1. Once you are done with your branch, open a Pull Request to the `main` (or `develop`) branch.
2. Your PR title should be clear and include the related GitHub Issue number (e.g., `feat: add initial password change form (Resolves #4)`).
3. Briefly explain what you changed and how it can be tested in the PR description.
4. Contact your team leader as the **Reviewer**.
5. A PR cannot be merged without reviewer approval and passing all automated tests/checks.

## 4. Local Setup

Follow the steps below to run the project on your local machine:

1. Clone the repo: `git clone <repo-url>`
2. Install dependencies: `npm install` (or the corresponding command for the backend)
3. Set up environment variables: Copy the `.env.example` file to `.env` and fill in the required values.
4. Start the development server: `npm run dev`

If you have any questions, please reach out to the project team leader.

> **In line with the team retrospective, the following sections (5–10) were unanimously approved as additions to our contribution rules. Thank you to everyone for your work and support!! 💙**

## 5. API Specification Is the Contract

**Why this rule exists:** Multiple PRs in our retrospective shipped code whose DTO field names, response shapes, or status codes did not match the OpenAPI documents under `docs/api/`. The frontend conformed to the spec and the backend conformed to the developer's intuition — integration broke at runtime instead of at review time.

The OpenAPI specifications under `docs/api/` are the binding contract between teams. Treat them as the source of truth, not as a guideline.

* Match field names, types, and `required` lists exactly. A field renamed from spec is a broken contract, even if the code "works" locally.
  * *Example:* If the spec defines a `grade` field on the request body, do not implement it as `score`. The frontend will send `grade`, the backend will reject it with 400, and no one will know why.
* Match response shapes exactly: status codes, payload field names, and the success/error envelope.
  * *Example:* If the spec's `SuccessResponse` schema has `{ "status": "success", "message": "..." }`, do not return `{ "success": true, "message": "..." }`. Clients written against the spec will fail to parse it.
* When the spec re-uses an existing schema, re-use the corresponding DTO. Do not introduce variant DTOs unless the spec explicitly defines them.
  * *Example:* If the spec says both `POST` and `PUT` use `GradeCreateRequest`, do not invent a separate `GradeUpdateRequest` that drops half the fields.
* When the spec lists internal side-effects (notifications, audit log entries, scheduled jobs), implement and test all of them — not just the response.
  * *Example:* If the spec says "auditEntry → D9" on approval, the implementation must write the entry and the test must assert it exists.

## 6. When the Spec Is Wrong, Talk First — Don't Work Around

**Why this rule exists:** In our retrospective we found PRs that admitted in `// TODO` and `// FIXME` comments that the spec said one thing while the code did another. Each of these would have been resolved by a five-minute conversation with the spec owner before any code was written. Silent workarounds become future bugs whose origin no one remembers.

If the specification is incomplete, ambiguous, or in conflict with your issue's acceptance criteria, **stop and resolve the conflict before writing code**.

* Identify the team lead who owns the spec for the affected process and contact them in the team channel.
* Describe the gap precisely: cite the spec section/line, your issue number, and the exact conflict.
  * *Example:* "Issue #169 acceptance criteria says `currentAdviseeCount` decrements by 1 on advisor override, but the User schema in `P4-Advisor-Assignment-api.yaml` does not contain this field. Should I add it to the spec, or is the AC out of date?"
  * *Example:* "Spec § 4.4 doesn't list auto-rejection of sibling pending requests as a side-effect of approval, but my issue is to test that behavior. Is the behavior intended? If yes, the spec needs updating."
* The outcome must be one of two things, both visible to the team:
  1. A spec update lands as its own PR (`docs:` commit), or
  2. The team lead clarifies the intended behavior in writing and the issue's acceptance criteria are amended accordingly.
* **Never** ship code with comments that admit the spec is unmet, such as:
  * `// TODO: implement the real X later`
  * `// FIXME: the spec says Y but we do Z for now`
  * Proxy fields or proxy queries that stand in for missing schema columns.
  * *Example:* Using `groupFormationDeadline` as a stand-in for a not-yet-implemented `gradingDeadline` is not acceptable in a merged PR. Either add the real field in this PR, or block on the spec owner.

## 7. Inline Coordination Markers for Parallel Work

**Why this rule exists:** Several teammates frequently work on overlapping areas in parallel — the same DTOs, the same services, the same frontend pages. Without an explicit signaling mechanism, two branches can edit (or delete) the same file independently and only discover the conflict at merge time, when one team's work is already lost or duplicated.

When you plan to modify a file that other teammates may also touch, leave a structured TODO marker so others see what is in flight before editing.

**Required format:**

```
// TODO(parallel: #<issue>, @<author>, <YYYY-MM-DD>): <one-line description>
//   Affects: <files / DTOs / specs likely to overlap>
//   Coordinate before editing: <Slack handle>
```

**Java example:**

```java
// TODO(parallel: #169, @ahmet, 2026-04-25): adding currentAdviseeCount field to User
//   Affects: User.java, V3 migration, AdvisorAssignmentServiceImpl, P4 spec § Schemas.User
//   Coordinate before editing: @ahmet on Slack
@Entity
public class User { ... }
```

**TypeScript example:**

```ts
// TODO(parallel: #152, @ayse, 2026-04-25): redesigning grade-update form
//   Affects: components/GradeForm.tsx, lib/submissions-api.ts, P3 spec PUT /grades/{id}
//   Coordinate before editing: @ayse on Slack
```

Rules:

* Add the marker at the top of any file you plan to modify heavily — including files you haven't touched yet but will.
* Mandatory file types to mark: anything referenced in an OpenAPI spec, DTOs, services, models, database migrations, frontend route pages.
* **Search before starting a task:** `git grep "TODO(parallel"`. If a marker exists for a file you need to edit, message the marker's author before starting.
* **Remove your own marker** in the same PR that ships the change. Stale markers are noise.
* If you must edit a file someone else has marked, leave a reply marker noting the date you aligned with them:

```
// TODO(parallel-conflict: #169 ↔ #173): @mehmet also editing this file. Aligned on 2026-04-26.
```

This rule is the cheapest insurance against silent merge conflicts and duplicated work.

## 8. Keep Branches Fresh

**Why this rule exists:** A PR in our retrospective showed 18 changed files in GitHub's diff view, when the author had actually changed only 1. The other 17 were phantom files caused by the branch lagging behind `main`. Reviewers spent the first cycle decoding the noise instead of evaluating the change.

A pull request whose branch lags behind `main` is hard to review and easy to mis-merge.

* Rebase your branch onto `origin/main` before requesting review, and again whenever it has been open for more than a few days.
* Use `git push --force-with-lease` (never plain `--force`).
  * *Example flow:*
    ```bash
    git fetch origin
    git rebase origin/main
    # resolve conflicts
    git push --force-with-lease
    ```
* As a reviewer, run `git diff origin/main...origin/<branch> --stat` first. If the file list contains anything the author did not intend to change, request a rebase before reading code.

A diff with phantom files is not a reviewable PR.

## 9. Test What the Spec Promises

**Why this rule exists:** We saw tests that pinned implementation details rather than the spec's contract — string-matching on notification messages, "or"-ing two status codes together to mask uncertainty, and entire side-effects (audit logs, notifications) going unverified. Tests like these break on small refactors and let real regressions slip through.

Tests should pin down the spec's contract, not internal implementation details.

* Assert on structured fields (enum values, IDs, status fields) rather than message strings or human-readable text.
  * *Example:* Replace `notification.message.contains("cancelled")` with `notification.type == ADVISOR_OVERRIDE_CANCELLED`. Introduce the enum value if it doesn't exist. String-matching breaks the moment we localize the UI.
* Every side-effect listed in the spec — notifications, audit log entries, status transitions — needs a corresponding assertion.
  * *Example:* If the spec § *Internal Side-Effects* says "auditEntry → D9", the test must read `auditLogRepository` and assert the entry exists with the correct `actionType`. If it says "notification to group leader (D8)", assert it.
* Status code assertions must match the spec exactly. Do not "or" together two codes to make a test pass.
  * *Example:* `either(equalTo(400)).or(equalTo(429))` is acceptable only if both codes appear in the spec's response block. Otherwise pick the spec-conformant one, or update the spec.
* Use the shared test infrastructure (factories, helpers, base classes) instead of inlining setup logic.
  * *Example:* If `TestDataFactory.createCoordinator(...)` exists, use it. Don't `new User()` inline in your test — duplicated setup drifts silently as the codebase evolves.
* Never merge a test that contains `FIXME`, skipped assertions, or `System.err.println` warnings explaining what wasn't verified.
  * *Example:* A test that prints `"WARNING: skipped because not implemented"` and lets execution continue is not a test — it is a placeholder. Either implement the missing behavior or open a follow-up issue and remove the placeholder before merge.

## 10. PR Author & Reviewer Checklist

**Why this rule exists:** Sections 5–9 only help if they are actually applied. A short checklist keeps both authors and reviewers honest and gives the team a single place to look during review.

**Before opening a PR (Author):**

* [ ] Branch is rebased onto current `main` (Section 8).
* [ ] Every changed DTO field, response shape, and status code matches the spec (Section 5).
* [ ] Spec gaps were resolved with the relevant team lead before coding (Section 6).
* [ ] Side-effects listed in the spec are implemented and tested (Section 9).
* [ ] No `TODO`/`FIXME` admitting partial implementation remains in shipped code (Section 6).
* [ ] My own `TODO(parallel: ...)` markers are removed (Section 7).
* [ ] Tests use shared factories and structural assertions (Section 9).

**Before approving (Reviewer):**

* [ ] `git diff origin/main...origin/<branch> --stat` shows only files the author meant to change (Section 8).
* [ ] DTO field names cross-checked against the spec line by line (Section 5).
* [ ] No "real implementation later" comments in shipped code (Section 6).
* [ ] Spec changes (if any) shipped here or in a linked PR (Section 6).
* [ ] All spec-mandated side-effects have corresponding test assertions (Section 9).