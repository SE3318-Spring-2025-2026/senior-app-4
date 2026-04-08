"use client";

import { signIn } from "next-auth/react";
import { useState } from "react";

export default function LoginPage() {
  const [loading, setLoading] = useState(false);

  const handleGitHubLogin = async () => {
  setLoading(true);
  try {
    await signIn("github", { callbackUrl: "/coordinator/student-ids" });
  } catch (error) {
    console.error(error);
  } finally {
    setLoading(false);
  }
};

  return (
    <div className="min-h-screen bg-gray-950 flex items-center justify-center px-4">
      <div className="w-full max-w-sm space-y-8">

        {/* Logo */}
        <div className="text-center space-y-3">
          <div className="w-14 h-14 rounded-2xl bg-blue-600 flex items-center justify-center mx-auto shadow-lg shadow-blue-600/30">
            <svg className="w-7 h-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
          </div>
          <div>
            <h1 className="text-xl font-semibold text-white">SPMS</h1>
            <p className="text-sm text-gray-500">Senior Project Management System</p>
          </div>
        </div>

        {/* Card */}
        <div className="bg-gray-900 border border-white/8 rounded-2xl p-8 space-y-6">
          <div className="space-y-1 text-center">
            <h2 className="text-base font-semibold text-white">Sign in to your account</h2>
            <p className="text-xs text-gray-500">
              Use your GitHub account to continue. Only registered students can access the system.
            </p>
          </div>

          {/* GitHub Button */}
          <button
            onClick={handleGitHubLogin}
            disabled={loading}
            className={[
              "w-full flex items-center justify-center gap-3 px-4 py-3 rounded-xl",
              "text-sm font-medium transition-all duration-150",
              loading
                ? "bg-white/5 text-gray-600 cursor-not-allowed border border-white/5"
                : "bg-white text-gray-900 hover:bg-gray-100 active:scale-95 shadow-lg shadow-black/20",
            ].join(" ")}
          >
            {loading ? (
              <>
                <svg className="w-4 h-4 animate-spin text-gray-500" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                </svg>
                <span className="text-gray-500">Redirecting...</span>
              </>
            ) : (
              <>
                {/* GitHub Icon */}
                <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 0C5.374 0 0 5.373 0 12c0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23A11.509 11.509 0 0112 5.803c1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576C20.566 21.797 24 17.3 24 12c0-6.627-5.373-12-12-12z" />
                </svg>
                Continue with GitHub
              </>
            )}
          </button>

          {/* Divider */}
          <div className="flex items-center gap-3">
            <div className="flex-1 h-px bg-white/5" />
            <span className="text-xs text-gray-600">or</span>
            <div className="flex-1 h-px bg-white/5" />
          </div>

          {/* Professor login note */}
          <div className="rounded-xl bg-blue-500/5 border border-blue-500/15 px-4 py-3 flex gap-3">
            <svg className="w-4 h-4 text-blue-400 mt-0.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11.25 11.25l.041-.02a.75.75 0 011.063.852l-.708 2.836a.75.75 0 001.063.853l.041-.021M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9-3.75h.008v.008H12V8.25z" />
            </svg>
            <p className="text-xs text-blue-400/80 leading-relaxed">
              Professors and Coordinators are registered manually by the Admin and receive login credentials via email.
            </p>
          </div>
        </div>

        {/* Footer */}
        <p className="text-center text-xs text-gray-700">
          Yaşar University · Senior Project Management System 
        </p>

      </div>
    </div>
  );
}