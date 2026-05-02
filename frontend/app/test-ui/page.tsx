"use client";

import React, { useState } from "react";
import StatusBadge from "@/components/StatusBadge";

export default function DesignSystemTestPage() {
  const [isModalOpen, setIsModalOpen] = useState(false);

  return (
    <div className="p-8 space-y-12 bg-white min-h-screen text-slate-900">
      <div>
        <h1 className="text-3xl font-bold mb-2">Design System UI Test (Issue 320)</h1>
        <p className="text-gray-500">This page demonstrates the standardized components, typography, and colors.</p>
      </div>

      <section className="space-y-4">
        <h2 className="text-2xl font-semibold border-b pb-2">1. Typography Hierarchy</h2>
        <div className="space-y-2">
          <h1 className="text-[36px] font-bold">H1 Header (36px)</h1>
          <h2 className="text-[28px] font-semibold">H2 Header (28px)</h2>
          <h3 className="text-[24px] font-medium">H3 Header (24px)</h3>
          <p className="text-[16px]">Body Text (16px) - The quick brown fox jumps over the lazy dog.</p>
          <p className="text-[14px] text-gray-500">Small Text (14px) - Useful for captions and hints.</p>
        </div>
      </section>

      <section className="space-y-4">
        <h2 className="text-2xl font-semibold border-b pb-2">2. Color Palette & Status Badges</h2>
        <div className="flex gap-4 items-center">
          <div className="w-16 h-16 rounded-md bg-[#007BFF] shadow-sm flex items-center justify-center text-white text-xs">Primary</div>
          <div className="w-16 h-16 rounded-md bg-[#6C757D] shadow-sm flex items-center justify-center text-white text-xs">Secondary</div>
          <div className="w-16 h-16 rounded-md bg-[#28A745] shadow-sm flex items-center justify-center text-white text-xs">Success</div>
          <div className="w-16 h-16 rounded-md bg-[#FFC107] shadow-sm flex items-center justify-center text-black text-xs">Warning</div>
        </div>
        <div className="flex gap-4 pt-4">
          <StatusBadge status="ACTIVE" />
          <StatusBadge status="INACTIVE" />
          <StatusBadge status="COMPLETED" />
          <StatusBadge status="forming" />
        </div>
      </section>

      <section className="space-y-4">
        <h2 className="text-2xl font-semibold border-b pb-2">3. Buttons & Inputs</h2>
        <div className="flex gap-4 items-center">
          <button className="btn btn-primary">Primary Button</button>
          <button className="btn btn-secondary">Secondary Button</button>
          <button className="btn btn-ghost">Ghost Button</button>
        </div>
        <div className="max-w-sm pt-4">
          <input type="text" className="input" placeholder="Focus me to see WCAG 2.1 AA ring" />
        </div>
      </section>

      <section className="space-y-4">
        <h2 className="text-2xl font-semibold border-b pb-2">4. Standardized Table</h2>
        <div className="card overflow-hidden">
          <table className="table">
            <thead>
              <tr>
                <th>Student ID</th>
                <th>Name</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>11070001000</td>
                <td>Jane Doe</td>
                <td><StatusBadge status="ACTIVE" /></td>
                <td><button className="text-blue-500 hover:underline text-sm">Edit</button></td>
              </tr>
              <tr>
                <td>11070001001</td>
                <td>John Smith</td>
                <td><StatusBadge status="COMPLETED" /></td>
                <td><button className="text-blue-500 hover:underline text-sm">Edit</button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section className="space-y-4">
        <h2 className="text-2xl font-semibold border-b pb-2">5. Cards & Modals</h2>
        <div className="card max-w-sm">
          <div className="card-header font-medium">Standard Card Header</div>
          <div className="card-body text-sm text-gray-600">
            This is a standardized card body. It uses the design system radius and shadow.
          </div>
          <div className="p-4 border-t border-gray-100 flex justify-end">
            <button className="btn btn-primary" onClick={() => setIsModalOpen(true)}>Open Modal</button>
          </div>
        </div>

        {isModalOpen && (
          <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
            <div className="modal-content" onClick={e => e.stopPropagation()}>
              <div className="card-header font-semibold text-lg border-b pb-3 mb-3">
                Modal Title
              </div>
              <div className="card-body">
                <p className="text-sm text-gray-600 mb-4">
                  This modal conforms to the standardized design system. It includes a subtle backdrop blur and proper shadowing.
                </p>
                <div className="flex justify-end gap-2">
                  <button className="btn btn-secondary" onClick={() => setIsModalOpen(false)}>Cancel</button>
                  <button className="btn btn-primary" onClick={() => setIsModalOpen(false)}>Confirm</button>
                </div>
              </div>
            </div>
          </div>
        )}
      </section>
    </div>
  );
}
