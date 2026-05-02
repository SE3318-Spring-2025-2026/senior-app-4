"use client";

import React, { useState } from "react";
import StatusBadge from "@/components/StatusBadge";
import Sidebar from "@/components/Sidebar";

export default function DesignSystemTestPage() {
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <div className="min-h-screen bg-gray-950 flex">
      <Sidebar activePage="" />
      <main className="flex-1 min-w-0 px-6 py-10 overflow-y-auto">
        <div className="mx-auto max-w-6xl space-y-12 text-white">
          
          <div>
            <h1 className="text-3xl font-bold mb-2">Design System UI Test (Issue 320)</h1>
            <p className="text-gray-400">This page demonstrates the standardized components, typography, and colors in the correct dark-mode environment.</p>
          </div>

          <section className="space-y-4">
            <h2 className="text-2xl font-semibold border-b border-white/10 pb-2">1. Typography Hierarchy</h2>
            <div className="space-y-2 bg-gray-900 border border-white/10 rounded-2xl p-6 shadow-lg shadow-black/20">
              <h1 className="text-[36px] font-bold">H1 Header (36px)</h1>
              <h2 className="text-[28px] font-semibold">H2 Header (28px)</h2>
              <h3 className="text-[24px] font-medium">H3 Header (24px)</h3>
              <p className="text-[16px]">Body Text (16px) - The quick brown fox jumps over the lazy dog.</p>
              <p className="text-[14px] text-gray-500">Small Text (14px) - Useful for captions and hints.</p>
            </div>
          </section>

          <section className="space-y-4">
            <h2 className="text-2xl font-semibold border-b border-white/10 pb-2">2. Color Palette & Status Badges</h2>
            <div className="bg-gray-900 border border-white/10 rounded-2xl p-6 shadow-lg shadow-black/20 space-y-6">
              <div className="flex gap-4 items-center">
                <div className="w-20 h-20 rounded-xl bg-[#007BFF] shadow-sm flex items-center justify-center text-white text-sm font-medium">Primary</div>
                <div className="w-20 h-20 rounded-xl bg-[#6C757D] shadow-sm flex items-center justify-center text-white text-sm font-medium">Secondary</div>
                <div className="w-20 h-20 rounded-xl bg-[#28A745] shadow-sm flex items-center justify-center text-white text-sm font-medium">Success</div>
                <div className="w-20 h-20 rounded-xl bg-[#FFC107] shadow-sm flex items-center justify-center text-black text-sm font-medium">Warning</div>
              </div>
              <div className="flex gap-4">
                <StatusBadge status="ACTIVE" />
                <StatusBadge status="INACTIVE" />
                <StatusBadge status="COMPLETED" />
                <StatusBadge status="forming" />
              </div>
            </div>
          </section>

          <section className="space-y-4">
            <h2 className="text-2xl font-semibold border-b border-white/10 pb-2">3. Buttons & Inputs</h2>
            <div className="bg-gray-900 border border-white/10 rounded-2xl p-6 shadow-lg shadow-black/20 space-y-6">
              <div className="flex gap-4 items-center">
                <button className="rounded-xl bg-[#007BFF] px-6 py-3 text-sm font-semibold text-white hover:bg-blue-600 transition-colors shadow-lg shadow-blue-500/20 focus-visible:ring-4 focus-visible:ring-blue-500/30 outline-none">Primary Button</button>
                <button className="rounded-xl bg-[#6C757D] px-6 py-3 text-sm font-semibold text-white hover:bg-gray-600 transition-colors shadow-lg shadow-gray-500/20 focus-visible:ring-4 focus-visible:ring-gray-500/30 outline-none">Secondary Button</button>
                <button className="rounded-xl border border-white/10 bg-transparent px-6 py-3 text-sm font-semibold text-gray-300 hover:bg-white/5 transition-colors focus-visible:ring-4 focus-visible:ring-gray-500/30 outline-none">Ghost Button</button>
              </div>
              <div className="max-w-sm">
                <input type="text" className="w-full bg-gray-950 border border-white/10 text-sm text-white rounded-xl px-4 py-3 focus:border-[#007BFF] focus:ring-4 focus:ring-[#007BFF]/30 outline-none shadow-inner" placeholder="Focus me to see WCAG 2.1 AA ring" />
              </div>
            </div>
          </section>

          <section className="space-y-4">
            <h2 className="text-2xl font-semibold border-b border-white/10 pb-2">4. Standardized Table</h2>
            <div className="bg-gray-900 border border-white/10 rounded-2xl overflow-hidden shadow-lg shadow-black/20">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="border-b border-white/10 bg-white/5">
                    <th className="p-4 font-semibold text-gray-300">Student ID</th>
                    <th className="p-4 font-semibold text-gray-300">Name</th>
                    <th className="p-4 font-semibold text-gray-300">Status</th>
                    <th className="p-4 font-semibold text-gray-300">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  <tr className="hover:bg-white/5 transition-colors">
                    <td className="p-4 text-gray-300">11070001000</td>
                    <td className="p-4 text-white font-medium">Jane Doe</td>
                    <td className="p-4"><StatusBadge status="ACTIVE" /></td>
                    <td className="p-4"><button className="text-[#007BFF] hover:underline text-sm font-medium">Edit</button></td>
                  </tr>
                  <tr className="hover:bg-white/5 transition-colors">
                    <td className="p-4 text-gray-300">11070001001</td>
                    <td className="p-4 text-white font-medium">John Smith</td>
                    <td className="p-4"><StatusBadge status="COMPLETED" /></td>
                    <td className="p-4"><button className="text-[#007BFF] hover:underline text-sm font-medium">Edit</button></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <section className="space-y-4">
            <h2 className="text-2xl font-semibold border-b border-white/10 pb-2">5. Modals</h2>
            <div className="bg-gray-900 border border-white/10 rounded-2xl p-6 shadow-lg shadow-black/20 flex justify-between items-center max-w-md">
              <div>
                <div className="font-semibold text-white mb-1">Interactive Modal</div>
                <div className="text-sm text-gray-400">Click to open a glassmorphism modal.</div>
              </div>
              <button className="rounded-xl bg-[#007BFF] px-6 py-2.5 text-sm font-semibold text-white hover:bg-blue-600 transition-colors shadow-lg shadow-blue-500/20" onClick={() => setIsModalOpen(true)}>Open Modal</button>
            </div>

            {isModalOpen && (
              <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4 animate-in fade-in duration-200" onClick={() => setIsModalOpen(false)}>
                <div className="bg-gray-900 border border-white/10 rounded-2xl shadow-2xl w-full max-w-md overflow-hidden animate-in zoom-in-95 duration-200" onClick={e => e.stopPropagation()}>
                  <div className="p-6 border-b border-white/10">
                    <h3 className="text-xl font-semibold text-white">Modal Title</h3>
                  </div>
                  <div className="p-6">
                    <p className="text-sm text-gray-400 mb-6">
                      This modal conforms to the standardized design system. It includes a subtle backdrop blur, accurate border styling, and proper shadowing for the dark theme.
                    </p>
                    <div className="flex justify-end gap-3">
                      <button className="rounded-xl border border-white/10 bg-transparent px-5 py-2.5 text-sm font-semibold text-gray-300 hover:bg-white/5 transition-colors" onClick={() => setIsModalOpen(false)}>Cancel</button>
                      <button className="rounded-xl bg-[#007BFF] px-5 py-2.5 text-sm font-semibold text-white hover:bg-blue-600 transition-colors shadow-lg shadow-blue-500/20" onClick={() => setIsModalOpen(false)}>Confirm</button>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </section>

        </div>
      </main>
    </div>
  );
}
