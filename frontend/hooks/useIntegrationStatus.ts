"use client";

import { useState, useEffect, useCallback } from "react";
import { fetchGithubIntegration, fetchJiraIntegration } from "@/lib/integrations-api";

export type IntegrationEntry = {
    connected: boolean;
    connectedAt: string | null;
};

export type IntegrationStatusState = {
    github: IntegrationEntry;
    jira: IntegrationEntry;
};

type HookReturn = {
    status: IntegrationStatusState | null;
    loading: boolean;
    error: string | null;
    refetch: () => void;
};

export function useIntegrationStatus(groupId: number): HookReturn {
    const [status, setStatus] = useState<IntegrationStatusState | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const load = useCallback(async () => {
        setLoading(true);
        setError(null);

        const [githubResult, jiraResult] = await Promise.allSettled([
            fetchGithubIntegration(groupId),
            fetchJiraIntegration(groupId),
        ]);

        const github =
            githubResult.status === "fulfilled"
                ? githubResult.value
                : { data: { status: "inactive", connectedAt: null } };

        const jira =
            jiraResult.status === "fulfilled"
                ? jiraResult.value
                : { data: { status: "inactive", connectedAt: null } };

        const bothFailed =
            githubResult.status === "rejected" && jiraResult.status === "rejected";

        if (bothFailed) {
            const msg =
                githubResult.status === "rejected"
                    ? (githubResult.reason as Error)?.message ?? "Failed to fetch status"
                    : "Failed to fetch status";
            setError(msg);
            setStatus(null);
        } else {
            setStatus({
                github: {
                    connected: github.data?.status === "active",
                    connectedAt: github.data?.connectedAt ?? null,
                },
                jira: {
                    connected: jira.data?.status === "active",
                    connectedAt: jira.data?.connectedAt ?? null,
                },
            });
        }

        setLoading(false);
    }, [groupId]);

    useEffect(() => {
        load();
    }, [load]);

    return { status, loading, error, refetch: load };
}
