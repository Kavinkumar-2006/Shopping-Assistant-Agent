import React from 'react';
import { Sparkles, ShoppingBag, Terminal } from 'lucide-react';

export default function Header() {
  return (
    <header className="sticky top-0 z-50 w-full border-b border-slate-200/60 bg-white/70 backdrop-blur-md">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-brand-550 to-blue-600 text-white shadow-md shadow-brand-550/10">
              <ShoppingBag className="h-5.5 w-5.5" />
            </div>
            <div>
              <span className="text-lg font-extrabold tracking-tight text-slate-900 flex items-center gap-1.5">
                ShopSmart <span className="bg-gradient-to-r from-brand-550 to-blue-600 bg-clip-text text-transparent font-black flex items-center gap-0.5">AI</span>
              </span>
              <p className="text-[9px] font-semibold text-slate-400 uppercase tracking-widest leading-none mt-0.5">
                AI Shopping Co-Pilot
              </p>
            </div>
          </div>
          <div className="flex items-center gap-4">
            <div className="hidden md:flex items-center gap-1 text-xs font-semibold text-slate-400">
              <Terminal className="h-3.5 w-3.5" /> Local Engine v1.0
            </div>
            <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-100 bg-emerald-50/60 px-3 py-1 text-xs font-semibold text-emerald-700 shadow-sm">
              <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
              Agent Online
            </span>
          </div>
        </div>
      </div>
    </header>
  );
}
