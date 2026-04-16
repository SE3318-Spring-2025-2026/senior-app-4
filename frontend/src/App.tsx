import React from 'react';
import { ToastProvider } from './components/toast/ToastContext';
import { ToastContainer } from './components/toast/ToastContainer';
import apiClient from './api/client';

export const App = () => {
  // test request to see global error handle works!
  const triggerError = () => {
    apiClient.get('/simulate-404-error').catch(() => {});
  };

  return (
    <ToastProvider>
      <ToastContainer />
      <div style={{ padding: '20px', fontFamily: 'sans-serif' }}>
        <h2>Global Toast & Error Handling Demo</h2>
        <p>Click the button below to simulate an API request that will fail with a 404 error.</p>
        <button 
          onClick={triggerError}
          style={{
            padding: '10px 16px',
            background: '#e84118',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer',
            fontWeight: 'bold',
            marginTop: '10px'
          }}
        >
          Simulate 404 Error
        </button>
      </div>
    </ToastProvider>
  );
};

export default App;
