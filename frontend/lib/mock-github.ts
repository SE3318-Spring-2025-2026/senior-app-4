import { GithubIntegration, IntegrationTestResult } from "@/lib/github-types";

export const mockGithubIntegrations: GithubIntegration[] = [
    {
        integrationId: 1,
        groupId: 101,
        type: "github",
        status: "active",
        organizationName: "SPMS-Alpha-Team",
        connectedAt: "2026-04-10T14:20:00Z",
    },
    {
        integrationId: 2,
        groupId: 103,
        type: "github",
        status: "active",
        organizationName: "Code-Crafters-Org",
        connectedAt: "2026-04-08T09:15:00Z",
    },
];

export const mockIntegrationTests: Record<number, IntegrationTestResult> = {
    101: {
        success: true,
        github: {
            connected: true,
            message: "GitHub organization is reachable.",
        },
        jira: {
            connected: true,
            message: "JIRA space is reachable.",
        },
    },
    102: {
        success: false,
        github: {
            connected: false,
            message: "GitHub organization is not bound.",
        },
        jira: {
            connected: true,
            message: "JIRA space is reachable.",
        },
    },
    103: {
        success: false,
        github: {
            connected: true,
            message: "GitHub organization is reachable.",
        },
        jira: {
            connected: false,
            message: "JIRA space is not bound.",
        },
    },
    104: {
        success: false,
        github: {
            connected: false,
            message: "GitHub organization is not bound.",
        },
        jira: {
            connected: false,
            message: "JIRA space is not bound.",
        },
    },
};