"use client";

import { useState } from "react";
import { RefreshCw } from "lucide-react";
import { showToast } from "@/components/toast/ToastContext";
import {
    bindGithubIntegration,
    bindJiraIntegration,
    triggerScrumSync,
} from "@/lib/integrations-api";

interface Props {
    groupId: number | null;
    onSuccess?: () => void;
}

interface FormErrors {
    githubPat?: string;
    organizationName?: string;
    repositoryName?: string;
    jiraSpaceUrl?: string;
    jiraEmail?: string;
    jiraApiKey?: string;
    projectKey?: string;
}

export default function IntegrationSettingsForm({ groupId, onSuccess }: Props) {
    const [githubPat, setGithubPat] = useState("");
    const [organizationName, setOrganizationName] = useState("");
    const [repositoryName, setRepositoryName] = useState("");
    const [jiraSpaceUrl, setJiraSpaceUrl] = useState("");
    const [jiraEmail, setJiraEmail] = useState("");
    const [jiraApiKey, setJiraApiKey] = useState("");
    const [projectKey, setProjectKey] = useState("");
    const [errors, setErrors] = useState<FormErrors>({});
    const [submitting, setSubmitting] = useState(false);
    const [syncing, setSyncing] = useState(false);

    function validate(): FormErrors {
        const errs: FormErrors = {};
        const githubTouched = githubPat.trim() || organizationName.trim() || repositoryName.trim();
        const jiraTouched = jiraSpaceUrl.trim() || jiraEmail.trim() || jiraApiKey.trim() || projectKey.trim();

        if (githubTouched) {
            if (!githubPat.trim()) errs.githubPat = "GitHub PAT is required";
            if (!organizationName.trim()) errs.organizationName = "Organization name is required";
            if (!repositoryName.trim()) errs.repositoryName = "Repository name is required";
        }
        if (jiraTouched) {
            if (!jiraSpaceUrl.trim()) errs.jiraSpaceUrl = "JIRA Space URL is required";
            if (!jiraEmail.trim()) errs.jiraEmail = "Account email is required";
            if (!projectKey.trim()) errs.projectKey = "Project key is required";
        }
        if (!githubTouched && !jiraTouched) {
            errs.jiraSpaceUrl = "JIRA Space URL is required";
        }
        return errs;
    }

    async function handleSubmit(e: { preventDefault(): void }) {
        e.preventDefault();
        const errs = validate();
        if (Object.keys(errs).length > 0) {
            setErrors(errs);
            return;
        }
        setErrors({});

        if (!groupId) {
            showToast("Group not found. Please join a group first.", "error");
            return;
        }

        setSubmitting(true);
        try {
            const tasks: Promise<void>[] = [];

            if (githubPat.trim() && organizationName.trim() && repositoryName.trim()) {
                tasks.push(bindGithubIntegration(
                    groupId,
                    githubPat.trim(),
                    organizationName.trim(),
                    repositoryName.trim()
                ));
            }
            if (jiraSpaceUrl.trim() && jiraEmail.trim() && jiraApiKey.trim() && projectKey.trim()) {
                tasks.push(bindJiraIntegration(groupId, jiraSpaceUrl.trim(), jiraEmail.trim(), jiraApiKey.trim(), projectKey.trim()));
            }

            await Promise.all(tasks);

            setGithubPat("");
            setOrganizationName("");
            setRepositoryName("");
            setJiraSpaceUrl("");
            setJiraEmail("");
            setJiraApiKey("");
            setProjectKey("");
            showToast("Settings saved successfully", "success");
            onSuccess?.();
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : "An error occurred";
            showToast(msg, "error");
        } finally {
            setSubmitting(false);
        }
    }

    async function handleSyncNow() {
        setSyncing(true);
        try {
            await triggerScrumSync();
            showToast("Sync started successfully", "success");
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : "Sync failed";
            showToast(msg, "error");
        } finally {
            setSyncing(false);
        }
    }

    return (
        <div className="space-y-6">
            {/* GitHub Section */}
            <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
                <h2 className="text-lg font-semibold text-white mb-4">GitHub Integration</h2>
                <div className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-300 mb-1.5">
                            Personal Access Token
                        </label>
                        <input
                            name="githubPat"
                            type="password"
                            placeholder="ghp_..."
                            value={githubPat}
                            onChange={(e) => setGithubPat(e.target.value)}
                            className="w-full bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                        />
                        {errors.githubPat && (
                            <p className="mt-1.5 text-sm text-red-400">{errors.githubPat}</p>
                        )}
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-300 mb-1.5">
                            Organization Name
                        </label>
                        <input
                            name="organizationName"
                            type="text"
                            placeholder="your-github-org"
                            value={organizationName}
                            onChange={(e) => setOrganizationName(e.target.value)}
                            className="w-full bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                        />
                        {errors.organizationName && (
                            <p className="mt-1.5 text-sm text-red-400">{errors.organizationName}</p>
                        )}
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-300 mb-1.5">
                            Repository Name
                        </label>
                        <input
                            name="repositoryName"
                            type="text"
                            placeholder="senior-project-app"
                            value={repositoryName}
                            onChange={(e) => setRepositoryName(e.target.value)}
                            className="w-full bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                        />
                        {errors.repositoryName && (
                            <p className="mt-1.5 text-sm text-red-400">{errors.repositoryName}</p>
                        )}
                    </div>
                </div>
            </div>

            {/* JIRA Section */}
            <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
                <h2 className="text-lg font-semibold text-white mb-4">JIRA Integration</h2>
                <div className="space-y-4">
                    <div>
                        <label className="block text-sm font-medium text-gray-300 mb-1.5">
                            Space URL
                        </label>
                        <input
                            name="jiraSpaceUrl"
                            type="url"
                            placeholder="https://your-domain.atlassian.net"
                            value={jiraSpaceUrl}
                            onChange={(e) => setJiraSpaceUrl(e.target.value)}
                            className="w-full bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                        />
                        {errors.jiraSpaceUrl && (
                            <p className="mt-1.5 text-sm text-red-400">{errors.jiraSpaceUrl}</p>
                        )}
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-300 mb-1.5">
                            Account Email
                        </label>
                        <input
                            name="jiraEmail"
                            type="email"
                            placeholder="you@yourcompany.com"
                            value={jiraEmail}
                            onChange={(e) => setJiraEmail(e.target.value)}
                            className="w-full bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                        />
                        {errors.jiraEmail && (
                            <p className="mt-1.5 text-sm text-red-400">{errors.jiraEmail}</p>
                        )}
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-300 mb-1.5">
                            API Token
                        </label>
                        <input
                            name="jiraApiKey"
                            type="password"
                            placeholder="Atlassian API token"
                            value={jiraApiKey}
                            onChange={(e) => setJiraApiKey(e.target.value)}
                            className="w-full bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                        />
                        {errors.jiraApiKey && (
                            <p className="mt-1.5 text-sm text-red-400">{errors.jiraApiKey}</p>
                        )}
                    </div>
                    <div>
                        <label className="block text-sm font-medium text-gray-300 mb-1.5">
                            Project Key
                        </label>
                        <input
                            name="projectKey"
                            type="text"
                            placeholder="PROJ"
                            value={projectKey}
                            onChange={(e) => setProjectKey(e.target.value)}
                            className="w-full bg-black/40 border border-white/10 text-white rounded-xl px-4 py-3 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                        />
                        {errors.projectKey && (
                            <p className="mt-1.5 text-sm text-red-400">{errors.projectKey}</p>
                        )}
                    </div>
                </div>
            </div>

            {/* Buttons */}
            <form onSubmit={handleSubmit} className="flex flex-col sm:flex-row gap-3">
                <button
                    type="submit"
                    disabled={submitting || !groupId}
                    className="bg-blue-600 text-white px-6 py-3 rounded-xl font-medium hover:bg-blue-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                >
                    {submitting ? (
                        <>
                            <RefreshCw className="lucide-loader w-4 h-4 animate-spin" />
                            Saving...
                        </>
                    ) : (
                        "Save Settings"
                    )}
                </button>

                <button
                    type="button"
                    onClick={handleSyncNow}
                    disabled={syncing}
                    className="bg-gray-700 text-white px-6 py-3 rounded-xl font-medium hover:bg-gray-600 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                >
                    {syncing ? (
                        <>
                            <RefreshCw className="lucide-loader w-4 h-4 animate-spin" />
                            Syncing...
                        </>
                    ) : (
                        "Sync Now"
                    )}
                </button>
            </form>
        </div>
    );
}
