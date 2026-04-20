"use client";

import { useParams, useRouter } from "next/navigation";
import ValidationRulesPanel from "@/components/committees/ValidationRulesPanel";
import AdvisorJuryAssignment from "@/components/committees/AdvisorJuryAssignment";
import GroupAssignment from "@/components/committees/GroupAssignment";

export default function CommitteeDetailPage() {
  const params = useParams();
  const committeeId = params.committeeId as string;
  const router = useRouter();

  return (
    <div className="min-h-screen bg-gray-950 flex flex-col">
      <header className="border-b border-white/5 px-8 py-4 bg-gray-900/50 flex items-center justify-between sticky top-0 z-10 backdrop-blur-sm">
        <div className="flex items-center gap-4">
          <button 
            onClick={() => router.back()}
            className="p-2 bg-white/5 hover:bg-white/10 rounded-lg text-gray-400 transition-colors"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
          </button>
          <div>
            <h1 className="text-lg font-semibold text-white">Committee Details</h1>
            <p className="text-xs text-gray-500 mt-0.5">Manage assignments and schedule for Committee #{committeeId}</p>
          </div>
        </div>
      </header>

      <main className="flex-1 p-8 overflow-y-auto">
        <div className="max-w-6xl mx-auto space-y-8">
          <ValidationRulesPanel committeeId={committeeId} />
          
          <div className="space-y-6">
            <h2 className="text-lg font-semibold text-white pb-2 border-b border-white/10">Staff Assignment</h2>
            <AdvisorJuryAssignment committeeId={committeeId} />
          </div>

          <div className="space-y-6">
            <h2 className="text-lg font-semibold text-white pb-2 border-b border-white/10">Group Assignment & Scheduling</h2>
            <GroupAssignment committeeId={committeeId} />
          </div>
        </div>
      </main>
    </div>
  );
}
