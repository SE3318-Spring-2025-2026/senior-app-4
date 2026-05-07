import { getToken } from "@/lib/auth";
import { API_BASE, parseError } from "@/lib/api-utils";

export type GithubIntegrationApiResponse = {
    success: boolean;
    data: {
        status: string;
        organizationName: string | null;
        connectedAt: string | null;
        message: string | null;
    };
};

export type JiraIntegrationApiResponse = {
    success: boolean;
    data: {
        status: string;
        jiraSpaceUrl: string | null;
        projectKey: string | null;
        connectedAt: string | null;
        message: string | null;
    };
};


export async function fetchGithubIntegration(
    groupId: number
): Promise<GithubIntegrationApiResponse> {
    const token = getToken();

    const res = await fetch(
        `${API_BASE}/groups/${groupId}/integrations/github`,
        {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token ?? ""}`,
            },
            cache: "no-store",
        }
    );

    if (!res.ok) {
        const errorMessage = await parseError(res);

        return {
            success: false,
            data: {
                status: "inactive",
                organizationName: null,
                connectedAt: null,
                message: errorMessage || "Not connected",
            },
        };
    }

    return res.json();
}

export async function fetchJiraIntegration(
    groupId: number
): Promise<JiraIntegrationApiResponse> {
    const token = getToken();

    const res = await fetch(
        `${API_BASE}/groups/${groupId}/integrations/jira`,
        {
            method: "GET",
            headers: {
                Authorization: `Bearer ${token ?? ""}`,
            },
            cache: "no-store",
        }
    );

    if (!res.ok) {
        const errorMessage = await parseError(res);

        return {
            success: false,
            data: {
                status: "inactive",
                jiraSpaceUrl: null,
                projectKey: null,
                connectedAt: null,
                message: errorMessage || "Not connected",
            },
        };
    }

    return res.json();
}

export type IntegrationsTestResponse = {
    github: {
        connected: boolean;
        message: string;
    };
    jira: {
        connected: boolean;
        message: string;
    };
};

export async function bindGithubIntegration(
    groupId: number,
    githubPat: string,
    organizationName: string,
    repositoryName: string
): Promise<void> {
    const token = getToken();

    const res = await fetch(`${API_BASE}/groups/${groupId}/integrations/github`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token ?? ""}`,
        },
        body: JSON.stringify({
            githubPat,
            organizationName,
            repositoryName,
        }),
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }
}

export async function unbindGithubIntegration(groupId: number): Promise<void> {
    const token = getToken();

    const res = await fetch(`${API_BASE}/groups/${groupId}/integrations/github`, {
        method: "DELETE",
        headers: {
            Authorization: `Bearer ${token ?? ""}`,
        },
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }
}

export async function testIntegrations(
    groupId: number
): Promise<IntegrationsTestResponse> {
    const token = getToken();

    const res = await fetch(`${API_BASE}/groups/${groupId}/integrations/test`, {
        method: "POST",
        headers: {
            Authorization: `Bearer ${token ?? ""}`,
        },
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }

    return res.json();
}

export type IntegrationStatusResponse = {
    github: "CONNECTED" | "DISCONNECTED";
    jira: "CONNECTED" | "DISCONNECTED";
};

export type IntegrationSettingsPayload = {
    githubPat: string;
    jiraSpaceUrl: string;
};

export async function getIntegrationStatus(): Promise<IntegrationStatusResponse> {
    const token = getToken();

    const res = await fetch(`${API_BASE}/integrations/status`, {
        method: "GET",
        headers: {
            Authorization: `Bearer ${token ?? ""}`,
        },
        cache: "no-store",
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }

    return res.json();
}

export async function saveIntegrationSettings(
    payload: IntegrationSettingsPayload
): Promise<{ message: string }> {
    const token = getToken();

    const res = await fetch(`${API_BASE}/integrations/settings`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token ?? ""}`,
        },
        body: JSON.stringify(payload),
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }

    return res.json();
}

export async function bindJiraIntegration(
    groupId: number,
    jiraSpaceUrl: string,
    email: string,
    apiKey: string,
    projectKey: string
): Promise<void> {
    const token = getToken();

    const res = await fetch(`${API_BASE}/groups/${groupId}/integrations/jira`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token ?? ""}`,
        },
        body: JSON.stringify({ jiraSpaceUrl, email, apiKey, projectKey }),
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }
}

export async function triggerScrumSync(): Promise<{ status: string }> {
    const token = getToken();

    const res = await fetch(`${API_BASE}/scrum-sync/trigger`, {
        method: "POST",
        headers: {
            Authorization: `Bearer ${token ?? ""}`,
        },
    });

    if (!res.ok) {
        throw new Error(await parseError(res));
    }

    return res.json();
}