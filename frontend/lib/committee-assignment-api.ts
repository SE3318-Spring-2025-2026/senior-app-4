
import { 
    CommitteeValidationRules, 
    AdvisorAssignment, 
    JuryAssignment, 
    AdvisorAssignmentFormValues,
    JuryAssignmentFormValues,
    AdvisorAssignmentStatus, 
    JuryType                 
} from "./committee-types";


import { getToken } from "@/lib/auth";
import { parseError, API_BASE } from "@/lib/api-utils";

// ═══════════════════════════════════════════════════════════
// NORMALIZATION HELPERS (snake_case -> camelCase)
// ═══════════════════════════════════════════════════════════

function normalizeAdvisorAssignment(raw: any): AdvisorAssignment {
    return {
        assignmentId: raw.assignment_id ?? raw.assignmentId,
        committeeId: raw.committee_id ?? raw.committeeId,
        advisorId: raw.advisor_id ?? raw.advisorId,
        fullName: raw.full_name ?? raw.advisor_name ?? raw.fullName,
        email: raw.email,
        role: raw.role,
        assignedAt: raw.assigned_at ?? raw.assignedAt,
        status: raw.status as AdvisorAssignmentStatus, // <-- Type Casting Eklendi
    };
}

function normalizeJuryAssignment(raw: any): JuryAssignment {
    return {
        assignmentId: raw.assignment_id ?? raw.assignmentId,
        committeeId: raw.committee_id ?? raw.committeeId,
        juryId: raw.jury_id ?? raw.juryId,
        fullName: raw.full_name ?? raw.jury_name ?? raw.fullName,
        email: raw.email,
        juryType: (raw.jury_type ?? raw.juryType) as JuryType, // <-- Type Casting Eklendi
        assignedAt: raw.assigned_at ?? raw.assignedAt,
    };
}

function normalizeValidationRules(raw: any): CommitteeValidationRules {
    return {
        sizeRequirements: raw.size_requirements ?? raw.sizeRequirements ?? [],
        advisorQualifications: raw.advisor_qualifications ?? raw.advisorQualifications ?? [],
        scheduleRules: raw.schedule_rules ?? raw.scheduleRules ?? [],
        groupAssignmentRules: raw.group_assignment_rules ?? raw.groupAssignmentRules ?? [],
    };
}

// ═══════════════════════════════════════════════════════════
// VALIDATION RULES API
// ═══════════════════════════════════════════════════════════

export async function fetchValidationRules(committeeId: number): Promise<CommitteeValidationRules> {
    const token = getToken(); 
    const res = await fetch(`${API_BASE}/committees/${committeeId}/validation-rules`, {
        headers: { Authorization: `Bearer ${token ?? ""}` },
        cache: "no-store",
    });
    if (!res.ok) throw new Error(await parseError(res)); 
    const data = await res.json();
    return normalizeValidationRules(data);
}

// ═══════════════════════════════════════════════════════════
// ADVISOR API
// ═══════════════════════════════════════════════════════════

export async function fetchAdvisors(committeeId: number): Promise<AdvisorAssignment[]> {
    const token = getToken();
    const res = await fetch(`${API_BASE}/committees/${committeeId}/advisors`, {
        headers: { Authorization: `Bearer ${token ?? ""}` },
        cache: "no-store",
    });
    if (!res.ok) throw new Error(await parseError(res));
    const data = await res.json();
    return (Array.isArray(data) ? data : data.content || []).map(normalizeAdvisorAssignment);
}

export async function assignAdvisor(committeeId: number, values: AdvisorAssignmentFormValues): Promise<AdvisorAssignment> {
    const token = getToken();
    const payload = { advisor_id: values.advisorId, role: values.role };
    
    const res = await fetch(`${API_BASE}/committees/${committeeId}/advisors`, {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token ?? ""}` },
        body: JSON.stringify(payload),
    });
    if (!res.ok) throw new Error(await parseError(res));
    return normalizeAdvisorAssignment(await res.json());
}

export async function removeAdvisor(committeeId: number, assignmentId: number): Promise<void> {
    const token = getToken();
    const res = await fetch(`${API_BASE}/committees/${committeeId}/advisors/${assignmentId}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token ?? ""}` },
    });
    if (!res.ok) throw new Error(await parseError(res));
}

// ═══════════════════════════════════════════════════════════
// JURY API
// ═══════════════════════════════════════════════════════════

export async function fetchJury(committeeId: number): Promise<JuryAssignment[]> {
    const token = getToken();
    const res = await fetch(`${API_BASE}/committees/${committeeId}/jury`, {
        headers: { Authorization: `Bearer ${token ?? ""}` },
        cache: "no-store",
    });
    if (!res.ok) throw new Error(await parseError(res));
    const data = await res.json();
    return (Array.isArray(data) ? data : data.content || []).map(normalizeJuryAssignment);
}

export async function assignJury(committeeId: number, values: JuryAssignmentFormValues): Promise<JuryAssignment> {
    const token = getToken();
    const payload = { jury_id: values.juryId, jury_type: values.juryType };
    
    const res = await fetch(`${API_BASE}/committees/${committeeId}/jury`, {
        method: "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token ?? ""}` },
        body: JSON.stringify(payload),
    });
    if (!res.ok) throw new Error(await parseError(res));
    return normalizeJuryAssignment(await res.json());
}

export async function removeJury(committeeId: number, assignmentId: number): Promise<void> {
    const token = getToken();
    const res = await fetch(`${API_BASE}/committees/${committeeId}/jury/${assignmentId}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token ?? ""}` },
    });
    if (!res.ok) throw new Error(await parseError(res));
}