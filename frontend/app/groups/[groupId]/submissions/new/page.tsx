"use client";

import Link from "next/link";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { Suspense, useEffect, useState } from "react";
import StudentSubmissionForm from "@/components/StudentSubmissionForm";
import { getToken, getUser } from "@/lib/auth";
import { fetchGroupDetail, type ApiGroupDetail } from "@/lib/groups-api";
import { fetchSubmissionReviews, type SubmissionReview } from "@/lib/submissions-api";

type AuthState = "loading" | "ready" | "unauthorized";

export default function NewSubmissionPage() {
  return (
    <Suspense fallback={<SubmissionPageSpinner />}>
      <NewSubmissionPageContent />
    </Suspense>
  );
}

function NewSubmissionPageContent() {
  const router = useRouter();
  const params = useParams();
  const searchParams = useSearchParams();
  const [authState, setAuthState] = useState<AuthState>("loading");
  const [userId, setUserId] = useState<number | null>(null);
  const [group, setGroup] = useState<ApiGroupDetail | null>(null);
  const [groupError, setGroupError] = useState<string | null>(null);
  const [reviewerComments, setReviewerComments] = useState<SubmissionReview[]>([]);
  const [commentsLoading, setCommentsLoading] = useState(false);
  const [commentsError, setCommentsError] = useState<string | undefined>();
  const parentSubmissionId = searchParams.get("parentSubmissionId")?.trim() || undefined;
  const isRevisionFlow = Boolean(parentSubmissionId);

  const rawGroupId = params.groupId;
  const groupIdStr = Array.isArray(rawGroupId) ? rawGroupId[0] : rawGroupId;
  const groupIdNum = groupIdStr ? Number(groupIdStr) : null;

  useEffect(() => {
    const token = getToken();
    const user = getUser();

    if (!token || !user) {
      router.replace("/auth/login");
      return;
    }

    if (user.requiresPasswordChange) {
      router.replace("/auth/change-password");
      return;
    }

    if (user.role !== "student") {
      setAuthState("unauthorized");
      return;
    }

    setUserId(user.userId ?? null);
    setAuthState("ready");
  }, [router]);

  useEffect(() => {
    if (!groupIdNum) return;
    let cancelled = false;

    fetchGroupDetail(groupIdNum)
      .then((data) => { if (!cancelled) setGroup(data); })
      .catch((err) => { if (!cancelled) setGroupError(err instanceof Error ? err.message : "Failed to load group."); });

    return () => { cancelled = true; };
  }, [groupIdNum]);

  useEffect(() => {
    let cancelled = false;

    async function loadReviewerComments() {
      if (!parentSubmissionId) {
        setReviewerComments([]);
        setCommentsError(undefined);
        return;
      }

      setCommentsLoading(true);
      setCommentsError(undefined);

      try {
        const response = await fetchSubmissionReviews(parentSubmissionId);
        if (!cancelled) {
          setReviewerComments(response.data ?? []);
        }
      } catch (error) {
        if (!cancelled) {
          setReviewerComments([]);
          setCommentsError(error instanceof Error ? error.message : "Reviewer comments could not be loaded.");
        }
      } finally {
        if (!cancelled) {
          setCommentsLoading(false);
        }
      }
    }

    loadReviewerComments();

    return () => {
      cancelled = true;
    };
  }, [parentSubmissionId]);

  if (authState === "loading") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-gray-950">
        <svg className="h-6 w-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
      </div>
    );
  }

  if (authState === "unauthorized") {
    return (
      <main className="flex min-h-screen items-center justify-center bg-gray-950 px-6 text-white">
        <div className="max-w-md rounded-3xl border border-red-500/20 bg-red-500/8 p-8 text-center">
          <h1 className="text-xl font-semibold">Access restricted</h1>
          <p className="mt-3 text-sm text-red-100/80">Only students can use the deliverable submission form.</p>
        </div>
      </main>
    );
  }

  if (groupError) {
    return (
      <main className="flex min-h-screen items-center justify-center bg-gray-950 px-6 text-white">
        <div className="max-w-md rounded-3xl border border-white/10 bg-gray-900/80 p-8 text-center">
          <h1 className="text-xl font-semibold">Group not found</h1>
          <p className="mt-3 text-sm text-gray-400">{groupError}</p>
        </div>
      </main>
    );
  }

  if (!group) {
    return <SubmissionPageSpinner />;
  }

  const member = group.members.find((m) => String(m.userId) === String(userId));
  const isLeader = member?.role === "LEADER";
  const isMemberOfGroup = Boolean(member);
  const disabled = !isLeader;
  const disabledReason = !isMemberOfGroup
    ? "You are not a member of this group, so the form is view-only."
    : "Only the group leader can submit deliverables.";

  return (
    <main className="min-h-screen bg-gray-950 px-6 py-10 text-white">
      <div className="mx-auto max-w-4xl space-y-8">
        <div className="space-y-4">
          <div>
            <Link href={`/groups/${group.id}`} className="text-sm text-blue-300 transition-colors hover:text-blue-200">
              {"<- Back to group"}
            </Link>
            <h1 className="mt-6 text-3xl font-bold">
              {isRevisionFlow ? "Student Revision Form" : "Student Submission Form"}
            </h1>
            <p className="mt-2 text-gray-400">
              {isRevisionFlow
                ? "Review the committee comments, attach the revised file, and submit it for your group."
                : "Select the deliverable type, attach your file, and submit it for your group."}
            </p>
          </div>

          <div className="rounded-2xl border border-white/10 bg-gray-900/70 p-6 shadow-lg shadow-black/20 backdrop-blur">
            <p className="text-sm text-gray-400">Current group</p>
            <p className="mt-2 text-lg font-semibold text-white">{group.groupName}</p>
            <p className="mt-2 text-sm text-gray-400">
              {isLeader
                ? "You are viewing the form as the group leader."
                : "You can view the form here, but only the group leader can submit."}
            </p>
          </div>
        </div>

        <StudentSubmissionForm
          groupId={String(group.id)}
          groupName={group.groupName}
          disabled={disabled}
          disabledReason={disabled ? disabledReason : undefined}
          mode={isRevisionFlow ? "revision" : "new"}
          parentSubmissionId={parentSubmissionId}
          reviewerComments={reviewerComments}
          commentsLoading={commentsLoading}
          commentsError={commentsError}
        />
      </div>
    </main>
  );
}

function SubmissionPageSpinner() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-gray-950">
      <svg className="h-6 w-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>
  );
}
