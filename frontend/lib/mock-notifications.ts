import { Notification } from "./notification-types";

export const mockNotifications: Notification[] = [
    {
        id: 1,
        type: "membership_invite",
        message: "You have been invited to join Alpha Team.",
        status: "pending",
        readStatus: false,
        fromUserId: 101,
        fromUserName: "Alice Leader",
        groupId: 101,
        createdAt: "2026-04-10T12:00:00Z",
    },

    {
        id: 3,
        type: "system_alert",
        message: "Sprint deadline is approaching!",
        status: "cleared",
        readStatus: false,
        fromUserId: null,
        fromUserName: null,
        groupId: null,
        createdAt: "2026-04-08T10:00:00Z",
    },
    {
        id: 4,
        type: "group_disbanded",
        message: "Your group Delta Vision has been disbanded.",
        status: "cleared",
        readStatus: false,
        fromUserId: null,
        fromUserName: null,
        groupId: 104,
        createdAt: "2026-04-07T09:00:00Z",
    },
];