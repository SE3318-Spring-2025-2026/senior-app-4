"use client";

import React, { useState } from 'react';
import apiClient from '../../lib/client';
import { Tooltip } from '../../components/Tooltip';
import { showToast } from '../../components/toast/ToastContext';
import axios from 'axios';

export default function TestUIPage() {
  const [loading, setLoading] = useState(false);

  // Triggering actual API interceptors
  const trigger404 = async () => {
    try {
      await apiClient.get('/this-endpoint-does-not-exist');
    } catch (e) {
      // Caught by interceptor
    }
  };

  const triggerNetworkError = async () => {
    try {
      await axios.get('http://localhost:12345/down');
    } catch (e: any) {
      showToast("Unable to connect. Check your connection.", 'error');
    }
  };

  const trigger500 = async () => {
    showToast("Server error. Please try again later.", 'error');
  };

  const triggerSuccess = () => {
    showToast("Operation completed successfully!", 'success');
  };

  const triggerWarning = () => {
    showToast("This is a warning message.", 'warning');
  };

  return (
    <div className="min-h-screen bg-slate-50/50">
      <div className="max-w-6xl mx-auto px-6 py-16 space-y-16">
        
        {/* Header Section */}
        <div className="text-center space-y-4">
          <div className="inline-flex items-center justify-center p-2 bg-blue-50 rounded-2xl mb-4">
            <span className="bg-blue-600 text-white px-3 py-1 rounded-xl text-sm font-semibold tracking-wide">TEST ENVIRONMENT</span>
          </div>
          <h1 className="text-5xl font-extrabold tracking-tight text-slate-900">
            Design System & <span className="text-transparent bg-clip-text bg-gradient-to-r from-blue-600 to-indigo-500">Error Handling</span>
          </h1>
          <p className="text-lg text-slate-500 max-w-2xl mx-auto leading-relaxed">
            A premium showcase of our new standardized typography, components, and global interceptor system.
          </p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-10">
          
          {/* ISSUE 320: DESIGN SYSTEM */}
          <section className="space-y-8">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center text-blue-600 font-bold">1</div>
              <h2 className="text-2xl font-bold text-slate-800">Design System (Issue 320)</h2>
            </div>
            
            <div className="bg-white rounded-3xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-slate-100 space-y-10 relative overflow-hidden">
              <div className="absolute top-0 right-0 w-64 h-64 bg-blue-50 rounded-full blur-3xl -mr-32 -mt-32 opacity-60"></div>
              
              <div className="space-y-6 relative z-10">
                <div>
                  <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-6">Typography</h3>
                  <div className="space-y-5">
                    <div><h1 className="h1-title text-slate-900">H1 Title (36px)</h1></div>
                    <div><h2 className="h2-title text-slate-800">H2 Title (28px)</h2></div>
                    <div><h3 className="h3-title text-slate-700">H3 Title (24px)</h3></div>
                    <div><p className="body-text text-slate-600">Body Text (16px) - The quick brown fox jumps over the lazy dog, creating a seamless reading experience.</p></div>
                    <div><small className="text-small text-slate-500">Small Text (14px) - For subtle hints and secondary information.</small></div>
                  </div>
                </div>

                <div className="h-px bg-slate-100 w-full"></div>

                <div>
                  <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-6">Buttons & Focus Rings</h3>
                  <div className="flex flex-wrap gap-4">
                    <button className="btn btn-primary">Primary Action</button>
                    <button className="btn btn-secondary">Secondary</button>
                    <button className="btn btn-ghost">Ghost</button>
                  </div>
                  <p className="text-xs text-slate-400 mt-4">Press <kbd className="px-2 py-1 bg-slate-100 rounded text-slate-600 font-mono text-[10px]">Tab</kbd> to see WCAG 2.1 AA compliant focus rings.</p>
                </div>

                <div className="h-px bg-slate-100 w-full"></div>

                <div>
                  <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-6">Status Badges</h3>
                  <div className="flex flex-wrap gap-4">
                    <span className="badge badge-active">ACTIVE</span>
                    <span className="badge badge-inactive">INACTIVE</span>
                    <span className="badge badge-completed shadow-sm">COMPLETED</span>
                  </div>
                </div>
              </div>
            </div>
          </section>

          {/* ISSUE 321: GLOBAL ERROR HANDLING & TOAST */}
          <section className="space-y-8">
            <div className="flex items-center gap-3">
              <div className="w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-600 font-bold">2</div>
              <h2 className="text-2xl font-bold text-slate-800">Global Error & Toast (Issue 321)</h2>
            </div>
            
            <div className="bg-white rounded-3xl p-8 shadow-[0_8px_30px_rgb(0,0,0,0.04)] border border-slate-100 space-y-10 relative overflow-hidden h-full">
              <div className="absolute bottom-0 left-0 w-64 h-64 bg-indigo-50 rounded-full blur-3xl -ml-32 -mb-32 opacity-60"></div>
              
              <div className="space-y-6 relative z-10">
                <div>
                  <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-6">Interactive Tooltip</h3>
                  <div className="flex items-center gap-3 p-4 bg-slate-50 rounded-2xl border border-slate-100 w-fit">
                    <label className="body-text font-medium text-slate-700">Username field</label>
                    <Tooltip content="Kullanıcı adı en az 8 karakter olmalı ve boşluk içermemelidir." />
                  </div>
                  <p className="text-xs text-slate-400 mt-4">Hover over the info icon to see the fluid tooltip animation.</p>
                </div>

                <div className="h-px bg-slate-100 w-full"></div>

                <div>
                  <h3 className="text-sm font-semibold text-slate-400 uppercase tracking-wider mb-6">Toast Interceptor triggers</h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <button className="btn btn-secondary w-full justify-center" onClick={trigger404}>Test 404 (API Call)</button>
                    <button className="btn btn-secondary w-full justify-center" onClick={triggerWarning}>Test Warning</button>
                    <button className="btn w-full justify-center !bg-red-50 !text-red-600 !border !border-red-200 hover:!bg-red-100" onClick={triggerNetworkError}>Network Error (Retry)</button>
                    <button className="btn w-full justify-center !bg-red-50 !text-red-600 !border !border-red-200 hover:!bg-red-100" onClick={trigger500}>500 Error (Retry)</button>
                    <button className="btn w-full justify-center !bg-emerald-50 !text-emerald-600 !border !border-emerald-200 hover:!bg-emerald-100 sm:col-span-2" onClick={triggerSuccess}>Success Toast</button>
                  </div>
                </div>
              </div>
            </div>
          </section>

        </div>
      </div>
    </div>
  );
}
