import React from 'react';
import { useToast } from './ToastContext';

export const ToastContainer: React.FC = () => {
  const { toasts, removeToast } = useToast();

  const getBackgroundColor = (type: string) => {
    switch (type) {
      case 'success':
        return '#4caf50'; // green
      case 'error':
        return '#f44336'; // red
      case 'warning':
        return '#ff9800'; // yellow
      case 'info':
        return '#2196f3'; // blue
      default:
        return '#333';
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
      {toasts.map((toast) => (
        <div
          key={toast.id}
          style={{
            backgroundColor: getBackgroundColor(toast.type),
            color: '#fff',
            padding: '16px 20px',
            borderRadius: '8px',
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            minWidth: '300px',
            fontFamily: 'sans-serif',
            transition: 'all 0.3s ease',
          }}
        >
          <span style={{ fontSize: '14px', lineHeight: '1.4' }}>{toast.message}</span>
          <button
            onClick={() => removeToast(toast.id)}
            style={{
              background: 'none',
              border: 'none',
              color: '#fff',
              cursor: 'pointer',
              marginLeft: '16px',
              fontSize: '20px',
              fontWeight: 'bold',
              opacity: 0.8,
              padding: 0,
              lineHeight: 1,
            }}
            onMouseOver={(e: React.MouseEvent<HTMLButtonElement>) => { e.currentTarget.style.opacity = '1'; }}
            onMouseOut={(e: React.MouseEvent<HTMLButtonElement>) => { e.currentTarget.style.opacity = '0.8'; }}
          >
            &times;
          </button>
        </div>
      ))}
    </div>
  );
};
