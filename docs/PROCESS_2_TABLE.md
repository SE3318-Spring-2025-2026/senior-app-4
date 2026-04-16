# 🚀 Process-2: API Delivery Summary
## Level-2 DFD — Group Management & Integrations

---

### 📐 DFD Component Mapping

| Component | Type | DFD ID |
| :--- | :--- | :--- |
| Student | External Entity (Human) | e_student |
| Professor | External Entity (Human) | e_professor |
| Coordinator | External Entity (Human) | e_coordinator |
| GitHub | External Entity (System) | e_github |
| JIRA | External Entity (System) | e_jira |
| Users | Data Store | D1 |
| Groups | Data Store | D3 |
| Integrations | Data Store | D8 |
| System Logs | Data Store | D9 |
| Notifications | Data Store | D10 |
| Schedule | Data Store | D11 |

### 📦 Sub-Process Summary

| ID | Sub-Process | DFD Colour |
| :--- | :--- | :--- |
| 2.1 | Manage Project Groups | Blue (dae8fc) |
| 2.2 | Manage Group Members | Blue (dae8fc) |
| 2.3 | Manage GitHub Integration | Blue (dae8fc) |
| 2.4 | View Project Groups | Blue (dae8fc) |
| 2.5 | Notification Service (auto-reject on approval) | Green (d5e8d4) |
| 2.6 | Manage JIRA Integration | Blue (dae8fc) |
| 2.7 | Schedule Validator | Blue (dae8fc) |
| 2.8 | Disband Unadvised Groups | Orange (ffe6cc) |
| 2.9 | Logging & Audit Trail (immutable write) | Red (f8cecc) |

---

## 📊 Complete Implementation Table

| # | Method | Path | Auth | Sub-process | DFD Flows | Issue ID | SP | Difficulty |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **— 2.1 Manage Project Groups —** | | | | | | | | |
| 1 | POST | `/groups` | Student (Leader) | 2.1 | f1, f2, f2b, f3, f4 | P2-API-01 | 3 | Medium |
| 2 | PUT | `/groups/{groupId}` | Student (Leader) | 2.1 | f1, f3 | P2-API-02 | 2 | Easy |
| 3 | POST | `/groups/{groupId}/advisor/transfer` | Staff (Coordinator) | 2.1 | coord_f2 | P2-API-03 | 3 | Medium |
| 4 | GET | `/coordinator/reports/group-formation` | Staff (Coordinator) | 2.1 | coord_f4 | P2-API-04 | 3 | Medium |
| **— 2.2 Manage Group Members —** | | | | | | | | |
| 5 | POST | `/groups/{groupId}/members` | Student (Leader) / Coordinator | 2.2 | f5, f7, ns_f1 | P2-API-05 | 3 | Medium |
| 6 | GET | `/groups/{groupId}/members` | Auth User | 2.2 | f7b, f17 | P2-API-06 | 1 | Easy |
| 7 | DELETE | `/groups/{groupId}/members/{studentId}` | Student (Leader) / Coordinator | 2.2 | f5, f7, log_f1 | P2-API-07 | 2 | Easy |
| 8 | POST | `/groups/{groupId}/leave` | Student (Member) | 2.2 | ns_f3, ns_f4 | P2-API-08 | 2 | Easy |
| **— 2.3 Manage GitHub Integration —** | | | | | | | | |
| 9 | POST | `/groups/{groupId}/integrations/github` | Student (Leader) | 2.3 | f9, f10, f10b, f11, f12, f13 | P2-API-09 | 3 | Medium |
| 10 | GET | `/groups/{groupId}/integrations/github` | Student (Leader) | 2.3 | f9, f13 | P2-API-10 | 1 | Easy |
| 11 | DELETE | `/groups/{groupId}/integrations/github` | Student (Leader) | 2.3 | f13, f14 | P2-API-11 | 2 | Easy |
| **— 2.4 View Project Groups —** | | | | | | | | |
| 12 | GET | `/groups` | Auth User | 2.4 | f15, f16, f17, f18 | P2-API-12 | 2 | Easy |
| 13 | GET | `/groups/{groupId}` | Auth User | 2.4 | f15, f6, f17 | P2-API-13 | 1 | Easy |
| **— 2.5 Notification Service —** | | | | | | | | |
| 14 | POST | `/groups/{groupId}/advisor-request` | Student (Leader) | 2.5 | ns_f5 | P2-API-14 | 3 | Medium |
| 15 | GET | `/groups/{groupId}/advisor-request` | Student (Leader) | 2.5 | ns_f9 | P2-API-15 | 1 | Easy |
| 16 | DELETE | `/groups/{groupId}/advisor-request` | Student (Leader) | 2.5 | log_f2 | P2-API-16 | 2 | Easy |
| 17 | GET | `/notifications` | Auth User | 2.5 | ns_f2, ns_f8 | P2-API-17 | 2 | Easy |
| 18 | DELETE | `/notifications` | Auth User | 2.5 | ns_f8 | P2-API-18 | 1 | Easy |
| 19 | POST | `/notifications/{notificationId}/respond` | Student | 2.5 | ns_f3 | P2-API-19 | 3 | Medium |
| 20 | GET | `/professors/advisor-requests` | Staff (Professor) | 2.5 | ns_f6 | P2-API-20 | 2 | Easy |
| 21 | PATCH | `/professors/advisor-requests` | Staff (Professor) | 2.5 | ns_f7, ns_f8, ns_f10, autoReject | P2-API-21 | 5 | Hard |
| 22 | GET | `/coordinator/system-alerts` | Staff (Coordinator) | 2.5 | coord_f5 | P2-API-22 | 2 | Easy |
| **— 2.6 Manage JIRA Integration —** | | | | | | | | |
| 23 | POST | `/groups/{groupId}/integrations/jira` | Student (Leader) | 2.6 | jira_f1, jira_f2, jira_f3, jira_f4 | P2-API-23 | 3 | Medium |
| 24 | GET | `/groups/{groupId}/integrations/jira` | Student (Leader) | 2.6 | jira_f1 | P2-API-24 | 1 | Easy |
| 25 | DELETE | `/groups/{groupId}/integrations/jira` | Student (Leader) | 2.6 | jira_f4 | P2-API-25 | 2 | Easy |
| **— 2.3 / 2.6 Integration Test —** | | | | | | | | |
| 26 | POST | `/groups/{groupId}/integrations/test` | Student (Leader) | 2.3 / 2.6 | f11, f12, jira_f2, jira_f3 | P2-API-26 | 5 | Hard |
| **— 2.7 Schedule Validator —** | | | | | | | | |
| 27 | GET | `/coordinator/schedule` | Staff (Coordinator) | 2.7 | coord_f3, D11→p2_7 | P2-API-27 | 1 | Easy |
| 28 | PUT | `/coordinator/schedule` | Staff (Coordinator) | 2.7 | coord_f3 | P2-API-28 | 2 | Easy |
| **— 2.8 Disband Unadvised Groups —** | | | | | | | | |
| 29 | DELETE | `/groups/{groupId}` | Student (Leader) / Coordinator | 2.8 | disb_f1, disb_f2, disb_f3 | P2-API-29 | 5 | Hard |
| **— 2.9 Logging & Audit Trail —** | | | | | | | | |
| 30 | GET | `/admin/logs` | Staff (Coordinator) | 2.9 | log_f4 | P2-API-30 | 2 | Easy |
| 31 | GET | `/groups/{groupId}/logs` | Auth User | 2.9 | log_f1, log_f3 | P2-API-31 | 2 | Easy |

**Total Cumulative Complexity: 72 SP**

---

## 🔄 DFD Data Flow Traceability Matrix

> Her endpoint'in hangi DFD akışına (flow) karşılık geldiğini gösterir.

### 2.1 — Manage Project Groups
| Flow ID | Label | From → To | Endpoint |
| :--- | :--- | :--- | :--- |
| f1 | groupData (create/update/delete) | Student → P2.1 | POST/PUT `/groups` |
| f2 | validateCreator (studentId) | P2.1 → D1 | POST `/groups` (internal) |
| f2b | validationStatus | D1 → P2.1 | POST `/groups` (internal) |
| f3 | groupRecord (write/update/delete) | P2.1 → D3 | POST/PUT `/groups` |
| f4 | groupResponse / deleteResponse | P2.1 → Student | POST/PUT `/groups` (response) |
| coord_f2 | advisorTransferCommand | Coordinator → P2.1 | POST `/groups/{groupId}/advisor/transfer` |
| coord_f4 | groupStatusReport (formed/unadvised) | P2.1 → Coordinator | GET `/coordinator/reports/group-formation` |

### 2.2 — Manage Group Members
| Flow ID | Label | From → To | Endpoint |
| :--- | :--- | :--- | :--- |
| f5 | memberData (add/remove) | Student → P2.2 | POST/DELETE `/groups/{groupId}/members` |
| f7 | memberRecord (write/delete) | P2.2 → D3 | POST/DELETE (internal write) |
| f7b | groupStatus | D3 → P2.2 | GET `/groups/{groupId}/members` |
| f8 | memberResponse / deleteResponse | P2.2 → Student | Response payloads |
| coord_f1 | manualMemberAdjustment | Coordinator → P2.2 | POST/DELETE `/groups/{groupId}/members` |
| ns_f1 | membershipRequest | P2.2 → P2.5 | POST `/groups/{groupId}/members` (trigger) |
| ns_f4 | finalizedMembershipData | P2.5 → P2.2 | POST `/groups/{groupId}/leave` |
| autoReject | autoRejectCommand | P2.5 → P2.2 | Internal (on advisor approval) |

### 2.3 — Manage GitHub Integration
| Flow ID | Label | From → To | Endpoint |
| :--- | :--- | :--- | :--- |
| f9 | integrationData (bind/get/unbind) | Student → P2.3 | POST/GET/DELETE `.../github` |
| f10 | verifyGroup (groupId) | P2.3 → D3 | Internal validation |
| f10b | groupValidation | D3 → P2.3 | Internal response |
| f11 | validateRepo (repositoryUrl) | P2.3 → GitHub | POST `.../integrations/test` |
| f12 | repoValidationStatus | GitHub → P2.3 | External callback |
| f13 | integrationRecord (write/read/delete) | P2.3 → D8 | POST/GET/DELETE `.../github` |
| f14 | integrationResponse / deleteResponse | P2.3 → Student | Response payloads |

### 2.4 — View Project Groups
| Flow ID | Label | From → To | Endpoint |
| :--- | :--- | :--- | :--- |
| f15 | listRequest / detailRequest(groupId) | Student → P2.4 | GET `/groups`, GET `/groups/{groupId}` |
| f6 | verifyStudent (studentId) | P2.4 → D1 | Internal auth check |
| f16 | queryGroups | P2.4 → D3 | Internal query |
| f17 | groupRecords | D3 → P2.4 | Internal response |
| f18 | groupListResponse (message, count, data) | P2.4 → Student | GET `/groups` (response) |

### 2.5 — Notification Service
| Flow ID | Label | From → To | Endpoint |
| :--- | :--- | :--- | :--- |
| ns_f2 | pendingInviteNotification | P2.5 → Student | GET `/notifications` |
| ns_f3 | membershipResponse (Approve/Reject) | Student → P2.5 | POST `.../respond` |
| ns_f5 | adviseeRequest | Student → P2.5 | POST `.../advisor-request` |
| ns_f6 | advisorReviewAlert | P2.5 → Professor | GET `/professors/advisor-requests` |
| ns_f7 | requestDecision (Approve/Reject) | Professor → P2.5 | PATCH `/professors/advisor-requests` |
| ns_f8 | saveNotificationStatus | P2.5 → D10 | Internal write |
| ns_f9 | queryPendingRecords | P2.5 → D3 | GET `.../advisor-request` |
| ns_f10 | markRejected (other pending) | D3 → P2.5 | PATCH (auto-reject side effect) |
| coord_f5 | systemAlerts (critical/deadline exceeded) | P2.5 → Coordinator | GET `/coordinator/system-alerts` |

### 2.6 — Manage JIRA Integration
| Flow ID | Label | From → To | Endpoint |
| :--- | :--- | :--- | :--- |
| jira_f1 | JiraSpaceURL / APIKey / ProjectKey | Student → P2.6 | POST/GET `.../jira` |
| jira_f2 | ConnectionValidationRequest | P2.6 → JIRA | POST `.../integrations/test` |
| jira_f3 | SpaceValidationSuccess / Error | JIRA → P2.6 | External callback |
| jira_f4 | JiraIntegrationDetails | P2.6 → D3 | POST/DELETE `.../jira` |
| jira_f5 | IntegrationStatusAlert | P2.6 → P2.5 | Internal notification trigger |

### 2.7 — Schedule Validator
| Flow ID | Label | From → To | Endpoint |
| :--- | :--- | :--- | :--- |
| coord_f3 | scheduleConfig (group/advisor deadlines) | Coordinator → P2.7 | GET/PUT `/coordinator/schedule` |
| D11→P2.7 | scheduleData | D11 → P2.7 | Internal read |

### 2.8 — Disband Unadvised Groups
| Flow ID | Label | From → To | Endpoint |
| :--- | :--- | :--- | :--- |
| disb_f1 | disbandCommand (delete/setDisbanded) | P2.8 → D3 | DELETE `/groups/{groupId}` |
| disb_f2 | disbandNotification | P2.8 → P2.5 | Internal notification trigger |
| disb_f3 | resetStudentRoles (remove Leader/Member) | P2.8 → D1 | Internal user role reset |

### 2.9 — Logging & Audit Trail
| Flow ID | Label | From → To | Endpoint |
| :--- | :--- | :--- | :--- |
| log_f1 | groupCreated / memberAdded / advisorAssigned | P2.1 → P2.9 | Internal event |
| log_f2 | notificationSent / approvalReceived | P2.5 → P2.9 | Internal event |
| log_f3 | groupDisbandedEvent / rolesResetEvent | P2.8 → P2.9 | Internal event |
| log_f4 | auditEntry (userId, actionType, eventDetails, ip, ts) | P2.9 → D9 | GET `/admin/logs`, GET `/groups/{groupId}/logs` |

---

## 📌 Data Store Write/Read Summary

| Data Store | Read By | Written By |
| :--- | :--- | :--- |
| D1 \| Users | 2.1 (f2), 2.4 (f6) | 2.8 (disb_f3) |
| D3 \| Groups | 2.2 (f7b), 2.3 (f10), 2.4 (f16/f17), 2.5 (ns_f9/ns_f10) | 2.1 (f3), 2.2 (f7), 2.6 (jira_f4), 2.8 (disb_f1) |
| D8 \| Integrations | 2.3 (f13 read) | 2.3 (f13 write) |
| D9 \| System Logs | 2.9 (log_f4 read) | 2.9 (log_f4 write) |
| D10 \| Notifications | 2.5 (ns_f8 read) | 2.5 (ns_f8 write) |
| D11 \| Schedule | 2.7 (read) | 2.7 (coord_f3 write) |
