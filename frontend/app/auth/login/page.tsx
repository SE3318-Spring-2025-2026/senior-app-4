'use client';

import { showToast } from '../../../components/toast/ToastContext';

export default function LoginPage() {
  const testSuccessToast = () => {
    showToast('Success çalıştı', 'success');
  };

  const testErrorToast = () => {
    showToast('Resource not found', 'error');
  };

  return (
    <div style={{ padding: 20 }}>
      <h1>LOGIN PAGE (TEST)</h1>

      <button onClick={testSuccessToast}>Success Test</button>
      <button onClick={testErrorToast} style={{ marginLeft: 10 }}>
        Error Test
      </button>
    </div>
  );
}