"use client";

import React from 'react';
import { useToast } from './ToastContext';
import { CheckCircle, AlertCircle, AlertTriangle, Info } from 'lucide-react';

export const ToastContainer: React.FC = () => {
  const { toasts, removeToast } = useToast();

  const getConfig = (type: string) => {
    switch (type) {
      case 'success':
        return { bg: '#28A745', icon: <CheckCircle size={20} className="shrink-0" /> };
      case 'error':
        return { bg: '#ef4444', icon: <AlertCircle size={20} className="shrink-0" /> };
      case 'warning':
        return { bg: '#FFC107', icon: <AlertTriangle size={20} className="shrink-0" />, color: '#000' };
      case 'info':
        return { bg: '#007BFF', icon: <Info size={20} className="shrink-0" /> };
      default:
        return { bg: '#333', icon: <Info size={20} className="shrink-0" /> };
    }
  };

  if (toasts.length === 0) return null;

  return (
    <div
      style={{
        position: 'fixed',
        top: '20px',
        right: '20px',
        zIndex: 9999,
        display: 'flex',
        flexDirection: 'column',
        gap: '10px',
      }}
    >
      {toasts.map((toast) => {
        const config = getConfig(toast.type);
        const isError = toast.type === 'error';
        const isNetworkOrServerError = isError && (toast.message.includes("Unable to connect") || toast.message.includes("Server error"));
        
        return (
          <div
            key={toast.id}
            style={{
              backgroundColor: config.bg,
              color: config.color || '#fff',
              padding: '16px 20px',
              borderRadius: '8px',
              boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              minWidth: '320px',
              maxWidth: '450px',
              fontFamily: 'sans-serif',
              transition: 'all 0.3s ease',
              gap: '12px'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', flex: 1 }}>
              <div style={{ marginTop: '2px' }}>{config.icon}</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                <span style={{ fontSize: '14px', lineHeight: '1.4', fontWeight: 500 }}>
                  {toast.message}
                </span>
                
                {/* Retry Button for Network/500 Errors as per AC */}
                {isNetworkOrServerError && (
                  <button 
                    onClick={() => window.location.reload()}
                    style={{
                      background: 'rgba(255,255,255,0.2)',
                      border: 'none',
                      color: '#fff',
                      padding: '4px 12px',
                      borderRadius: '4px',
                      fontSize: '12px',
                      fontWeight: 'bold',
                      cursor: 'pointer',
                      width: 'fit-content',
                      transition: 'background 0.2s'
                    }}
                    onMouseOver={(e: React.MouseEvent<HTMLButtonElement>) => { e.currentTarget.style.background = 'rgba(255,255,255,0.3)'; }}
                    onMouseOut={(e: React.MouseEvent<HTMLButtonElement>) => { e.currentTarget.style.background = 'rgba(255,255,255,0.2)'; }}
                  >
                    Retry
                  </button>
                )}
              </div>
            </div>

            <button
              onClick={() => removeToast(toast.id)}
              style={{
                background: 'none',
                border: 'none',
                color: config.color || '#fff',
                cursor: 'pointer',
                fontSize: '20px',
                fontWeight: 'bold',
                opacity: 0.7,
                padding: 0,
                lineHeight: 1,
                marginLeft: '8px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
              onMouseOver={(e: React.MouseEvent<HTMLButtonElement>) => { e.currentTarget.style.opacity = '1'; }}
              onMouseOut={(e: React.MouseEvent<HTMLButtonElement>) => { e.currentTarget.style.opacity = '0.7'; }}
            >
              &times;
            </button>
          </div>
        );
      })}
    </div>
  );
};
