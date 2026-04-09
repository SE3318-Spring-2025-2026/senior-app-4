"use client";

import { useState } from "react";

export default function AdminProfessorRegisterPage() {
  const [formData, setFormData] = useState({
    fullName: "",
    email: "",
    password: "",
    role: "PROFESSOR", // Default value for dropdown
  });
  
  const [message, setMessage] = useState({ type: "", text: "" });
  const [isLoading, setIsLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage({ type: "", text: "" });

    // 1. Client-side validation: Valid Email Format
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(formData.email)) {
      setMessage({ type: "error", text: "Please enter a valid email address." });
      return;
    }

    // 2. Client-side validation: Strong Password Check (Min 8 chars, 1 Uppercase, 1 Lowercase, 1 Number)
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d\w\W]{8,}$/;
    if (!passwordRegex.test(formData.password)) {
      setMessage({ 
        type: "error", 
        text: "Password must be at least 8 characters long, and include at least one uppercase letter, one lowercase letter, and one number." 
      });
      return;
    }

    setIsLoading(true);

    try {
      // 3. Backend Endpoint Integration
      const response = await fetch("/api/v1/professors/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(formData),
      });

      const data = await response.json();

      if (response.ok) {
        // 4. Show success message and clear form
        setMessage({ type: "success", text: "Professor/Coordinator successfully registered!" });
        setFormData({ fullName: "", email: "", password: "", role: "PROFESSOR" });
      } else {
        // 5. Show backend errors (e.g., Email already exists)
        setMessage({ type: "error", text: data.message || "An error occurred during registration. The email might already be registered." });
      }
    } catch (err) {
      setMessage({ type: "error", text: "Could not connect to the server. Please check the API connection." });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50 text-black p-4">
      <div className="bg-white border p-8 rounded-xl shadow-md w-full max-w-md">
        <div className="mb-6 border-b pb-4 text-center">
          <h1 className="text-2xl font-bold text-gray-800">Register Personnel</h1>
          {/* Security Note: Role check (Coordinator) should ideally be handled at Next.js Middleware or Layout level */}
          <span className="inline-block mt-2 px-3 py-1 bg-blue-100 text-blue-800 text-xs font-semibold rounded-full">
            Coordinator Access Only
          </span>
        </div>

        {message.text && (
          <div className={`p-4 mb-6 rounded-md text-sm font-medium ${message.type === "error" ? "bg-red-50 text-red-700 border border-red-200" : "bg-green-50 text-green-700 border border-green-200"}`}>
            {message.text}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Full Name</label>
            <input
              type="text"
              name="fullName"
              required
              className="w-full p-3 border rounded-md border-gray-300 focus:ring-2 focus:ring-blue-500 outline-none"
              placeholder="Prof. Dr. John Doe"
              value={formData.fullName}
              onChange={handleChange}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Email Address</label>
            <input
              type="email"
              name="email"
              required
              className="w-full p-3 border rounded-md border-gray-300 focus:ring-2 focus:ring-blue-500 outline-none"
              placeholder="example@university.edu"
              value={formData.email}
              onChange={handleChange}
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Temporary Password</label>
            <input
              type="password"
              name="password"
              required
              className="w-full p-3 border rounded-md border-gray-300 focus:ring-2 focus:ring-blue-500 outline-none"
              placeholder="••••••••"
              value={formData.password}
              onChange={handleChange}
            />
            <p className="text-xs text-gray-500 mt-1">Min 8 chars, 1 uppercase, 1 lowercase, 1 number.</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Role</label>
            <select
              name="role"
              className="w-full p-3 border rounded-md border-gray-300 focus:ring-2 focus:ring-blue-500 outline-none bg-white"
              value={formData.role}
              onChange={handleChange}
            >
              <option value="PROFESSOR">Professor</option>
              <option value="COORDINATOR">Coordinator</option>
            </select>
          </div>

          <button 
            type="submit" 
            disabled={isLoading}
            className={`w-full text-white p-3 rounded-md font-medium transition-colors mt-2 ${isLoading ? "bg-blue-400 cursor-not-allowed" : "bg-blue-600 hover:bg-blue-700"}`}
          >
            {isLoading ? "Processing..." : "Register"}
          </button>
        </form>
      </div>
    </div>
  );
}