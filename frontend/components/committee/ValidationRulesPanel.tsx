import { LucideShieldCheck } from 'lucide-react';
import { CommitteeValidationRules } from '@/lib/committee-types';

export default function ValidationRulesPanel({ rules }: { rules: CommitteeValidationRules }) {
  return (
    <div className="bg-blue-50 border border-blue-200 rounded-lg p-6 mb-8">
      <div className="flex items-center gap-2 mb-4 text-blue-800">
        <LucideShieldCheck size={20} />
        <h3 className="font-semibold text-lg">Committee Validation Rules</h3>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
        <section>
          <h4 className="font-medium text-blue-900 mb-1">Size Requirements</h4>
          <ul className="list-disc list-inside text-blue-700">
            {rules.sizeRequirements.map((r, i) => <li key={i}>{r}</li>)}
          </ul>
        </section>
        <section>
          <h4 className="font-medium text-blue-900 mb-1">Advisor Qualifications</h4>
          <ul className="list-disc list-inside text-blue-700">
            {rules.advisorQualifications.map((r, i) => <li key={i}>{r}</li>)}
          </ul>
        </section>
        {/* Schedule Rules */}
        <section>
          <h4 className="font-medium text-blue-900 mb-1">Schedule Rules</h4>
          <ul className="list-disc list-inside text-blue-700">
            <li>Exam dates must be scheduled in the future.</li>
            <li>No overlapping schedules allowed for the same committee.</li>
          </ul>
        </section>

        {/* Group Assignment Rules */}
        <section>
          <h4 className="font-medium text-blue-900 mb-1">Group Assignment Rules</h4>
          <ul className="list-disc list-inside text-blue-700">
            <li>Committee cannot exceed maximum group capacity.</li>
            <li>Groups must meet prerequisite status for assignment.</li>
          </ul>
        </section>
      </div>
    </div>
  );
}