import React, { useState } from 'react';
import { ExternalLink, Heart, Scale, Star, X } from 'lucide-react';
import Badge from '../ui/Badge';

const formatPrice = (price) => `₹${Number(price || 0).toLocaleString('en-IN')}`;

function ProductImage({ product }) {
  const [failed, setFailed] = useState(false);
  return <div className="flex aspect-square items-center justify-center overflow-hidden rounded-2xl border border-slate-100 bg-slate-50">{product.imageUrl && !failed ? <img src={product.imageUrl} alt={product.name} className="h-full w-full object-contain p-6" onError={() => setFailed(true)} /> : <span className="text-6xl" role="img" aria-label={`${product.category || 'product'} placeholder`}>{product.emoji || '📦'}</span>}</div>;
}

export default function ProductDetailsDialog({ product, onClose, isWishlisted, onToggleWishlist, isCompared, onToggleCompare }) {
  if (!product) return null;
  const hasExternalProductUrl = product.source && product.source !== 'LOCAL' && /^https?:\/\//i.test(product.productUrl || '');
  return <div className="fixed inset-0 z-[60] flex items-end bg-slate-950/35 p-0 backdrop-blur-sm sm:items-center sm:justify-center sm:p-6" role="dialog" aria-modal="true" aria-labelledby="product-details-title" onMouseDown={onClose}>
    <article className="max-h-[92vh] w-full max-w-4xl overflow-y-auto rounded-t-3xl bg-white shadow-2xl sm:rounded-3xl" onMouseDown={(event) => event.stopPropagation()}>
      <div className="sticky top-0 z-10 flex justify-end bg-white/90 p-4 backdrop-blur"><button type="button" onClick={onClose} className="rounded-xl p-2 text-slate-500 hover:bg-slate-100" aria-label="Close product details"><X className="h-5 w-5" /></button></div>
      <div className="grid gap-8 px-6 pb-8 sm:grid-cols-2 sm:px-8"><ProductImage product={product} />
        <div><p className="text-xs font-extrabold uppercase tracking-widest text-brand-550">{product.brand || 'ShopSmart pick'}</p><h1 id="product-details-title" className="mt-2 text-2xl font-black tracking-tight text-slate-900">{product.name}</h1><div className="mt-3 flex items-center gap-2 text-sm font-semibold text-slate-600"><Star className="h-4 w-4 fill-amber-400 text-amber-400" /> {Number(product.rating || 0).toFixed(1)} <span className="font-normal text-slate-400">({Number(product.reviewCount || 0).toLocaleString()} reviews)</span></div>{product.source && <p className="mt-2 text-xs font-semibold text-slate-400">Source: {product.source === 'LOCAL' ? 'Local Catalog' : product.storeName || product.source}</p>}<p className="mt-5 text-3xl font-black text-slate-900">{formatPrice(product.price)}</p><p className="mt-4 text-sm leading-relaxed text-slate-500">{product.description || 'Product details are provided by the current catalog.'}</p><div className="mt-6 rounded-2xl border border-brand-100 bg-brand-50/50 p-4"><p className="text-sm font-bold text-slate-800">Why ShopSmart recommends this</p><p className="mt-1 text-xs leading-relaxed text-slate-600">A strong match based on its {product.tags?.slice(0, 2).join(' and ') || 'catalog'} profile, price, and customer rating.</p></div><div className="mt-6 flex flex-wrap gap-2"><button type="button" onClick={() => onToggleWishlist(product)} className={`inline-flex items-center gap-2 rounded-xl border px-4 py-2.5 text-sm font-bold ${isWishlisted ? 'border-rose-200 bg-rose-50 text-rose-600' : 'border-slate-200 text-slate-700 hover:bg-slate-50'}`}><Heart className={`h-4 w-4 ${isWishlisted ? 'fill-current' : ''}`} /> {isWishlisted ? 'Saved' : 'Save'}</button><button type="button" onClick={() => onToggleCompare(product)} className={`inline-flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm font-bold ${isCompared ? 'bg-brand-50 text-brand-600' : 'bg-slate-900 text-white hover:bg-slate-800'}`}><Scale className="h-4 w-4" /> {isCompared ? 'Selected' : 'Compare'}</button>{hasExternalProductUrl && <a href={product.productUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-2 rounded-xl border border-brand-200 px-4 py-2.5 text-sm font-bold text-brand-600 hover:bg-brand-50">View product <ExternalLink className="h-4 w-4" /></a>}</div></div>
        <div className="sm:col-span-2"><h2 className="text-sm font-bold text-slate-900">Specifications</h2><dl className="mt-3 grid gap-2 sm:grid-cols-2">{Object.entries(product.specs || {}).map(([key, value]) => <div key={key} className="flex justify-between rounded-xl bg-slate-50 px-4 py-3 text-sm"><dt className="text-slate-500">{key}</dt><dd className="font-semibold text-slate-800">{value}</dd></div>)}</dl><div className="mt-4 flex flex-wrap gap-2">{(product.tags || []).map((tag) => <Badge key={tag} variant="brand">{tag}</Badge>)}</div></div>
      </div>
    </article>
  </div>;
}
