"use client";

import { useEffect, useState, useCallback } from "react";
import { Loader2 } from "lucide-react";
import { CommitteeValidationRules, AdvisorAssignment, JuryAssignment } from "@/lib/committee-types";
import { fetchValidationRules, fetchAdvisors, fetchJury } from "@/lib/committee-assignment-api";
import { handleApiError } from "@/lib/error-handler";

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
    
    const [loadingRules, setLoadingRules] = useState(true);
    const [loadingAdvisors, setLoadingAdvisors] = useState(true);
    const [loadingJury, setLoadingJury] = useState(true);

    const loadRules = useCallback(async () => {
        setLoadingRules(true);
        try {
            const data = await fetchValidationRules(committeeId);
            setRules(data);
        } catch (error) {
        } finally {
            setLoadingRules(false);
        }
    }, [committeeId]);

    const loadAdvisors = useCallback(async () => {
        setLoadingAdvisors(true);
        try {
            const data = await fetchAdvisors(committeeId);
            setAdvisors(data);
        } catch (error) {
            handleApiError(error, "Advisors");
        } finally {
            setLoadingAdvisors(false);
        }
    }, [committeeId]);

    const loadJury = useCallback(async () => {
        setLoadingJury(true);
        try {
            const data = await fetchJury(committeeId);
            setJury(data);
        } catch (error) {
            handleApiError(error, "Jury");
        } finally {
            setLoadingJury(false);
        }
    }, [committeeId]);

    useEffect(() => {
        loadRules();
        loadAdvisors();
        loadJury();
    }, [loadRules, loadAdvisors, loadJury]);

    return (
        <div className="space-y-6">
            {loadingRules ? (
                <div className="h-32 animate-pulse rounded-2xl border border-white/5 bg-gray-900/30" />
            ) : (
                rules && <ValidationRulesPanel rules={rules} />
            )}

            <div className={loadingAdvisors ? "opacity-50 pointer-events-none transition-opacity" : ""}>
                <AdvisorAssignmentPanel 
                    committeeId={committeeId} 
                    advisors={advisors} 
                    onRefresh={loadAdvisors} 
                />
            </div>

            <div className={loadingJury ? "opacity-50 pointer-events-none transition-opacity" : ""}>
                <JuryAssignmentPanel 
                    committeeId={committeeId} 
                    jury={jury} 
                    onRefresh={loadJury} 
                />
            </div>

            {(loadingAdvisors || loadingJury) && (
                <div className="fixed bottom-4 right-4 flex items-center gap-2 rounded-full bg-blue-600 px-4 py-2 text-xs font-medium text-white shadow-lg z-50">
                    <Loader2 size={14} className="animate-spin" />
                    Syncing...
                </div>
            )}
        </div>
    );
}