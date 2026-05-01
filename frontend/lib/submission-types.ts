export type { DeliverableType, SubmissionStatus, ReviewDecision } from "@/lib/submissions-api";

import type { DeliverableType, SubmissionStatus, ReviewDecision } from "@/lib/submissions-api";

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
