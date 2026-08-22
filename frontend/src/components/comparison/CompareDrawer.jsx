import React from 'react';
import { Scale, X } from 'lucide-react';
import ComparisonTable from '../products/ComparisonTable';

export default function CompareDrawer({ products, onRemove, onClear, onClose, onViewProduct }) {
  if (!products.length) return null;
  return (
    <section className="fixed inset-x-0 bottom-0 z-40 mx-auto max-w-7xl border border-slate-200 bg-white p-4 shadow-[0_-12px_40px_rgba(15,23,42,.16)] sm:bottom-4 sm:rounded-2xl sm:p-5" aria-label="Product comparison tray">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2"><span className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-50 text-brand-550"><Scale className="h-4 w-4" /></span><div><h2 className="text-sm font-bold text-slate-900">Compare products</h2><p className="text-[11px] text-slate-400">Select up to three products for a clear comparison.</p></div></div>
        <div className="flex items-center gap-2"><button type="button" onClick={onClear} className="rounded-lg px-3 py-2 text-xs font-semibold text-slate-500 hover:bg-slate-100">Clear</button><button type="button" onClick={onClose} className="rounded-lg p-2 text-slate-400 hover:bg-slate-100" aria-label="Close comparison"><X className="h-4 w-4" /></button></div>
      </div>
      <div className="mb-4 flex gap-2 overflow-x-auto pb-1 custom-scrollbar">
        {products.map((product) => <div key={product.id} className="flex shrink-0 items-center gap-2 rounded-xl border border-slate-200 px-2 py-1.5"><button type="button" onClick={() => onViewProduct(product)} className="text-xs font-semibold text-slate-700 hover:text-brand-550">{product.name}</button><button type="button" onClick={() => onRemove(product.id)} className="text-slate-400 hover:text-red-500" aria-label={`Remove ${product.name}`}><X className="h-3.5 w-3.5" /></button></div>)}
      </div>
      {products.length >= 2 ? <ComparisonTable products={products} /> : <p className="rounded-xl bg-slate-50 px-4 py-3 text-xs text-slate-500">Choose one more product to see the comparison table.</p>}
    </section>
  );
}
