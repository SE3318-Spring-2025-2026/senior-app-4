export type NotificationType =
    | "membership_invite"
    | "advisor_request"
    | "advisor_decision"
    | "system_alert"
    | "group_disbanded";

export type NotificationStatus =
    | "pending"
    | "accepted"
    | "rejected"
    | "cleared";

export type Notification = {
    id: number;
    type: NotificationType;
    message: string;
    status: NotificationStatus;
    fromUserId: number | null;
    groupId: number | null;
    createdAt: string;
};

export type NotificationDecision = "accept" | "reject";