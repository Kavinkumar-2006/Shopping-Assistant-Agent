import React from 'react';
import { Star, Check } from 'lucide-react';

export default function ComparisonTable({ products }) {
  if (!products || products.length === 0) return null;

  const formatPrice = (price) => {
    return '₹' + price.toLocaleString('en-IN');
  };

  const allSpecKeys = Array.from(
    new Set(
      products.flatMap(p => p.specs ? Object.keys(p.specs) : [])
    )
  );

  return (
    <div className="w-full overflow-hidden rounded-2xl border border-slate-200/70 bg-white shadow-premium">
      <div className="overflow-x-auto custom-scrollbar">
        <table className="w-full min-w-[650px] table-fixed border-collapse text-left text-sm">
          <thead>
            <tr className="bg-slate-50/50 border-b border-slate-200/60">
              <th scope="col" className="w-[180px] px-6 py-4.5 text-[10px] font-extrabold uppercase tracking-widest text-slate-400">
                Specifications
              </th>
              {products.map((p, idx) => (
                <th 
                  key={p.id || idx} 
                  scope="col" 
                  className={`px-6 py-4.5 text-xs font-extrabold uppercase tracking-wider relative ${
                    idx === 0 
                      ? 'text-brand-550 bg-brand-50/20 border-x border-brand-100/40' 
                      : 'text-slate-700'
                  }`}
                >
                  {idx === 0 && (
                    <span className="absolute top-1.5 right-6 inline-flex items-center gap-0.5 rounded-full bg-gradient-to-r from-brand-550 to-blue-600 px-2 py-0.5 text-[8px] font-black text-white uppercase tracking-wider shadow-sm">
                      Best Pick
                    </span>
                  )}
                  <div className="flex flex-col">
                    <span className="text-[9px] font-semibold text-slate-400 leading-none">{p.brand}</span>
                    <span className="text-xs font-extrabold mt-1 truncate" title={p.name}>{p.name.split(' ')[0]} {p.name.split(' ').slice(1).join(' ')}</span>
                  </div>
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-slate-600">
            {/* Price Row */}
            <tr className="hover:bg-slate-50/10">
              <td className="px-6 py-4 font-bold text-slate-800 bg-slate-50/20">Price</td>
              {products.map((p, idx) => (
                <td 
                  key={p.id || idx} 
                  className={`px-6 py-4 text-base font-black text-slate-900 ${
                    idx === 0 ? 'bg-brand-50/10 border-x border-brand-100/20' : ''
                  }`}
                >
                  {formatPrice(p.price)}
                </td>
              ))}
            </tr>

            {/* Rating Row */}
            <tr className="hover:bg-slate-50/10">
              <td className="px-6 py-4 font-bold text-slate-800 bg-slate-50/20">Rating</td>
              {products.map((p, idx) => (
                <td 
                  key={p.id || idx} 
                  className={`px-6 py-4 ${
                    idx === 0 ? 'bg-brand-50/10 border-x border-brand-100/20' : ''
                  }`}
                >
                  <div className="flex items-center gap-1.5">
                    <Star className="h-4.5 w-4.5 fill-amber-500 text-amber-500" />
                    <span className="font-bold text-slate-800">{p.rating.toFixed(1)}</span>
                    <span className="text-[11px] text-slate-400 font-medium">({p.reviewCount?.toLocaleString()})</span>
                  </div>
                </td>
              ))}
            </tr>

            {/* Dynamic Specification Rows */}
            {allSpecKeys.map(specKey => (
              <tr key={specKey} className="hover:bg-slate-50/10">
                <td className="px-6 py-4 font-semibold text-slate-500 bg-slate-50/20 truncate">{specKey}</td>
                {products.map((p, idx) => (
                  <td 
                    key={p.id || idx} 
                    className={`px-6 py-4 font-medium text-slate-700 truncate max-w-[200px] ${
                      idx === 0 ? 'bg-brand-50/10 border-x border-brand-100/20 font-bold text-slate-900' : ''
                    }`}
                    title={p.specs?.[specKey] || 'N/A'}
                  >
                    {p.specs?.[specKey] || <span className="text-slate-300">—</span>}
                  </td>
                ))}
              </tr>
            ))}

            {/* Bullet Highlights Row */}
            <tr className="hover:bg-slate-50/10">
              <td className="px-6 py-4.5 font-bold text-slate-800 bg-slate-50/20">Top Advantages</td>
              {products.map((p, idx) => (
                <td 
                  key={p.id || idx} 
                  className={`px-6 py-4.5 text-xs ${
                    idx === 0 ? 'bg-brand-50/10 border-x border-brand-100/20' : ''
                  }`}
                >
                  {p.highlights && p.highlights.length > 0 ? (
                    <ul className="space-y-1.5">
                      {p.highlights.slice(0, 3).map((hl, i) => (
                        <li key={i} className="flex items-start gap-1 text-slate-600 font-medium">
                          <Check className="h-3.5 w-3.5 text-brand-550 shrink-0 mt-0.5" />
                          <span>{hl}</span>
                        </li>
                      ))}
                    </ul>
                  ) : (
                    <span className="text-slate-300">—</span>
                  )}
                </td>
              ))}
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  );
}
