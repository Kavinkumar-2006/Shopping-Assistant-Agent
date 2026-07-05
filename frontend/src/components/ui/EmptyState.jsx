import React from 'react';
import { Sparkles, ArrowRight } from 'lucide-react';
import { SUGGESTIONS } from '../../constants/suggestions';

export default function EmptyState({ onSelectSuggestion }) {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-6 text-center max-w-2xl mx-auto">
      {/* Premium glowing icon container */}
      <div className="relative mb-6 flex h-20 w-20 items-center justify-center rounded-3xl bg-gradient-to-tr from-brand-50 to-blue-50 text-brand-550 border border-slate-100 shadow-inner">
        <Sparkles className="h-9 w-9 animate-pulse" />
      </div>

      <h1 className="text-3xl font-black tracking-tight text-slate-900 sm:text-4xl">
        ShopSmart <span className="bg-gradient-to-r from-brand-550 to-blue-600 bg-clip-text text-transparent">AI Agent</span>
      </h1>
      
      <p className="mt-4 text-sm sm:text-base text-slate-500 max-w-lg leading-relaxed font-medium">
        Natural language shopping co-pilot. Enter what you are seeking and I will extract category filters, match budgets, and recommend products.
      </p>

      {/* Suggestion Card Grid */}
      <div className="w-full mt-12">
        <div className="flex items-center gap-2 mb-4">
          <span className="h-0.5 w-6 bg-brand-500 rounded-full"></span>
          <span className="text-[10px] font-extrabold uppercase tracking-widest text-slate-400">
            Suggested Queries
          </span>
        </div>
        <div className="grid gap-3.5 sm:grid-cols-2">
          {SUGGESTIONS.map((item, index) => (
            <button
              key={index}
              onClick={() => onSelectSuggestion(item.query)}
              className="flex items-center justify-between p-4.5 rounded-2xl border border-slate-200/60 bg-white text-left hover:border-brand-300 hover:bg-brand-50/10 shadow-sm transition-all group hover:-translate-y-0.5 duration-200 cursor-pointer"
            >
              <div className="flex items-center gap-3.5 overflow-hidden">
                <span className="text-2xl shrink-0" role="img" aria-label={item.category}>
                  {item.icon}
                </span>
                <div className="overflow-hidden">
                  <span className="text-[9px] font-bold text-brand-550 uppercase tracking-widest block leading-none">
                    {item.category}
                  </span>
                  <p className="text-xs font-bold text-slate-800 mt-1 line-clamp-1 truncate" title={item.label}>
                    {item.label}
                  </p>
                </div>
              </div>
              <ArrowRight className="h-4 w-4 text-slate-300 shrink-0 group-hover:text-brand-550 group-hover:translate-x-0.5 transition-all" />
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
