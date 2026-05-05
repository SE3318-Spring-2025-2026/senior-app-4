"use client";

import { useEffect, useState, useCallback } from "react";
import { Loader2, LayoutGrid, ClipboardList } from "lucide-react";
import { 
    CommitteeValidationRules, 
    AdvisorAssignment, 
    JuryAssignment, 
    GroupAssignment,
    AvailabilitySlot
} from "@/lib/committee-types";
import { 
    fetchValidationRules, 
    fetchAdvisors, 
    fetchJury,
    fetchAssignedGroups,
    fetchCommitteeAvailability
} from "@/lib/committee-assignment-api";
import { handleApiError } from "@/lib/error-handler";

import { GroupSelectionPanel } from "./GroupSelectionPanel";
import { GroupSchedulingPanel } from "./GroupSchedulingPanel";
import ValidationRulesPanel from "@/components/committee/ValidationRulesPanel";
import AdvisorAssignmentPanel from "@/components/committees/AdvisorAssignmentPanel";
import JuryAssignmentPanel from "@/components/committees/JuryAssignmentPanel";

interface Props {
    committeeId: number;
}

export default function CommitteeAssignmentManager({ committeeId }: Props) {
    const [rules, setRules] = useState<CommitteeValidationRules | null>(null);
    const [advisors, setAdvisors] = useState<AdvisorAssignment[]>([]);
    const [jury, setJury] = useState<JuryAssignment[]>([]);
    const [assignedGroups, setAssignedGroups] = useState<GroupAssignment[]>([]);
    const [availabilities, setAvailabilities] = useState<AvailabilitySlot[]>([]);
    const [loadingRules, setLoadingRules] = useState(true);
    const [loadingAdvisors, setLoadingAdvisors] = useState(true);
    const [loadingJury, setLoadingJury] = useState(true);
    const [loadingGroups, setLoadingGroups] = useState(true);

    const loadAvailabilities = useCallback(async () => {
        try {
            const data = await fetchCommitteeAvailability(committeeId);
            setAvailabilities(data);
        } catch (error) {
            handleApiError(error, "Failed to load professor availabilities");
        }
    }, [committeeId]);

    useEffect(() => {
        loadAvailabilities();
    }, [loadAvailabilities]);

    const getStatusBadge = (status: string | undefined) => {
        const s = status?.toUpperCase() || "ASSIGNED";
        switch(s) {
            case 'ASSIGNED': 
                return <span className="bg-orange-500/10 text-orange-400 border border-orange-500/20 px-2 py-0.5 rounded-full text-[10px] font-semibold tracking-wider">ASSIGNED</span>;
            case 'SCHEDULED': 
                return <span className="bg-blue-500/10 text-blue-400 border border-blue-500/20 px-2 py-0.5 rounded-full text-[10px] font-semibold tracking-wider">SCHEDULED</span>;
            case 'COMPLETED': 
                return <span className="bg-green-500/10 text-green-400 border border-green-500/20 px-2 py-0.5 rounded-full text-[10px] font-semibold tracking-wider">COMPLETED</span>;
            case 'CANCELLED': 
                return <span className="bg-red-500/10 text-red-400 border border-red-500/20 px-2 py-0.5 rounded-full text-[10px] font-semibold tracking-wider">CANCELLED</span>;
            default: 
                return <span className="bg-gray-500/10 text-gray-400 border border-gray-500/20 px-2 py-0.5 rounded-full text-[10px] font-semibold tracking-wider">{s}</span>;
        }
    };

    const loadData = useCallback(async () => {
        setLoadingRules(true);
        setLoadingAdvisors(true);
        setLoadingJury(true);
        setLoadingGroups(true);

        const [rulesRes, advisorsRes, juryRes, groupsRes] = await Promise.allSettled([
            fetchValidationRules(committeeId),
            fetchAdvisors(committeeId),
            fetchJury(committeeId),
            fetchAssignedGroups(committeeId)
        ]);

        if (rulesRes.status === "fulfilled") {
            setRules(rulesRes.value);
        } else {
            handleApiError(rulesRes.reason, "Failed to load validation rules");
        }
        setLoadingRules(false);

        if (advisorsRes.status === "fulfilled") {
            setAdvisors(advisorsRes.value);
        } else {
            handleApiError(advisorsRes.reason, "Failed to load advisors");
        }
        setLoadingAdvisors(false);

        if (juryRes.status === "fulfilled") {
            setJury(juryRes.value);
        } else {
            handleApiError(juryRes.reason, "Failed to load jury members");
        }
        setLoadingJury(false);

        if (groupsRes.status === "fulfilled") {
            setAssignedGroups(groupsRes.value);
        } else {
            handleApiError(groupsRes.reason, "Failed to load assigned groups");
        }
        setLoadingGroups(false);

    }, [committeeId]);

    useEffect(() => {
        loadData();
    }, [loadData]);

    const handleGroupAssigned = (newAssignment: GroupAssignment) => {
        setAssignedGroups((prev) => [...prev, newAssignment]);
    };

    return (
        <div className="max-w-[1600px] mx-auto p-4 lg:p-8">
            <div className="flex items-center gap-3 mb-8">
                <div className="p-3 bg-blue-500/10 rounded-xl">
                    <LayoutGrid className="text-blue-500" size={24} />
                </div>
                <div>
                    <h1 className="text-2xl font-bold text-white">Committee Management</h1>
                    <p className="text-gray-400 text-sm">Orchestrate assignments and verify scheduling requirements.</p>
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-2 space-y-8">
                    <div className={loadingAdvisors ? "opacity-50 pointer-events-none" : ""}>
                        <AdvisorAssignmentPanel 
                            committeeId={committeeId} 
                            advisors={advisors} 
                            onRefresh={loadData} 
                        />
                    </div>

                    <div className={loadingJury ? "opacity-50 pointer-events-none" : ""}>
                        <JuryAssignmentPanel 
                            committeeId={committeeId} 
                            jury={jury} 
                            onRefresh={loadData} 
                        />
                    </div>

                    <div className="rounded-2xl border border-white/10 bg-gray-900/40 p-6 backdrop-blur-sm">
                        <h3 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                            <ClipboardList size={20} className="text-emerald-500" />
                            Active Group Assignments ({assignedGroups.length})
                        </h3>
                        {loadingGroups ? (
                            <div className="flex justify-center p-4">
                                <Loader2 className="animate-spin text-gray-500" />
                            </div>
                        ) : assignedGroups.length === 0 ? (
                            <p className="text-sm text-gray-500 italic">No group assignments detected for this committee.</p>
                        ) : (
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                {assignedGroups.map((assignment) => (
                                    <div key={assignment.assignmentId} className="p-4 rounded-xl bg-white/5 border border-white/5 flex flex-col gap-2">
                                        <div className="flex justify-between items-start">
                                            <div className="flex flex-col">
                                                <span className="text-sm font-medium text-white">{assignment.groupName}</span>
                                                <span className="text-[11px] text-gray-500">
                                                    Members: {assignment.membersCount || '?'} | Since: {new Date(assignment.assignedAt).toLocaleDateString()}
                                                </span>
                                            </div>
                                            {getStatusBadge(assignment.assignmentStatus)}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>

                <div className="space-y-8">
                    {loadingRules ? (
                        <div className="h-32 animate-pulse rounded-2xl bg-gray-900/30 border border-white/5" />
                    ) : (
                        rules && <ValidationRulesPanel rules={rules} />
                    )}

                    <GroupSelectionPanel
                        committeeId={committeeId}
                        onGroupAssigned={handleGroupAssigned}
                        currentGroupCount={assignedGroups.length}
                        maxGroupsAllowed={rules?.maxGroupsPerCommittee || 99}
                    />
                    <GroupSchedulingPanel
                        committeeId={committeeId}
                        assignedGroups={assignedGroups}
                        availabilities={availabilities}
                        onScheduleUpdated={() => {
                            loadData();
                        }}
                    />
                </div>
            </div>
        </div>
    );
}