import Link from "next/link";
import Image from "next/image";
import { Group } from "@/lib/group-types";
import StatusBadge from "@/components/StatusBadge";
import { deleteGroupApi } from "@/lib/groups-api";
import { useState } from "react";
import { showToast } from "@/components/toast/ToastContext";


type Props = {
    group: Group;
    isOwnGroup?: boolean;
    isLeader?: boolean;
    onDisbanded?: () => void;
};

export default function GroupCard({ group, isOwnGroup = false, isLeader = false, onDisbanded }: Props) {
    const [disbanding, setDisbanding] = useState(false);

    const handleDisband = async (e: React.MouseEvent) => {
        e.preventDefault();
        e.stopPropagation();

        const confirmed = window.confirm(
            "Are you sure you want to disband this group? This action cannot be undone."
        );
        if (!confirmed) return;

        setDisbanding(true);
        try {
            await deleteGroupApi(group.groupId);
            showToast("Group disbanded successfully!", "success");
            onDisbanded?.();
        } catch (err) {
            const message = err instanceof Error ? err.message : "Failed to disband group.";
            showToast(message, "error");
        } finally {
            setDisbanding(false);
        }
    };

    return (
        <Link href={`/groups/${group.groupId}`}>
            <div
                className={[
                    "cursor-pointer rounded-2xl p-5 transition-all duration-200 backdrop-blur border",
                    "hover:scale-[1.02] hover:shadow-xl hover:shadow-black/30",

                    isOwnGroup
                        ? "bg-blue-500/10 border-blue-400/50 shadow-lg shadow-blue-500/10 hover:shadow-blue-500/20 hover:border-blue-300/60"
                        : "bg-gray-900/70 border-white/10 shadow-lg shadow-black/20 hover:bg-white/5 hover:border-blue-500/30",
                ].join(" ")}
            >
                <div className="mb-3 flex items-center justify-between">
                    <h2 className="text-lg font-semibold text-white">{group.groupName}</h2>
                    <StatusBadge status={group.status} />
                </div>

                {isOwnGroup && (
                    <p className="mb-2 text-sm font-medium text-blue-300">Your group</p>
                )}

                <div className="space-y-1 text-sm text-gray-400">
                    <p>
                        Members: <span className="text-white">{group.memberCount}</span>
                    </p>
                    <p>
                        Advisor:{" "}
                        <span className="text-white">
                            {group.advisorName || "Not Assigned"}
                        </span>
                    </p>
                </div>

                <div className="mt-4 flex gap-5 text-sm">
                    <div className="flex items-center gap-2">
                        <Image
                            src="/git.svg"
                            alt="GitHub"
                            width={16}
                            height={16}
                            className="opacity-90"
                        />
                        <span
                            className={
                                group.githubBound ? "text-green-400" : "text-gray-500"
                            }
                        >
                            GitHub {group.githubBound ? "✔" : "✖"}
                        </span>
                    </div>

                    <div className="flex items-center gap-2">
                        <Image
                            src="/jira-1.svg"
                            alt="JIRA"
                            width={16}
                            height={16}
                            className="opacity-90"
                        />
                        <span
                            className={group.jiraBound ? "text-green-400" : "text-gray-500"}
                        >
                            JIRA {group.jiraBound ? "✔" : "✖"}
                        </span>
                    </div>
                </div>

                {isLeader && (
                    <div className="mt-4 pt-4 border-t border-white/10">
                        <button
                            onClick={handleDisband}
                            disabled={disbanding}
                            className="w-full rounded-lg bg-red-600/20 text-red-400 border border-red-500/30 px-3 py-2 text-xs font-medium hover:bg-red-600/30 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                        >
                            {disbanding ? "Disbanding..." : "Disband Group"}
                        </button>
                    </div>
                )}
            </div>
        </Link>
    );
}