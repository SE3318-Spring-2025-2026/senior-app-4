### Phase 2: Critical Business Processes

| PROCESS | DESCRIPTION | SYSTEM COMPONENTS INVOLVED |
| :--- | :--- | :--- |
| **User Registration** | [cite_start]Students register using valid IDs uploaded by the coordinator [cite: 74] [cite_start]and connect their GitHub accounts[cite: 75]. [cite_start]Professors are manually registered by the admin[cite: 77]. | [cite_start]Frontend, Backend, GitHub OAuth (NextAuth.js) [cite: 124] |
| **Group Management & Integrations** | [cite_start]Team leaders create groups [cite: 81][cite_start], add members [cite: 82][cite_start], request advisors [cite: 87][cite_start], and bind both JIRA spaces [cite: 127] [cite_start]and GitHub Organization Personal Access Tokens[cite: 128]. | [cite_start]Frontend, Backend, Notification System [cite: 82, 88] |
| **Submissions & Reviews** | [cite_start]Groups submit Deliverables (Proposals and Statements of Work) [cite: 27, 28, 100, 112] [cite_start]for assigned committees to review, leave comments on, and grade[cite: 107, 111, 115]. | [cite_start]Frontend, Backend, Embedded Markdown Editor [cite: 136, 137] |
| **Scrum & Progress Tracking** | [cite_start]The system fetches active stories daily by matching issue keys[cite: 129]. [cite_start]It accesses GitHub to find branches starting with the issue key and obtains related Pull Requests to check if they have been merged[cite: 130, 131]. | [cite_start]Backend, JIRA API, GitHub API [cite: 129, 130] |
| **AI Validation** | [cite_start]An AI reads Pull Request comments to verify the review process [cite: 140, 141] [cite_start]and reads introduced file diffs to validate them against the issue description[cite: 143, 144]. | [cite_start]Backend, AI Integration Service [cite: 141, 144] |
| **Grading & Evaluation** | [cite_start]Advisors grade team sprint performance (Point A) [cite: 35] [cite_start]and code review (Point B) [cite: 36] using Soft Grading. [cite_start]The system calculates individual allowances based on the ratio of completed story points[cite: 62]. | [cite_start]Frontend, Backend, Database [cite: 35, 36, 62] |

---

### Phase 2: Granular Process-to-Component Mapping
*Decomposed Sub-Processes for Implementation*

| MAIN PROCESS & SUB-PROCESS STEPS | SYSTEM COMPONENT | DATA REQUIRED | API NEEDED |
| :--- | :--- | :--- | :--- |
| **1. Authentication & Registration** | | | |
| [cite_start]Validate student against uploaded IDs [cite: 74] | Backend + DB | Student ID | `GET /api/students/validate` |
| [cite_start]Execute GitHub OAuth login [cite: 123, 124] | Frontend + NextAuth.js | User Credentials | **External: GitHub OAuth API** |
| [cite_start]Fetch and register GitHub username [cite: 75, 125] | Backend + DB | Auth Token, GitHub Username | `POST /api/users/profile` |
| **2. Group Management & Integration Setup** | | | |
| [cite_start]Create group & auto-appoint leader [cite: 81] | Backend + DB | Student ID, Group Name | `POST /api/groups/create` |
| [cite_start]Bind JIRA space to team [cite: 127] | Backend + JIRA Integration | JIRA Space ID/URL, Team ID | `POST /api/integrations/jira` |
| [cite_start]Bind GitHub Organization using PAT [cite: 128] | Backend + GitHub Integration | GitHub PAT, Team ID | `POST /api/integrations/github` |
| **3. Daily Sprint Tracking** | | | |
| [cite_start]Trigger daily active story refresh [cite: 129] | Backend Cron Job | Timestamp, Team Integrations | *Internal Scheduler* |
| [cite_start]Fetch issue metrics (Key, Assignee, Desc., etc.) [cite: 129] | Backend + External Services | Issue Keys, JIRA/GitHub Tokens | **External: JIRA / GitHub API** |
| [cite_start]Verify end-of-sprint story point estimates [cite: 119] | Backend + GitHub API | Issue Keys, Estimates | **External: GitHub API** |
| **4. Pull Request & Branch Verification** | | | |
| [cite_start]Find branches starting with issue key [cite: 130, 131] | Backend + GitHub API | Issue Key | **External: GitHub API** |
| [cite_start]Obtain related PRs for the branch [cite: 131] | Backend + GitHub API | Branch Name | **External: GitHub API** |
| [cite_start]Verify if the PR has been merged [cite: 131] | Backend | PR Merge Status | *Internal Logic / DB Update* |
| **5. AI-Assisted Validation** | | | |
| [cite_start]Read PR comments to verify review process [cite: 140, 141] | Backend + AI Service | PR Comments | **External: AI Provider API** |
| [cite_start]Extract file diffs introduced in the PR [cite: 144] | Backend + GitHub API | PR ID, Commit Hashes | **External: GitHub API** |
| [cite_start]Validate diffs against issue description [cite: 143, 144] | Backend + AI Service | File Diffs, Issue Description | **External: AI Provider API** |
| **6. Advisor Evaluation** | | | |
| [cite_start]Grade team sprint performance (Point A) [cite: 35] | Frontend + Backend | Soft Grade (0-100) | `POST /api/grades/point-a` |
| [cite_start]Grade work/code review (Point B) [cite: 36] | Frontend + Backend | Soft Grade (0-100) | `POST /api/grades/point-b` |
| **7. Final Grade Calculation** | | | |
| [cite_start]Calculate Point A & B average (team allowance) [cite: 37] | Backend | Point A, Point B | `PUT /api/grades/allowance` |
| [cite_start]Calculate individual completion ratio [cite: 62] | Backend | Completed Story Points, Target Points | `PUT /api/grades/ratio` |
| [cite_start]Apply deliverable scalars [cite: 52] | Backend | Team Allowance, Deliverable Grades | `PUT /api/grades/scalars` |
| [cite_start]Compute final individual student grade [cite: 64] | Backend + DB | Individual Ratio, Scaled Deliverable Grade | `PUT /api/grades/final` |