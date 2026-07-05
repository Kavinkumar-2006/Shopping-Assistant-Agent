import React from 'react';
import { Sparkles } from 'lucide-react';

export default function LoadingState() {
  return (
    <div className="flex flex-col gap-4 p-5 rounded-2xl bg-white border border-slate-100 shadow-premium animate-pulse">
      <div className="flex items-center gap-3">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-50 text-brand-550">
          <Sparkles className="h-4 w-4 animate-spin" />
        </div>
        <div className="h-4 w-40 bg-slate-100 rounded-md"></div>
      </div>
      <div className="space-y-2 mt-2">
        <div className="h-3 w-full bg-slate-100 rounded-md"></div>
        <div className="h-3 w-5/6 bg-slate-100 rounded-md"></div>
        <div className="h-3 w-3/4 bg-slate-100 rounded-md"></div>
      </div>
    </div>
  );
}
