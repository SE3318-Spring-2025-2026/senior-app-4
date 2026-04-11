"use client";

import { useState, useRef, useCallback, useEffect } from "react";
import { useRouter } from "next/navigation";
import { toast } from "sonner";
import { getToken, getUser } from "@/lib/auth";

interface UploadResponse {
  message: string;
  totalRecords: number;
  validRecords: number;
  invalidRecords: number;
}

type UploadStatus = "idle" | "dragging" | "uploading" | "success" | "error";

export default function StudentIdUploadPage() {
  const router = useRouter();
  const [role, setRole] = useState<string | null>(null);

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
    setRole(user.role);
  }, [router]);

  if (role === null) return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center">
      <svg className="w-6 h-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>
  );
  if (role !== "coordinator") return <AccessDenied />;
  return <DashboardLayout />;
}

function AccessDenied() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-950">
      <div className="text-center space-y-4">
        <div className="w-16 h-16 rounded-2xl bg-red-500/10 border border-red-500/20 flex items-center justify-center mx-auto">
          <svg className="w-7 h-7 text-red-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 9v3.75m-9.303 3.376c-.866 1.5.217 3.374 1.948 3.374h14.71c1.73 0 2.813-1.874 1.948-3.374L13.949 3.378c-.866-1.5-3.032-1.5-3.898 0L2.697 16.126zM12 15.75h.007v.008H12v-.008z" />
          </svg>
        </div>
        <h1 className="text-lg font-semibold text-white">Access Restricted</h1>
        <p className="text-sm text-gray-500">Only Coordinators and Admins can access this page.</p>
      </div>
    </div>
  );
}

function NavItem({ icon, label, active = false }: { icon: React.ReactNode; label: string; active?: boolean }) {
  return (
    <div className={`flex items-center gap-3 px-3 py-2.5 rounded-lg cursor-pointer transition-all ${
      active ? "bg-blue-600 text-white" : "text-gray-400 hover:text-white hover:bg-white/5"
    }`}>
      <span className="w-5 h-5 flex items-center justify-center">{icon}</span>
      <span className="text-sm font-medium">{label}</span>
    </div>
  );
}

function DashboardLayout() {
  return (
    <div className="min-h-screen bg-gray-950 flex">
      {/* Sidebar */}
      <aside className="w-64 border-r border-white/5 flex flex-col shrink-0">
        <div className="px-5 py-5 border-b border-white/5">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-blue-600 flex items-center justify-center shrink-0">
              <svg className="w-4 h-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
              </svg>
            </div>
            <div>
              <p className="text-sm font-semibold text-white">SPMS</p>
              <p className="text-xs text-gray-500">Coordinator Panel</p>
            </div>
          </div>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1">
          <p className="text-xs font-medium text-gray-600 px-3 mb-2 uppercase tracking-widest">Management</p>
          <NavItem active label="Student IDs" icon={
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M15 9h3.75M15 12h3.75M15 15h3.75M4.5 19.5h15a2.25 2.25 0 002.25-2.25V6.75A2.25 2.25 0 0019.5 4.5h-15a2.25 2.25 0 00-2.25 2.25v10.5A2.25 2.25 0 004.5 19.5zm6-10.125a1.875 1.875 0 11-3.75 0 1.875 1.875 0 013.75 0zm1.294 6.336a6.721 6.721 0 01-3.17.789 6.721 6.721 0 01-3.168-.789 3.376 3.376 0 016.338 0z" />
            </svg>
          } />
          <NavItem label="Groups" icon={
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M18 18.72a9.094 9.094 0 003.741-.479 3 3 0 00-4.682-2.72m.94 3.198l.001.031c0 .225-.012.447-.037.666A11.944 11.944 0 0112 21c-2.17 0-4.207-.576-5.963-1.584A6.062 6.062 0 016 18.719m12 0a5.971 5.971 0 00-.941-3.197m0 0A5.995 5.995 0 0012 12.75a5.995 5.995 0 00-5.058 2.772m0 0a3 3 0 00-4.681 2.72 8.986 8.986 0 003.74.477m.94-3.197a5.971 5.971 0 00-.94 3.197M15 6.75a3 3 0 11-6 0 3 3 0 016 0zm6 3a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0zm-13.5 0a2.25 2.25 0 11-4.5 0 2.25 2.25 0 014.5 0z" />
            </svg>
          } />
          <NavItem label="Committees" icon={
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 21h16.5M4.5 3h15M5.25 3v18m13.5-18v18M9 6.75h1.5m-1.5 3h1.5m-1.5 3h1.5m3-6H15m-1.5 3H15m-1.5 3H15M9 21v-3.375c0-.621.504-1.125 1.125-1.125h3.75c.621 0 1.125.504 1.125 1.125V21" />
            </svg>
          } />
          <NavItem label="Schedule" icon={
            <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25m-18 0A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75m-18 0v-7.5A2.25 2.25 0 015.25 9h13.5A2.25 2.25 0 0121 9v7.5" />
            </svg>
          } />
          <div className="pt-4">
            <p className="text-xs font-medium text-gray-600 px-3 mb-2 uppercase tracking-widest">Grading</p>
            <NavItem label="Rubrics" icon={
              <svg fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M9 12h3.75M9 15h3.75M9 18h3.75m3 .75H18a2.25 2.25 0 002.25-2.25V6.108c0-1.135-.845-2.098-1.976-2.192a48.424 48.424 0 00-1.123-.08m-5.801 0c-.065.21-.1.433-.1.664 0 .414.336.75.75.75h4.5a.75.75 0 00.75-.75 2.25 2.25 0 00-.1-.664m-5.8 0A2.251 2.251 0 0113.5 2.25H15c1.012 0 1.867.668 2.15 1.586m-5.8 0c-.376.023-.75.05-1.124.08C9.095 4.01 8.25 4.973 8.25 6.108V8.25m0 0H4.875c-.621 0-1.125.504-1.125 1.125v11.25c0 .621.504 1.125 1.125 1.125h9.75c.621 0 1.125-.504 1.125-1.125V9.375c0-.621-.504-1.125-1.125-1.125H8.25zM6.75 12h.008v.008H6.75V12zm0 3h.008v.008H6.75V15zm0 3h.008v.008H6.75V18z" />
              </svg>
            } />
          </div>
        </nav>

        <div className="px-3 py-4 border-t border-white/5">
          <div className="flex items-center gap-3 px-3 py-2">
            <div className="w-8 h-8 rounded-full bg-blue-600/20 border border-blue-500/30 flex items-center justify-center shrink-0">
              <span className="text-xs font-semibold text-blue-400">CO</span>
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-white truncate">Coordinator</p>
              <p className="text-xs text-gray-500 truncate">Admin role</p>
            </div>
          </div>
        </div>
      </aside>

      {/* Main */}
      <main className="flex-1 flex flex-col min-w-0">
        <div className="border-b border-white/5 px-8 py-4 flex items-center justify-between">
          <div>
            <h1 className="text-base font-semibold text-white">Student ID Upload</h1>
            <p className="text-xs text-gray-500 mt-0.5">Upload valid student IDs to allow registration</p>
          </div>
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-white/5 border border-white/10">
            <div className="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse" />
            <span className="text-xs text-gray-400">System Online</span>
          </div>
        </div>

        <div className="flex-1 p-8">
          <div className="max-w-2xl mx-auto space-y-5">
            <UploadCard />
          </div>
        </div>
      </main>
    </div>
  );
}

function UploadCard() {
  const [status, setStatus] = useState<UploadStatus>("idle");
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<UploadResponse | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleDragOver = useCallback((e: React.DragEvent) => { e.preventDefault(); setStatus("dragging"); }, []);
  const handleDragLeave = useCallback(() => { setStatus("idle"); }, []);
  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault(); setStatus("idle");
    const dropped = e.dataTransfer.files[0];
    if (dropped) validateAndSetFile(dropped);
  }, []);

  const validateAndSetFile = (f: File) => {
    if (!f.name.endsWith(".csv")) { toast.error("Only .csv files are accepted."); return; }
    setFile(f); setResult(null); setStatus("idle");
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = e.target.files?.[0];
    if (selected) validateAndSetFile(selected);
  };

  const handleUpload = async () => {
    if (!file) return;
    setStatus("uploading"); setResult(null);
    const formData = new FormData();
    formData.append("file", file);
    try {
      const token = typeof window !== "undefined" ? localStorage.getItem("spms_token") : null;
      const headers: Record<string, string> = {};
      if (token) headers["Authorization"] = `Bearer ${token}`;
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/students/ids/upload`, { method: "POST", headers, body: formData });
      if (!res.ok) { const err = await res.json().catch(() => ({})); throw new Error(err.message || `Upload failed (${res.status})`); }
      const data: UploadResponse = await res.json();
      setResult(data); setStatus("success");
      toast.success(`Upload complete — ${data.validRecords} IDs added.`);
    } catch (err: unknown) {
      setStatus("error");
      toast.error(err instanceof Error ? err.message : "An unexpected error occurred.");
    }
  };

  const handleReset = () => {
    setFile(null); setResult(null); setStatus("idle");
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  return (
    <>
      <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
        <div className="px-6 py-4 border-b border-white/5 flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-blue-500/10 border border-blue-500/20 flex items-center justify-center">
            <svg className="w-4 h-4 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5" />
            </svg>
          </div>
          <div>
            <p className="text-sm font-semibold text-white">Upload CSV File</p>
            <p className="text-xs text-gray-500">Only .csv format is accepted</p>
          </div>
        </div>

        <div className="p-6">
          <div
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onDrop={handleDrop}
            onClick={() => fileInputRef.current?.click()}
            className={[
              "rounded-xl border-2 border-dashed cursor-pointer transition-all duration-200",
              "flex flex-col items-center justify-center gap-4 py-14 px-8 text-center",
              status === "dragging" ? "border-blue-500 bg-blue-500/5"
                : file ? "border-blue-500/50 bg-blue-500/5"
                : "border-white/10 hover:border-blue-500/40 hover:bg-blue-500/3",
            ].join(" ")}
          >
            <input ref={fileInputRef} type="file" accept=".csv" className="hidden" onChange={handleFileChange} />
            <div className={`w-14 h-14 rounded-2xl flex items-center justify-center transition-all ${file ? "bg-blue-500/15 border border-blue-500/30" : "bg-white/5 border border-white/10"}`}>
              {file ? (
                <svg className="w-6 h-6 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              ) : (
                <svg className="w-6 h-6 text-gray-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m6.75 12l-3-3m0 0l-3 3m3-3v6m-1.5-15H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z" />
                </svg>
              )}
            </div>
            {file ? (
              <div className="space-y-1">
                <p className="text-sm font-semibold text-white">{file.name}</p>
                <p className="text-xs text-gray-500">{(file.size / 1024).toFixed(1)} KB · Click to change</p>
              </div>
            ) : (
              <div className="space-y-1">
                <p className="text-sm font-medium text-gray-300">{status === "dragging" ? "Release to upload" : "Drag & drop your CSV file"}</p>
                <p className="text-xs text-gray-600">or click anywhere to browse</p>
              </div>
            )}
          </div>
        </div>

        <div className="mx-6 mb-6 rounded-xl bg-amber-500/5 border border-amber-500/15 px-4 py-3 flex gap-3">
          <svg className="w-4 h-4 text-amber-400 mt-0.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
          </svg>
          <div className="space-y-1">
            <p className="text-xs font-semibold text-amber-400">Expected CSV format</p>
            <p className="text-xs text-amber-500/80 font-mono leading-relaxed">studentId<br />11070001000<br />11070001001</p>
          </div>
        </div>

        <div className="px-6 pb-6 flex items-center gap-3">
          <button
            onClick={handleUpload}
            disabled={!file || status === "uploading"}
            className={[
              "flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-medium transition-all duration-150",
              !file || status === "uploading"
                ? "bg-white/5 text-gray-600 cursor-not-allowed border border-white/5"
                : "bg-blue-600 text-white hover:bg-blue-500 active:scale-95 shadow-lg shadow-blue-600/20",
            ].join(" ")}
          >
            {status === "uploading" ? (
              <><svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24"><circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" /><path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" /></svg>Uploading...</>
            ) : (
              <><svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5" /></svg>Upload CSV</>
            )}
          </button>
          {file && status !== "uploading" && (
            <button onClick={handleReset} className="px-4 py-2.5 rounded-xl text-sm font-medium text-gray-500 hover:text-gray-300 hover:bg-white/5 transition-colors border border-white/5">
              Clear
            </button>
          )}
        </div>
      </div>

      {result && status === "success" && (
        <div className="bg-gray-900 border border-white/8 rounded-2xl overflow-hidden">
          <div className="px-6 py-4 border-b border-white/5 flex items-center gap-3">
            <div className="w-2 h-2 rounded-full bg-green-400" />
            <p className="text-sm font-semibold text-white">Upload Result</p>
          </div>
          <div className="grid grid-cols-3 divide-x divide-white/5">
            <StatBox label="Total Records" value={result.totalRecords} color="gray" />
            <StatBox label="Successfully Added" value={result.validRecords} color="green" />
            <StatBox label="Invalid / Skipped" value={result.invalidRecords} color="red" />
          </div>
          {result.invalidRecords > 0 && (
            <div className="px-6 py-3 bg-red-500/5 border-t border-red-500/10">
              <p className="text-xs text-red-400">{result.invalidRecords} record(s) skipped due to invalid format or duplicates.</p>
            </div>
          )}
        </div>
      )}
    </>
  );
}

function StatBox({ label, value, color }: { label: string; value: number; color: "gray" | "green" | "red" }) {
  const colors = { gray: "text-white", green: "text-green-400", red: "text-red-400" };
  return (
    <div className="px-6 py-6 text-center space-y-1">
      <p className={`text-3xl font-bold ${colors[color]}`}>{value}</p>
      <p className="text-xs text-gray-600">{label}</p>
    </div>
  );
}