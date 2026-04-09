"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";

export default function ChangePasswordPage() {
  const router = useRouter();
  const [formData, setFormData] = useState({
    tempPassword: "",
    newPassword: "",
    confirmNewPassword: "",
  });
  
  const [message, setMessage] = useState({ type: "", text: "" });
  const [isLoading, setIsLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage({ type: "", text: "" });

    // 1. Validation: New passwords must match
    if (formData.newPassword !== formData.confirmNewPassword) {
      setMessage({ type: "error", text: "New passwords do not match!" });
      return;
    }

    // 2. Validation: Password strength (Min 8 chars, 1 Upper, 1 Lower, 1 Number)
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d\w\W]{8,}$/;
    if (!passwordRegex.test(formData.newPassword)) {
      setMessage({ 
        type: "error", 
        text: "Password must be at least 8 characters long, including an uppercase letter, a lowercase letter, and a number." 
      });
      return;
    }

    setIsLoading(true);

    try {
      // 3. API Integration: POST /auth/change-password
      const response = await fetch("/api/v1/auth/change-password", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          tempPassword: formData.tempPassword,
          newPassword: formData.newPassword,
        }),
      });

      const data = await response.json();

      if (response.ok) {
        setMessage({ type: "success", text: "Password updated successfully! Redirecting to dashboard..." });
        
        // 4. Redirect to dashboard on success after 2 seconds
        setTimeout(() => {
          router.push("/dashboard");
        }, 2000);
      } else {
        setMessage({ type: "error", text: data.message || "Something went wrong. Please check your temporary password." });
      }
    } catch (err) {
      setMessage({ type: "error", text: "Server connection failed. Please try again later." });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50 text-black p-4">
      <div className="bg-white border p-8 rounded-xl shadow-md w-full max-w-md">
        <div className="mb-6 border-b pb-4 text-center">
          <h1 className="text-2xl font-bold text-gray-800">Change Password</h1>
          <p className="text-sm text-gray-500 mt-2">
            This is your first login. Please update your temporary password to continue.
          </p>
        </div>

        {message.text && (
          <div className={`p-4 mb-6 rounded-md text-sm font-medium ${
            message.type === "error" 
              ? "bg-red-50 text-red-700 border border-red-200" 
              : "bg-green-50 text-green-700 border border-green-200"
          }`}>
            {message.text}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Temporary Password</label>
            <input
              type="password"
              name="tempPassword"
              required
              className="w-full p-3 border rounded-md border-gray-300 focus:ring-2 focus:ring-blue-500 outline-none"
              placeholder="Current temporary password"
              value={formData.tempPassword}
              onChange={handleChange}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">New Password</label>
            <input
              type="password"
              name="newPassword"
              required
              className="w-full p-3 border rounded-md border-gray-300 focus:ring-2 focus:ring-blue-500 outline-none"
              placeholder="••••••••"
              value={formData.newPassword}
              onChange={handleChange}
            />
            <p className="text-xs text-gray-500 mt-1">At least 8 characters, 1 upper, 1 lower, 1 number.</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Confirm New Password</label>
            <input
              type="password"
              name="confirmNewPassword"
              required
              className="w-full p-3 border rounded-md border-gray-300 focus:ring-2 focus:ring-blue-500 outline-none"
              placeholder="••••••••"
              value={formData.confirmNewPassword}
              onChange={handleChange}
            />
          </div>

          <button 
            type="submit" 
            disabled={isLoading}
            className={`w-full text-white p-3 rounded-md font-medium transition-colors mt-4 ${
              isLoading ? "bg-blue-400 cursor-not-allowed" : "bg-blue-600 hover:bg-blue-700"
            }`}
          >
            {isLoading ? "Updating..." : "Update Password"}
          </button>
        </form>
      </div>
    </div>
  );
}