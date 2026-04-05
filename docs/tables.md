### Phase 2: Critical Business Processes

| PROCESS | DESCRIPTION | SYSTEM COMPONENTS INVOLVED |
| :--- | :--- | :--- |
| **User Registration** | Students register using valid IDs uploaded by the coordinator  and connect their GitHub accounts. Professors are manually registered by the admin. | Frontend, Backend, GitHub OAuth (NextAuth.js)  |
| **Group Management & Integrations** | Team leaders create groups , add members , request advisors , and bind both JIRA spaces  and GitHub Organization Personal Access Tokens. | Frontend, Backend, Notification System  |
| **Submissions & Reviews** | Groups submit Deliverables (Proposals and Statements of Work)  for assigned committees to review, leave comments on, and grade. | Frontend, Backend, Embedded Markdown Editor  |
| **Scrum & Progress Tracking** | The system fetches active stories daily by matching issue keys. It accesses GitHub to find branches starting with the issue key and obtains related Pull Requests to check if they have been merged. | Backend, JIRA API, GitHub API  |
| **AI Validation** | An AI reads Pull Request comments to verify the review process  and reads introduced file diffs to validate them against the issue description. | Backend, AI Integration Service  |
| **Grading & Evaluation** | Advisors grade team sprint performance (Point A)  and code review (Point B)  using Soft Grading. The system calculates individual allowances based on the ratio of completed story points. | Frontend, Backend, Database  |

---

### Phase 2: Granular Process-to-Component Mapping
*Decomposed Sub-Processes for Implementation*

| MAIN PROCESS & SUB-PROCESS STEPS | SYSTEM COMPONENT | DATA REQUIRED | API NEEDED |
| :--- | :--- | :--- | :--- |
| **1. Authentication & Registration** | | | |
| Validate student against uploaded IDs  | Backend + DB | Student ID | `GET /api/students/validate` |
| Execute GitHub OAuth login  | Frontend + NextAuth.js | User Credentials | **External: GitHub OAuth API** |
| Fetch and register GitHub username  | Backend + DB | Auth Token, GitHub Username | `POST /api/users/profile` |
| **2. Group Management & Integration Setup** | | | |
| Create group & auto-appoint leader  | Backend + DB | Student ID, Group Name | `POST /api/groups/create` |
| Bind JIRA space to team  | Backend + JIRA Integration | JIRA Space ID/URL, Team ID | `POST /api/integrations/jira` |
| Bind GitHub Organization using PAT  | Backend + GitHub Integration | GitHub PAT, Team ID | `POST /api/integrations/github` |
| **3. Daily Sprint Tracking** | | | |
| Trigger daily active story refresh  | Backend Cron Job | Timestamp, Team Integrations | *Internal Scheduler* |
| Fetch issue metrics (Key, Assignee, Desc., etc.)  | Backend + External Services | Issue Keys, JIRA/GitHub Tokens | **External: JIRA / GitHub API** |
| Verify end-of-sprint story point estimates  | Backend + GitHub API | Issue Keys, Estimates | **External: GitHub API** |
| **4. Pull Request & Branch Verification** | | | |
| Find branches starting with issue key  | Backend + GitHub API | Issue Key | **External: GitHub API** |
| Obtain related PRs for the branch  | Backend + GitHub API | Branch Name | **External: GitHub API** |
| Verify if the PR has been merged  | Backend | PR Merge Status | *Internal Logic / DB Update* |
| **5. AI-Assisted Validation** | | | |
| Read PR comments to verify review process  | Backend + AI Service | PR Comments | **External: AI Provider API** |
| Extract file diffs introduced in the PR  | Backend + GitHub API | PR ID, Commit Hashes | **External: GitHub API** |
| Validate diffs against issue description  | Backend + AI Service | File Diffs, Issue Description | **External: AI Provider API** |
| **6. Advisor Evaluation** | | | |
| Grade team sprint performance (Point A)  | Frontend + Backend | Soft Grade (0-100) | `POST /api/grades/point-a` |
| Grade work/code review (Point B)  | Frontend + Backend | Soft Grade (0-100) | `POST /api/grades/point-b` |
| **7. Final Grade Calculation** | | | |
| Calculate Point A & B average (team allowance)  | Backend | Point A, Point B | `PUT /api/grades/allowance` |
| Calculate individual completion ratio  | Backend | Completed Story Points, Target Points | `PUT /api/grades/ratio` |
| Apply deliverable scalars  | Backend | Team Allowance, Deliverable Grades | `PUT /api/grades/scalars` |
| Compute final individual student grade  | Backend + DB | Individual Ratio, Scaled Deliverable Grade | `PUT /api/grades/final` |