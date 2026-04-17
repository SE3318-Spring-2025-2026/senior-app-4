'use client';

import apiClient from '../lib/client';
import { showToast } from '../components/toast/ToastContext';

export default function HomePage() {
  const testSuccessToast = () => {
    showToast('Test success toast çalıştı', 'success');
  };

  const test404Toast = () => {
    apiClient.get('/fake-url').catch(() => { });
  };

  return (
    <div style={{ padding: '20px', fontFamily: 'sans-serif' }}>
      <h1>Toast Test Page</h1>

      <button
        onClick={testSuccessToast}
        style={{
          padding: '10px 16px',
          marginRight: '10px',
          background: '#16a34a',
          color: 'white',
          border: 'none',
          borderRadius: '6px',
          cursor: 'pointer',
        }}
      >
        Test Success Toast
      </button>

      <button
        onClick={test404Toast}
        style={{
          padding: '10px 16px',
          background: '#dc2626',
          color: 'white',
          border: 'none',
          borderRadius: '6px',
          cursor: 'pointer',
        }}
      >
        Test Error Toast
      </button>
    </div>
  );
}