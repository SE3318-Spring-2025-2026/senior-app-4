
import { 
    CommitteeValidationRules, 
    AdvisorAssignment, 
    JuryAssignment, 
    AdvisorAssignmentFormValues,
    JuryAssignmentFormValues,
    AdvisorAssignmentStatus, 
    JuryType,
    StudentGroup,
    GroupAssignment,
    AvailabilitySlot,
    AvailabilityStatus,
    SlotType,
    GroupAssignmentStatus               
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

function normalizeStudentGroup(raw: any): StudentGroup {
    return {
        groupId: raw.group_id ?? raw.groupId,
        groupName: raw.group_name ?? raw.groupName,
        projectTitle: raw.project_title ?? raw.projectTitle,
        membersCount: raw.members_count ?? raw.membersCount ?? 0,
        students: (raw.students || []).map((s: any) => ({
            userId: s.user_id ?? s.userId,
            fullName: s.full_name ?? s.fullName,
            email: s.email
        }))
    };
}

function normalizeGroupAssignment(raw: any): GroupAssignment {
    return {
        assignmentId: raw.assignment_id ?? raw.assignmentId,
        committeeId: raw.committee_id ?? raw.committeeId,
        groupId: raw.group_id ?? raw.groupId,
        groupName: raw.group_name ?? raw.groupName,
        assignmentStatus: (raw.assignment_status ?? raw.assignmentStatus) as GroupAssignmentStatus,
        scheduledSlotId: raw.scheduled_slot_id ?? raw.scheduledSlotId ?? null, // Fallback eklendi.
        assignedAt: raw.assigned_at ?? raw.assignedAt,
    };
}

function normalizeAvailabilitySlot(raw: any): AvailabilitySlot {
    return {
        slotId: raw.slot_id ?? raw.slotId,
        committeeId: raw.committee_id ?? raw.committeeId,
        professorId: raw.professor_id ?? raw.professorId,
        professorName: raw.professor_name ?? raw.professorName,
        startDateTime: raw.start_date_time ?? raw.startDateTime,
        endDateTime: raw.end_date_time ?? raw.endDateTime,
        status: (raw.status ?? "AVAILABLE") as AvailabilityStatus,
        slotType: (raw.slot_type ?? raw.slotType ?? "EXAM") as SlotType,
        notes: raw.notes
    };
}

function normalizeValidationRules(raw: any): CommitteeValidationRules {
    return {
        sizeRequirements: raw.size_requirements ?? raw.sizeRequirements ?? [],
        advisorQualifications: raw.advisor_qualifications ?? raw.advisorQualifications ?? [],
        scheduleRules: raw.schedule_rules ?? raw.scheduleRules ?? [],
        groupAssignmentRules: raw.group_assignment_rules ?? raw.groupAssignmentRules ?? [],
        maxGroupsPerCommittee: raw.max_groups_per_committee ?? raw.maxGroupsPerCommittee ?? 10,
        minAdvisors: raw.min_advisors ?? raw.minAdvisors ?? 2,
        minJury: raw.min_jury ?? raw.minJury ?? 3,
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

// ═══════════════════════════════════════════════════════════
// 🆕 ISSUE#323 - GROUP & CALENDAR API
// ═══════════════════════════════════════════════════════════

export async function fetchAvailableGroups(committeeId: number): Promise<StudentGroup[]> {
    const token = getToken();
    const res = await fetch(`${API_BASE}/committees/${committeeId}/available-groups`, {
        headers: { Authorization: `Bearer ${token ?? ""}` },
        cache: "no-store",
    });
    if (!res.ok) throw new Error(await parseError(res));
    const data = await res.json();
    return (Array.isArray(data) ? data : data.content || []).map(normalizeStudentGroup);
}

export async function assignGroupToCommittee(committeeId: number, groupId: number): Promise<GroupAssignment> {
    const token = getToken();
    const res = await fetch(`${API_BASE}/committees/${committeeId}/groups/${groupId}`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token ?? ""}` },
    });
    if (!res.ok) throw new Error(await parseError(res));
    return normalizeGroupAssignment(await res.json());
}

export async function fetchCommitteeAvailability(committeeId: number): Promise<AvailabilitySlot[]> {
    const token = getToken();
    const res = await fetch(`${API_BASE}/committees/${committeeId}/availability`, {
        headers: { Authorization: `Bearer ${token ?? ""}` },
        cache: "no-store",
    });
    if (!res.ok) throw new Error(await parseError(res));
    const data = await res.json();
    return (Array.isArray(data) ? data : data.content || []).map(normalizeAvailabilitySlot);
}

export async function saveAvailabilitySlot(data: Partial<AvailabilitySlot>): Promise<AvailabilitySlot> {
    const token = getToken();
    
    const payload: Record<string, any> = {};
    if (data.committeeId !== undefined) payload.committee_id = data.committeeId;
    if (data.professorId !== undefined) payload.professor_id = data.professorId;
    if (data.startDateTime !== undefined) payload.start_date_time = data.startDateTime;
    if (data.endDateTime !== undefined) payload.end_date_time = data.endDateTime;
    if (data.status !== undefined) payload.status = data.status;
    if (data.slotType !== undefined) payload.slot_type = data.slotType;
    if (data.notes !== undefined) payload.notes = data.notes;

    const res = await fetch(`${API_BASE}/availability-slots`, {
        method: "POST",
        headers: { 
            "Content-Type": "application/json", 
            Authorization: `Bearer ${token ?? ""}` 
        },
        body: JSON.stringify(payload),
    });
    if (!res.ok) throw new Error(await parseError(res));
    return normalizeAvailabilitySlot(await res.json());
}

export async function removeGroupAssignment(committeeId: number, assignmentId: number): Promise<void> {
    const token = getToken();
    const res = await fetch(`${API_BASE}/committees/${committeeId}/group-assignments/${assignmentId}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token ?? ""}` },
    });
    if (!res.ok) throw new Error(await parseError(res));
}

export async function fetchAssignedGroups(committeeId: number): Promise<GroupAssignment[]> {
    const token = getToken();
    const res = await fetch(`${API_BASE}/committees/${committeeId}/group-assignments`, {
        headers: { Authorization: `Bearer ${token ?? ""}` },
        cache: "no-store",
    });
    if (!res.ok) throw new Error(await parseError(res));
    const data = await res.json();
    return (Array.isArray(data) ? data : data.content || []).map(normalizeGroupAssignment);
}

export async function scheduleGroupPresentation(
    committeeId: number,
    assignmentId: number,
    startDateTime: string,
    endDateTime: string
): Promise<GroupAssignment> {
    const token = getToken();
    const res = await fetch(`${API_BASE}/committees/${committeeId}/group-assignments/${assignmentId}/schedule`, {
        method: "PUT", // veya backend'in beklediği metoda göre POST/PATCH
        headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token ?? ""}`,
        },
        body: JSON.stringify({
            start_date_time: startDateTime,
            end_date_time: endDateTime,
        }),
    });
    
    if (!res.ok) throw new Error(await parseError(res));
    const data = await res.json();
    return normalizeGroupAssignment(data);
}