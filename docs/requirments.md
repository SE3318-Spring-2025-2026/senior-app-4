### Phase 2: Critical Business Processes

| PROCESS | DESCRIPTION | SYSTEM COMPONENTS INVOLVED |
| :--- | :--- | :--- |
| **1. User Registration** | Students register using valid IDs uploaded by the coordinator and connect their GitHub accounts. Professors are manually registered by the admin. | Frontend, Backend, GitHub OAuth (NextAuth.js) |
| **2. Group Management & Integrations** | Team leaders create groups, add members, and bind both JIRA spaces and GitHub Organization Personal Access Tokens. | Frontend, Backend, Notification System |
| **3. Submissions & Reviews** | Groups submit Deliverables (Proposals and Statements of Work) for assigned committees to review, leave comments on, and grade. | Frontend, Backend, Embedded Markdown Editor |
| **4. Advisor Assignment** | Team leaders make an advisee request to a professor, who is notified and can then approve the association. | Frontend, Backend, Notification Sevices |
| **5. Committee Assignment** | Coordinators create committees and assign advisors to them, who will then be responsible for grading multiple groups. | Frontend, Backend, Database |
| **6. Scrum & Progress Tracking** | The system fetches active stories daily by matching issue keys. It accesses GitHub to find branches starting with the issue key and obtains related Pull Requests to check if they have been merged. | Backend, JIRA API, GitHub API |
| **7. AI Validation** | An AI reads Pull Request comments to verify the review process and reads introduced file diffs to validate them against the issue description. | Backend, AI Integration Service |
| **8. Grading & Evaluation** | Advisors grade team sprint performance (Point A) and code review (Point B) using Soft Grading. The system calculates individual allowances based on the ratio of completed story points. | Frontend, Backend, Database |

---

### Phase 2: Granular Process-to-Component Mapping
*Decomposed Sub-Processes for Implementation*

| MAIN PROCESS & SUB-PROCESS STEPS | SYSTEM COMPONENT | DATA REQUIRED |
| :--- | :--- | :--- |
| **1. User Registration** | | |
| Validate student against uploaded IDs | Backend + DB | `studentId` |
| Execute GitHub OAuth login | Frontend + NextAuth.js | `userCredentials` |
| Fetch and register GitHub username | Backend + DB | `authToken`, `githubUsername` |
| **2. Group Management & Integrations** | | |
| Create group & auto-appoint leader | Backend + DB | `studentId`, `groupName` |
| Bind JIRA space to team | Backend + JIRA Integration | `jiraSpaceUrl`, `teamId` |
| Bind GitHub Organization using PAT | Backend + GitHub Integration | `githubPat`, `teamId` |
| **3. Submissions & Reviews** | | |
| Submit deliverable document | Frontend + Backend | `teamId`, `documentContent` |
| Committee reviews and comments | Frontend + Backend | `committeeId`, `comments` |
| **4. Advisor Assignment** | | |
| Make advisee request to professor | Frontend + Backend | `teamId`, `professorId` |
| Approve advisee request | Frontend + Backend | `requestId` |
| **5. Committee Assignment** | | |
| Create committee | Backend + DB | `committeeName` |
| Assign advisors | Backend + DB | `committeeId`, `advisorIds` |
| **6. Scrum & Progress Tracking** | | |
| Trigger daily active story refresh | Backend Cron Job | `timestamp`, `teamIntegrations` |
| Fetch issue metrics (Key, Assignee, Desc., etc.) | Backend + External Services | `issueKeys`, `jiraToken`, `githubToken` |
| Verify end-of-sprint story point estimates | Backend + GitHub API | `issueKeys`, `estimates` |
| Find branches starting with issue key | Backend + GitHub API | `issueKey` |
| Obtain related PRs for the branch | Backend + GitHub API | `branchName` |
| Verify if the PR has been merged | Backend | `prMergeStatus` |
| **7. AI Validation** | | |
| Read PR comments to verify review process | Backend + AI Service | `prComments` |
| Extract file diffs introduced in the PR | Backend + GitHub API | `prId`, `commitHashes` |
| Validate diffs against issue description | Backend + AI Service | `fileDiffs`, `issueDescription` |
| **8. Grading & Evaluation** | | |
| Grade team sprint performance (Point A) | Frontend + Backend | `softGrade` |
| Grade work/code review (Point B) | Frontend + Backend | `softGrade` |
| Calculate Point A & B average (team allowance) | Backend | `pointA`, `pointB` |
| Calculate individual completion ratio | Backend | `completedStoryPoints`, `targetPoints` |
| Apply deliverable scalars | Backend | `teamAllowance`, `deliverableGrades` |
| Compute final individual student grade | Backend + DB | `individualRatio`, `scaledDeliverableGrade` |