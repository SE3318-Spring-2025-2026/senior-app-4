# Process 5.4 — Assign Groups to Committee

Backend implementation for assigning student groups to committees, including
schedule validation (P5.5), state-machine status tracking, audit logging (P5.8),
and group-member notifications (P5.7).

## What was added / changed

### Entity & repository
- `model/GroupCommitteeAssignment.java` — added `@UniqueConstraint(committee_id, group_id)`,
  added a lazy `@ManyToOne Group`, indexes on committee/group/exam_date, and
  status constants (`ASSIGNED`, `SCHEDULED`, `COMPLETED`, `CANCELLED`).
- `repository/GroupCommitteeAssignmentRepository.java` — finder methods
  (`findByCommittee_CommitteeId`, `findByGroupId`,
  `existsByCommittee_CommitteeIdAndGroupId`) and conflict-window queries
  (`findConflictingByCommitteeAndWindow`, `findConflictingByWindow`).
- `repository/CommitteeRepository.java` — new (was missing).

### Validation
- `service/ScheduleValidator.java` — P5.5 conflict detector.
  - `hasScheduleConflict(committeeId, examDate)` — boolean check (±2 h window).
  - `validateExamDate(date)` — non-null, future, before grading deadline (D11).
  - `getConflictingAssignments(date)` — global scan within ±2 h.
  - `findConflictsForCommittee(...)` — committee-scoped variant for re-scheduling.

### DTOs
- `dto/request/GroupAssignmentRequest` (groupId required, examDate optional).
- `dto/request/AssignmentStatusUpdateRequest` (status required, examDate optional).
- `dto/response/GroupAssignmentResponse` and `GroupAssignmentListResponse`.
- Re-uses existing `dto/response/DeleteResponse`.

### Service
- `service/GroupCommitteeAssignmentService` + `impl/GroupCommitteeAssignmentServiceImpl`.
  - State machine — `ASSIGNED → SCHEDULED → COMPLETED|CANCELLED`, no backtracking.
    `COMPLETED` and `CANCELLED` are terminal.
  - SCHEDULED requires a valid, conflict-free `examDate`.
  - Conflict errors include the offending assignment ids/groups/dates.
  - Persists `AuditLog` rows directly (does not depend on AOP) for the three
    new `ActionType` values: `COMMITTEE_GROUP_ASSIGNED`,
    `COMMITTEE_GROUP_STATUS_UPDATED`, `COMMITTEE_GROUP_REMOVED`.
  - Sends a `NotificationType.COMMITTEE_ASSIGNED` notification to every group
    member on assign, status change, and delete.

### Controller
- `controller/GroupCommitteeAssignmentController.java` — `/api/v1/committees`
  - `POST   /{committeeId}/groups`                        → 201
  - `GET    /{committeeId}/groups`                        → 200
  - `GET    /groups/{groupId}/committees`                 → 200
  - `PATCH  /{committeeId}/groups/{assignmentId}/status`  → 200
  - `DELETE /{committeeId}/groups/{assignmentId}`         → 200

### Enums
- `ActionType` — added `COMMITTEE_GROUP_ASSIGNED`, `COMMITTEE_GROUP_STATUS_UPDATED`, `COMMITTEE_GROUP_REMOVED`.
- `NotificationType` — added `COMMITTEE_ASSIGNED`.

## HTTP code mapping

| Code | When |
|------|------|
| 200  | List, status update, delete |
| 201  | Successful POST assignment |
| 400  | Invalid status transition, schedule conflict, bad exam date |
| 404  | Committee / group / assignment not found |
| 409  | Group already assigned to this committee (unique constraint) |

## State machine

```
ASSIGNED ──► SCHEDULED ──► COMPLETED
    │            │
    └────────────┴────► CANCELLED
```

Any other transition (including backwards moves) returns 400.

## Tests
- `service/ScheduleValidatorTest.java` — unit tests for the validator
  (no conflict, conflict within ±2 h, exam-date rules).

## Build
```
cd backend && mvn -DskipTests clean compile     # passes
cd backend && mvn test-compile                  # passes
```
