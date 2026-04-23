export type DeliverableType = "PROPOSAL" | "REVISED_PROPOSAL" | "STATEMENT_OF_WORK";

export type SubmissionStatus =
  | "PENDING_REVIEW"
  | "UNDER_REVIEW"
  | "REVISION_REQUESTED"
  | "APPROVED"
  | "GRADED";

export type ReviewDecision = "APPROVED" | "REVISION_REQUESTED";

export type CommitteeSubmissionPreview = {
  submissionId: number;
  groupId: number;
  groupName: string;
  deliverableType: DeliverableType;
  status: SubmissionStatus;
  submittedAt: string;
  deadline: string;
  submittedBy: string;
  assignedCommitteeName: string;
  fileName: string;
  fileUrl: string;
  commentsCount: number;
  latestReviewDecision?: ReviewDecision;
};
