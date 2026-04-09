"use client";

import { useState } from "react";
import { signIn } from "next-auth/react";

export default function StudentLoginPage() {
  const [studentId, setStudentId] = useState("");
  const [error, setError] = useState("");
  const [isValidated, setIsValidated] = useState(false);

  const handleValidate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    // 11-digit numeric check
    if (!/^\d{11}$/.test(studentId)) {
      setError("Please enter a valid 11-digit student ID.");
      return;
    }

    try {
      // API Integration (YAML: POST /students/validate)
      const response = await fetch("/api/v1/students/validate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ studentId }),
      });

      const data = await response.json();

      if (response.ok && data.valid) {
        setIsValidated(true); // Show GitHub button if successful
      } else {
        setError(data.message || "Student ID not found in the system.");
      }
    } catch (err) {
      setError("Connection error occurred. Please try again.");
    }
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-white text-black">
      <div className="border p-10 rounded-lg shadow-sm w-full max-w-sm">
        <h1 className="text-xl font-bold mb-6 text-center">Student Validation</h1>
        {!isValidated ? (
          <form onSubmit={handleValidate} className="space-y-4">
            <input
              type="text"
              placeholder="Student ID"
              className="w-full p-3 border rounded border-gray-300 outline-none focus:ring-2 focus:ring-blue-500"
              value={studentId}
              onChange={(e) => setStudentId(e.target.value)}
            />
            {error && <p className="text-red-500 text-xs font-medium">{error}</p>}
            <button type="submit" className="w-full bg-blue-600 hover:bg-blue-700 text-white p-3 rounded font-medium transition-colors">
              Validate
            </button>
          </form>
        ) : (
          <div className="space-y-4">
            <p className="text-green-600 text-sm text-center font-medium">✓ ID Validated</p>
            <button 
              onClick={() => signIn("github")}
              className="w-full bg-black hover:bg-gray-800 text-white p-3 rounded font-medium transition-colors flex items-center justify-center gap-2"
            >
              Sign in with GitHub
            </button>
          </div>
        )}
      </div>
    </div>
  );
}