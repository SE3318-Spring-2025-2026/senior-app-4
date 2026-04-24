import { CommitteeSubmissionPreview } from "@/lib/submission-types";

// TODO(#74): replace this mock grading preview data with professor-facing
// submission list/detail API responses once Process 3 backend endpoints land.
export const mockCommitteeSubmissions: CommitteeSubmissionPreview[] = [
  {
    submissionId: 1001,
    groupId: 101,
    groupName: "Alpha Team",
    deliverableType: "PROPOSAL",
    status: "APPROVED",
    submittedAt: "2026-04-14T09:30:00Z",
    deadline: "2026-04-20T23:59:00Z",
    submittedBy: "Miray Yildirim",
    assignedCommitteeName: "Committee A",
    fileName: "alpha-team-proposal-v1.pdf",
    fileUrl: "https://example.com/files/alpha-team-proposal-v1.pdf",
    commentsCount: 3,
    latestReviewDecision: "APPROVED",
  },
  {
    submissionId: 1002,
    groupId: 103,
    groupName: "Code Crafters",
    deliverableType: "PROPOSAL",
    status: "UNDER_REVIEW",
    submittedAt: "2026-04-15T11:15:00Z",
    deadline: "2026-04-22T23:59:00Z",
    submittedBy: "Selin Aras",
    assignedCommitteeName: "Committee B",
    fileName: "code-crafters-proposal.pdf",
    fileUrl: "https://example.com/files/code-crafters-proposal.pdf",
    commentsCount: 1,
  },
];
