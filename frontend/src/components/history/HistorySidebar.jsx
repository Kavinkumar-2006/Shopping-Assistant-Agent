import React from 'react';
import { Clock3, MessageSquare, Trash2, X } from 'lucide-react';

export default function HistorySidebar({ history, onReopen, onDelete, onClear, open, onClose }) {
  return (
    <aside className={`fixed inset-y-0 left-0 z-50 flex w-80 flex-col border-r border-slate-200 bg-white shadow-2xl transition-transform duration-300 lg:static lg:z-0 lg:w-64 lg:translate-x-0 lg:shadow-none ${open ? 'translate-x-0' : '-translate-x-full'}`} aria-label="Shopping history">
      <div className="flex items-center justify-between border-b border-slate-100 px-5 py-5">
        <div>
          <p className="text-xs font-extrabold uppercase tracking-widest text-slate-400">Workspace</p>
          <h2 className="mt-1 text-base font-bold text-slate-900">Shopping history</h2>
        </div>
        <button type="button" onClick={onClose} className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700 lg:hidden" aria-label="Close sidebar"><X className="h-5 w-5" /></button>
      </div>
      <div className="flex-1 overflow-y-auto p-3 custom-scrollbar">
        {history.length ? history.map((item) => (
          <div key={item.id} className="group mb-1 flex items-center gap-2 rounded-xl p-2 hover:bg-slate-50">
            <button type="button" onClick={() => onReopen(item.query)} className="min-w-0 flex-1 text-left">
              <span className="flex items-center gap-2 text-xs font-semibold text-slate-700"><MessageSquare className="h-3.5 w-3.5 shrink-0 text-brand-550" /> <span className="truncate">{item.query}</span></span>
              <span className="mt-1 block pl-5.5 text-[10px] text-slate-400">{new Date(item.createdAt).toLocaleDateString()}</span>
            </button>
            <button type="button" onClick={() => onDelete(item.id)} className="rounded-lg p-1.5 text-slate-300 opacity-0 transition hover:bg-red-50 hover:text-red-500 group-hover:opacity-100 focus:opacity-100" aria-label={`Delete ${item.query} from history`}><Trash2 className="h-3.5 w-3.5" /></button>
          </div>
        )) : <div className="px-4 py-10 text-center"><Clock3 className="mx-auto h-6 w-6 text-slate-300" /><p className="mt-3 text-xs font-medium text-slate-400">Your searches will appear here.</p></div>}
      </div>
      {history.length > 0 && <div className="border-t border-slate-100 p-3"><button type="button" onClick={onClear} className="flex w-full items-center justify-center gap-2 rounded-xl px-3 py-2.5 text-xs font-semibold text-slate-500 transition hover:bg-red-50 hover:text-red-600"><Trash2 className="h-3.5 w-3.5" /> Clear history</button></div>}
    </aside>
  );
}
