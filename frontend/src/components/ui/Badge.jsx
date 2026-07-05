import React from 'react';

/**
 * Reusable Badge component for categorisation tags.
 *
 * @param {object} props
 * @param {'brand' | 'success' | 'warning' | 'neutral' | 'dark'} props.variant Style color scheme
 */
export default function Badge({ children, variant = 'neutral', className = '' }) {
  const styles = {
    brand: 'bg-brand-50/50 text-brand-550 border-brand-100/60',
    success: 'bg-emerald-50/50 text-emerald-700 border-emerald-100/60',
    warning: 'bg-amber-50/50 text-amber-700 border-amber-100/60',
    neutral: 'bg-slate-50 text-slate-600 border-slate-200/50',
    dark: 'bg-slate-900 text-white border-slate-900'
  };

  return (
    <span className={`inline-flex items-center rounded-lg border px-2.5 py-0.5 text-[10px] font-bold tracking-wide transition-all ${styles[variant]} ${className}`}>
      {children}
    </span>
  );
}
