import React, { useState } from 'react';
import { Award, CheckCircle, Heart, Scale, Star } from 'lucide-react';
import Badge from '../ui/Badge';

const formatPrice = (price) => `₹${Number(price || 0).toLocaleString('en-IN')}`;

export default function ProductCard({ product, isBestValue = false, isWishlisted, isCompared, onToggleWishlist, onToggleCompare, onViewDetails }) {
  const [imageFailed, setImageFailed] = useState(false);
  const specs = Object.entries(product.specs || {}).slice(0, 3);

  // Use dynamic backend label if present, otherwise fall back to isBestValue badge
  const badgeLabel = product.recommendationLabel || (isBestValue ? 'Top Pick' : null);
  const matchPct = product.matchPercentage != null ? Math.round(product.matchPercentage) : null;

  return (
    <article className={`relative flex flex-col rounded-2xl border bg-white p-5 transition-all duration-300 hover:-translate-y-1 hover:shadow-premium-hover ${(isBestValue || product.recommendationLabel) ? 'border-brand-200 shadow-premium ring-2 ring-brand-100/40' : 'border-slate-200/60 shadow-sm'}`}>
      {badgeLabel && <span className="absolute -top-3 left-4 z-10 inline-flex items-center gap-1 rounded-full bg-gradient-to-r from-brand-550 to-blue-600 px-3 py-1 text-[9px] font-black uppercase tracking-wider text-white shadow-md"><Award className="h-3 w-3" /> {badgeLabel}</span>}
      <button type="button" onClick={() => onToggleWishlist?.(product)} className={`absolute right-4 top-4 z-10 rounded-lg bg-white/90 p-2 shadow-sm transition hover:scale-105 ${isWishlisted ? 'text-rose-500' : 'text-slate-400 hover:text-rose-500'}`} aria-label={isWishlisted ? `Remove ${product.name} from wishlist` : `Add ${product.name} to wishlist`}><Heart className={`h-4 w-4 ${isWishlisted ? 'fill-current' : ''}`} /></button>

      <div className="group relative mb-4 flex aspect-[16/10] w-full items-center justify-center overflow-hidden rounded-xl border border-slate-200/50 bg-slate-50">
        {product.imageUrl && !imageFailed ? <img src={product.imageUrl} alt={product.name} loading="lazy" className="h-full w-full object-contain p-3 transition-transform duration-500 group-hover:scale-105" onError={() => setImageFailed(true)} /> : <span className="text-4xl" aria-label={`${product.category || 'product'} placeholder`} role="img">{product.emoji || '📦'}</span>}
      </div>

      <div className="flex items-center justify-between"><span className="text-[10px] font-extrabold uppercase tracking-widest text-slate-400">{product.brand}</span><div className="flex items-center gap-1.5"><div className="flex items-center gap-0.5 rounded-lg border border-amber-100/40 bg-amber-50 px-2 py-0.5 text-xs font-bold text-amber-700"><Star className="h-3 w-3 fill-amber-500 text-amber-500" />{Number(product.rating || 0).toFixed(1)}</div>{matchPct != null && <span className="rounded-lg border border-brand-100/60 bg-brand-50 px-2 py-0.5 text-[10px] font-bold text-brand-600" title="Match percentage">{matchPct}%</span>}</div></div>
      <p className="mt-1 text-[10px] font-medium text-slate-400">{Number(product.reviewCount || 0).toLocaleString('en-IN')} reviews</p>
      {product.source && <p className="mt-1 text-[10px] font-semibold uppercase tracking-wide text-slate-400">Source: {product.source === 'LOCAL' ? 'Local Catalog' : product.storeName || product.source}</p>}
      <h3 className="mt-2 line-clamp-1 text-sm font-bold leading-snug text-slate-900" title={product.name}>{product.name}</h3>
      <p className="mt-1 min-h-[34px] line-clamp-2 text-xs leading-relaxed text-slate-500">{product.description || 'Catalog product matched to your request.'}</p>
      <div className="mt-3.5 border-t border-slate-100/80 pt-3"><span className="text-xl font-black tracking-tight text-slate-900">{formatPrice(product.price)}</span></div>

      {specs.length > 0 && <div className="mt-3 rounded-xl border border-slate-200/30 bg-slate-50/70 p-3"><span className="mb-1.5 block text-[9px] font-extrabold uppercase tracking-widest text-slate-400">Specifications</span>{specs.map(([key, value]) => <div key={key} className="flex items-center justify-between border-b border-slate-100/50 py-1 text-[11px] last:border-0"><span className="text-slate-400">{key}</span><span className="max-w-[130px] truncate font-semibold text-slate-700" title={value}>{value}</span></div>)}</div>}
      {product.highlights?.length > 0 && <div className="mt-3 space-y-1">{product.highlights.slice(0, 2).map((highlight) => <div key={highlight} className="flex items-center gap-1.5 text-[11px] font-medium text-slate-600"><CheckCircle className="h-3.5 w-3.5 shrink-0 text-brand-550/80" /><span className="truncate">{highlight}</span></div>)}</div>}
      {product.tags?.length > 0 && <div className="mt-4 flex flex-wrap gap-1">{product.tags.slice(0, 3).map((tag) => <Badge key={tag} className="bg-slate-50 px-2 py-0.5 text-[9px]">#{tag}</Badge>)}</div>}
      <div className="mt-5 grid grid-cols-2 gap-2 border-t border-slate-100 pt-4"><button type="button" onClick={() => onToggleCompare?.(product)} className={`inline-flex items-center justify-center gap-1.5 rounded-xl px-3 py-2 text-xs font-bold transition ${isCompared ? 'bg-brand-50 text-brand-600' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'}`}><Scale className="h-3.5 w-3.5" />{isCompared ? 'Selected' : 'Compare'}</button><button type="button" onClick={() => onViewDetails?.(product)} className="rounded-xl bg-slate-900 px-3 py-2 text-xs font-bold text-white transition hover:bg-brand-550">View details</button></div>
    </article>
  );
}
