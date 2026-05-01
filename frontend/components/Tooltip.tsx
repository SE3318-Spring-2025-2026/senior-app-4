"use client";

import React, { useState } from 'react';

interface TooltipProps {
  content: string;
  children?: React.ReactNode;
}

export const Tooltip: React.FC<TooltipProps> = ({ content, children }) => {
  const [isVisible, setIsVisible] = useState(false);

  return (
    <div 
      className="relative inline-flex items-center"
      onMouseEnter={() => setIsVisible(true)}
      onMouseLeave={() => setIsVisible(false)}
      onFocus={() => setIsVisible(true)}
      onBlur={() => setIsVisible(false)}
      tabIndex={0}
      aria-label={content}
      role="tooltip"
    >
      {children || (
        <svg 
          xmlns="http://www.w3.org/2000/svg" 
          width="16" 
          height="16" 
          viewBox="0 0 24 24" 
          fill="none" 
          stroke="currentColor" 
          strokeWidth="2" 
          strokeLinecap="round" 
          strokeLinejoin="round" 
          className="text-gray-500 hover:text-blue-500 transition-colors cursor-help"
        >
          <circle cx="12" cy="12" r="10"></circle>
          <line x1="12" y1="16" x2="12" y2="12"></line>
          <line x1="12" y1="8" x2="12.01" y2="8"></line>
        </svg>
      )}
      
      {isVisible && (
        <div className="absolute z-50 w-64 p-2 mt-2 text-sm text-white bg-gray-900 rounded-lg shadow-lg left-1/2 -translate-x-1/2 top-full pointer-events-none">
          {content}
          <div className="absolute w-2 h-2 bg-gray-900 rotate-45 -top-1 left-1/2 -translate-x-1/2"></div>
        </div>
      )}
    </div>
  );
};
