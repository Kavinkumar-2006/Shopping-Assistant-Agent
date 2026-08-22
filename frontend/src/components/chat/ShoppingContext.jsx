import React from 'react';
import { SlidersHorizontal, X } from 'lucide-react';

const entries = (context) => [
  ['category', context.category],
  ['maxPrice', context.maxPrice ? `Up to ₹${Number(context.maxPrice).toLocaleString('en-IN')}` : null],
  ['useCase', context.useCase],
  ['brand', context.brand],
  ['preferredOperatingSystem', context.preferredOperatingSystem],
  ...(context.requiredFeatures || []).map((feature) => ['feature', feature]),
];

export default function ShoppingContext({ context, onRemove }) {
  const activeEntries = context ? entries(context).filter(([, value]) => value) : [];
  if (!activeEntries.length) return null;
  return <section className="border-b border-slate-200/70 bg-white/80 px-5 py-3 sm:px-6" aria-label="Current shopping preferences"><div className="mx-auto flex max-w-6xl flex-wrap items-center gap-2"><span className="mr-1 inline-flex items-center gap-1.5 text-[10px] font-extrabold uppercase tracking-widest text-slate-400"><SlidersHorizontal className="h-3.5 w-3.5" /> Current search</span>{activeEntries.map(([key, value], index) => <button key={`${key}-${index}`} type="button" onClick={() => onRemove(key, value)} className="inline-flex items-center gap-1 rounded-full border border-brand-100 bg-brand-50 px-2.5 py-1 text-[11px] font-bold capitalize text-brand-700 hover:bg-brand-100" aria-label={`Remove ${value} filter`}>{value}<X className="h-3 w-3" /></button>)}</div></section>;
}
