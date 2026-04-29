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
