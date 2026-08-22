import React from 'react';
import { Sparkles } from 'lucide-react';

export default function LoadingState() {
  return (
    <div className="flex flex-col gap-4 rounded-2xl border border-slate-100 bg-white p-5 shadow-premium" role="status" aria-live="polite">
      <div className="flex items-center gap-3">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-50 text-brand-550">
          <Sparkles className="h-4 w-4 animate-spin" />
        </div>
        <div><p className="text-sm font-bold text-slate-700">Analyzing your shopping request</p><p className="text-xs text-slate-400">Matching products, budgets, and preferences…</p></div>
      </div>
      <div className="space-y-2 mt-2">
        <div className="h-3 w-full animate-pulse rounded-md bg-slate-100"></div>
        <div className="h-3 w-5/6 animate-pulse rounded-md bg-slate-100"></div>
        <div className="h-3 w-3/4 animate-pulse rounded-md bg-slate-100"></div>
      </div>
      <div className="grid grid-cols-3 gap-3 pt-2" aria-hidden="true">
        {[0, 1, 2].map((item) => <div key={item} className="rounded-xl border border-slate-100 p-2"><div className="aspect-[4/3] animate-pulse rounded-lg bg-slate-100"></div><div className="mt-2 h-2 w-2/3 animate-pulse rounded bg-slate-100"></div><div className="mt-1.5 h-2 w-1/2 animate-pulse rounded bg-slate-100"></div></div>)}
      </div>
    </div>
  );
}
