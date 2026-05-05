export type NotificationType =
    | "membership_invite"
    | "advisor_request"
    | "advisor_decision"
    | "system_alert"
    | "group_disbanded"
    | "committee_assigned"
    | "advisor_assignment"
    | "jury_assignment"
    | "group_assignment"
    | "schedule_change"
    | "meeting_reminder"
    | "general";

export type NotificationStatus =
    | "pending"
    | "accepted"
    | "rejected"
    | "cleared"
    | "revoked";

export type Notification = {
    id: number;
    type: NotificationType;
    message: string;
    status: NotificationStatus;
    readStatus: boolean;
    fromUserId: number | null;
    fromUserName: string | null;
    groupId: number | null;
    createdAt: string;
};

export type NotificationDecision = "accept" | "reject";
