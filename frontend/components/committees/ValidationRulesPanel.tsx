"use client";

import { useState, useEffect } from "react";
import apiClient from "@/lib/client";

interface ValidationRules {
  committeeSizeRequirements?: string;
  advisorQualifications?: string;
  scheduleRules?: string;
  groupAssignmentRules?: string;
}

export default function ValidationRulesPanel({ committeeId }: { committeeId: string }) {
  const [rules, setRules] = useState<ValidationRules | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchRules = async () => {
      try {
        const response = await apiClient.get(`/committees/${committeeId}/validation-rules`);
        setRules(response.data);
      } catch (error) {
        // apiClient handles the toast error
      } finally {
        setLoading(false);
      }
    };
    fetchRules();
  }, [committeeId]);

  if (loading) {
    return <div className="animate-pulse h-32 bg-white/5 rounded-xl border border-white/10 mb-6"></div>;
  }

  if (!rules) return null;

  return (
    <div className="bg-gray-900 border border-white/10 rounded-xl overflow-hidden mb-6">
      <div className="px-5 py-4 border-b border-white/5 bg-white/5 flex items-center gap-2">
        <svg className="w-5 h-5 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
        </svg>
        <h2 className="text-sm font-semibold text-white">Validation Rules</h2>
      </div>
      <div className="p-5 grid grid-cols-1 md:grid-cols-2 gap-6">
        <RuleSection title="Committee Size Requirements" content={rules.committeeSizeRequirements} />
        <RuleSection title="Advisor Qualifications" content={rules.advisorQualifications} />
        <RuleSection title="Schedule Rules" content={rules.scheduleRules} />
        <RuleSection title="Group Assignment Rules" content={rules.groupAssignmentRules} />
      </div>
    </div>
  );
}

function RuleSection({ title, content }: { title?: string; content?: string }) {
  if (!content) return null;
  return (
    <div className="space-y-1.5">
      <p className="text-xs font-semibold text-blue-400 uppercase tracking-wider">{title}</p>
      <p className="text-sm text-gray-300 leading-relaxed">{content}</p>
    </div>
  );
}
