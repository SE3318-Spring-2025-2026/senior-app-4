import React, { useState } from 'react';
import { Info } from 'lucide-react';

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
      {children || <Info size={16} className="text-gray-500 hover:text-blue-500 transition-colors cursor-help" />}
      
      {isVisible && (
        <div className="absolute z-50 w-64 p-2 mt-2 text-sm text-white bg-gray-900 rounded-lg shadow-lg -left-1/2 translate-x-1/4 top-full">
          {content}
          <div className="absolute w-2 h-2 bg-gray-900 rotate-45 -top-1 left-1/2 -translate-x-1/2"></div>
        </div>
      )}
    </div>
  );
};
