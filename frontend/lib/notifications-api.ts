import { API_BASE, buildHeaders as authHeaders, parseError } from "@/lib/api-utils";

// ------------------------------------------------------------------ Types

export type ApiNotification = {
    id: number;
    type: string;
    message: string;
    status: string;
    fromUserId: number | null;
    fromUserName: string | null;
    toUserId: number | null;
    groupId: number | null;
    createdAt: string;
};

export type ApiPage<T> = {
    content: T[];
    totalPages: number;
    totalElements: number;
    number: number;
    size: number;
};

// ------------------------------------------------------------------ API calls

/**
 * ns_f2: Fetch notifications for the current user.
 */
export async function fetchNotifications(
    page = 0,
    size = 20
): Promise<ApiPage<ApiNotification>> {
    const res = await fetch(
        `${API_BASE}/notifications?page=${page}&size=${size}`,
        {
            method: "GET",
            headers: authHeaders(),
            cache: "no-store",
        }
    );

    if (!res.ok) {
        throw new Error(await parseError(res));
    }

    return res.json();
}

/**
 * P5.11 — Fetch the current user's committee notifications.
 */
export async function fetchMyCommitteeNotifications(): Promise<ApiNotification[]> {
    const res = await fetch(`${API_BASE}/committees/notifications`, {
        method: "GET",
        headers: authHeaders(),
        cache: "no-store",
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }

    return res.json();
}

/**
 * P2-API-19: Student responds to a notification (accept or reject).
 */
export async function respondNotification(
    notificationId: number,
    decision: "accept" | "reject"
): Promise<void> {
    const res = await fetch(
        `${API_BASE}/notifications/${notificationId}/respond`,
        {
            method: "POST",
            headers: authHeaders(),
            body: JSON.stringify({ decision }),
        }
    );

    if (!res.ok) {
        throw new Error(await parseError(res));
    }
}

/**
 * Clear a single notification.
 */
export async function clearNotificationApi(notificationId: number): Promise<void> {
    const res = await fetch(
        `${API_BASE}/notifications?id=${notificationId}`,
        {
            method: "DELETE",
            headers: authHeaders(),
        }
    );

    if (!res.ok && res.status !== 204) {
        throw new Error(await parseError(res));
    }
}

/**
 * Mark a single notification as read (issue #92).
 */
export async function markNotificationRead(notificationId: number): Promise<void> {
    const res = await fetch(
        `${API_BASE}/notifications/${notificationId}/read`,
        {
            method: "PATCH",
            headers: authHeaders(),
        }
    );

    if (!res.ok && res.status !== 204) {
        throw new Error(await parseError(res));
    }
}

/**
 * P2-API-05: Leader invites a student to the group (ns_f1).
 */
export async function inviteMemberApi(
    groupId: number,
    studentId: number
): Promise<void> {
    const res = await fetch(`${API_BASE}/groups/${groupId}/members`, {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify({ studentId }),
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }
}
