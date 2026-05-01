"use client";

import { useState } from "react";
import { CommitteeFormValues, CommitteeStatus } from "@/lib/committee-types";

type Props = {
    initialValues?: Partial<CommitteeFormValues>;
    onSubmit: (values: CommitteeFormValues) => Promise<void> | void;
    isSubmitting?: boolean;
    apiError?: string;
    submitLabel?: string;
};

export default function CommitteeForm({
    initialValues,
    onSubmit,
    isSubmitting = false,
    apiError = "",
    submitLabel = "Submit",
}: Props) {
    const [committeeName, setCommitteeName] = useState(
        initialValues?.committeeName ?? ""
    );
    const [description, setDescription] = useState(
        initialValues?.description ?? ""
    );
    const [status, setStatus] = useState<CommitteeStatus>(
        initialValues?.status ?? "FORMING"
    );
    const [validationError, setValidationError] = useState("");

    async function handleSubmit(e: React.FormEvent<HTMLFormElement>) {
        e.preventDefault();
        setValidationError("");

        const trimmedName = committeeName.trim();

        if (!trimmedName) {
            setValidationError("Committee name is required.");
            return;
        }

        if (trimmedName.length < 3 || trimmedName.length > 100) {
            setValidationError("Committee name must be between 3 and 100 characters.");
            return;
        }

        await onSubmit({
            committeeName: trimmedName,
            description: description.trim() || undefined,
            status,
        });
    }

    const errorMessage = validationError || apiError;

    return (
        <form onSubmit={handleSubmit} className="space-y-4">
            {errorMessage && (
                <div
                    role="alert"
                    className="rounded-xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-300"
                >
                    {errorMessage}
                </div>
            )}

            <div>
                <label
                    htmlFor="committeeName"
                    className="mb-2 block text-sm font-medium text-gray-300"
                >
                    Committee Name
                </label>
                <input
                    id="committeeName"
                    type="text"
                    value={committeeName}
                    onChange={(e) => setCommitteeName(e.target.value)}
                    disabled={isSubmitting}
                    className="w-full rounded-xl border border-white/10 bg-gray-900 px-4 py-3 text-sm text-white placeholder:text-gray-500 outline-none focus:border-blue-500"
                    placeholder="Senior Project Jury"
                />
            </div>

            <div>
                <label
                    htmlFor="committeeDescription"
                    className="mb-2 block text-sm font-medium text-gray-300"
                >
                    Description
                </label>
                <textarea
                    id="committeeDescription"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    disabled={isSubmitting}
                    rows={3}
                    className="w-full rounded-xl border border-white/10 bg-gray-900 px-4 py-3 text-sm text-white placeholder:text-gray-500 outline-none focus:border-blue-500"
                    placeholder="Optional description"
                />
            </div>

            <div>
                <label
                    htmlFor="committeeStatus"
                    className="mb-2 block text-sm font-medium text-gray-300"
                >
                    Status
                </label>
                <select
                    id="committeeStatus"
                    value={status}
                    onChange={(e) => setStatus(e.target.value as CommitteeStatus)}
                    disabled={isSubmitting}
                    className="w-full rounded-xl border border-white/10 bg-gray-900 px-4 py-3 text-sm text-white outline-none focus:border-blue-500"
                >
                    <option value="FORMING">FORMING</option>
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="INACTIVE">INACTIVE</option>
                    <option value="COMPLETED">COMPLETED</option>
                </select>
            </div>

            <button
                type="submit"
                disabled={isSubmitting}
                className="inline-flex items-center justify-center gap-2 rounded-xl bg-blue-600 px-5 py-3 text-sm font-semibold text-white hover:bg-blue-500 disabled:cursor-not-allowed disabled:opacity-60"
            >
                {isSubmitting && (
                    <span
                        data-testid="loading-spinner"
                        className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white"
                    />
                )}
                {submitLabel}
            </button>
        </form>
    );
}