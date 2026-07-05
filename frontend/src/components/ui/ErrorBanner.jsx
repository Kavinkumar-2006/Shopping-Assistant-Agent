import React from 'react';
import { AlertCircle, XCircle } from 'lucide-react';

export default function ErrorBanner({ message, onDismiss }) {
  if (!message) return null;

  return (
    <div className="rounded-xl border border-red-100 bg-red-50 p-4 shadow-sm">
      <div className="flex items-start gap-3">
        <div className="text-red-500 mt-0.5">
          <AlertCircle className="h-5 w-5" />
        </div>
        <div className="flex-grow">
          <h3 className="text-sm font-semibold text-red-800">Recommendation Failed</h3>
          <p className="text-xs text-red-700 mt-1 leading-relaxed">{message}</p>
        </div>
        {onDismiss && (
          <button 
            onClick={onDismiss}
            className="text-red-400 hover:text-red-600 transition-colors"
          >
            <XCircle className="h-4 w-4" />
          </button>
        )}
      </div>
    </div>
  );
}
