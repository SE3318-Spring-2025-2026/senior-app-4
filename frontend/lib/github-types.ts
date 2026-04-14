export type IntegrationStatus = "active" | "inactive" | "error";

export type GithubIntegration = {
    integrationId: number;
    groupId: number;
    type: "github";
    status: IntegrationStatus;
    organizationName: string | null;
    connectedAt: string | null;
};

export type IntegrationTestResult = {
    success: boolean;
    github: {
        connected: boolean;
        message: string;
    };
    jira: {
        connected: boolean;
        message: string;
    };
};