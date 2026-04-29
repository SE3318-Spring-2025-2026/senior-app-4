"use client";

import { useState, useRef, useCallback, useEffect } from "react";
import { toast } from "sonner";
import { useAuthGuard } from "@/hooks/useAuthGuard";
import Sidebar from "@/components/Sidebar";

interface UploadResponse {
  message: string;
  totalRecords: number;
  validRecords: number;
  invalidRecords: number;
}

type UploadStatus = "idle" | "dragging" | "uploading" | "success" | "error";

export default function StudentIdUploadPage() {
  const authStatus = useAuthGuard("coordinator");

  if (authStatus === "loading") return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center">
      <svg className="w-6 h-6 animate-spin text-blue-500" fill="none" viewBox="0 0 24 24">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>
  );
  if (authStatus === "denied") return <AccessDenied />;
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



function DashboardLayout() {
  return (
    <div className="min-h-screen bg-gray-950 flex">
      {/* Sidebar */}
      <Sidebar activePage="student-ids" />

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
            <StudentList />
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
  )
}
function StudentList() {
  const [searchTerm, setSearchTerm] = useState("");
  const [users, setUsers] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchUsers = async () => {
      try {
        const token = typeof window !== "undefined" ? localStorage.getItem("spms_token") : null;
        const headers: Record<string, string> = {};
        if (token) headers["Authorization"] = `Bearer ${token}`;

        const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/student-ids`, { 
        method: "GET",
        headers,
        });

        if (!res.ok) {
          console.warn(`Failed to fetch users: ${res.status}`);
          return;
        }

        const data = await res.json();
        
        if (Array.isArray(data)) setUsers(data);
        else if (data && Array.isArray(data.content)) setUsers(data.content);
        else if (data && Array.isArray(data.data)) setUsers(data.data);
      } catch (error) {
        console.error("Error fetching users:", error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchUsers();
  }, []);

  // 🔍 ARAMA FİLTRESİ
  const filteredUsers = users.filter((u) => {
    if (searchTerm.trim() === "") return false;

    const query = searchTerm.toLowerCase();
    
    const id = u.studentId ? String(u.studentId).toLowerCase() : "";
    const name = u.fullName ? String(u.fullName).toLowerCase() : "";

    return id.includes(query) || name.includes(query);
  });

  return (
    <div className="h-full">
      <h2 className="text-lg font-semibold text-white mb-4">Student List</h2>

      {/* studentId search barı */}
      <div className="mb-4 relative">
        <input
          type="text"
          placeholder="Search by Student ID or Full Name..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full px-4 py-3 pl-11 rounded-xl text-sm outline-none transition-all focus:ring-2 bg-gray-900 border border-white/10 text-white focus:border-blue-500 focus:ring-blue-500/20 placeholder-gray-500"
        />
        <svg
          className="absolute left-3.5 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-500"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
        </svg>
      </div>


      {searchTerm.trim() !== "" && (
        <div className="rounded-xl border border-white/10 bg-gray-900 overflow-hidden shadow-xl">
          {filteredUsers.length > 0 ? (
            <ul className="divide-y divide-white/5 max-h-[300px] overflow-y-auto custom-scrollbar">
              {filteredUsers.map((u, index) => (
                <li key={index} className="px-4 py-3 flex justify-between items-center hover:bg-white/5 transition-colors cursor-default">
                  

                  <div className="flex flex-col">
                    <span className="font-medium text-gray-200">
                      {u.fullName || "Unknown Name"}
                    </span>
                    <span className="font-mono text-xs text-gray-500 mt-0.5">
                      {u.studentId || "No ID"}
                    </span>
                  </div>
                  
                </li>
              ))}
            </ul>
          ) : (
            <div className="px-4 py-8 text-center text-gray-500 text-sm">
              No users found matching <span className="text-white font-medium">"{searchTerm}"</span>
            </div>
          )}
        </div>
      )}
    </div>
  );
}