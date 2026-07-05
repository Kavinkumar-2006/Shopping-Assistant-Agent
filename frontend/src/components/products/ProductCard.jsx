import React from 'react';
import { Star, Tag, Award, CheckCircle } from 'lucide-react';
import Badge from '../ui/Badge';

export default function ProductCard({ product, isBestValue = false }) {
  // Format price into Indian Rupees format
  const formatPrice = (price) => {
    return '₹' + price.toLocaleString('en-IN');
  };

  const renderSpecs = () => {
    if (!product.specs) return null;
    return Object.entries(product.specs).slice(0, 3).map(([key, val]) => (
      <div key={key} className="flex justify-between items-center text-[11px] py-1 border-b border-slate-100/50 last:border-0">
        <span className="text-slate-400 font-medium">{key}</span>
        <span className="font-semibold text-slate-700 truncate max-w-[130px]" title={val}>{val}</span>
      </div>
    ));
  };

  return (
    <div className={`relative flex flex-col rounded-2xl border bg-white p-5 transition-all duration-300 hover:shadow-premium-hover ${
      isBestValue 
        ? 'border-brand-200 shadow-premium ring-2 ring-brand-100/40' 
        : 'border-slate-200/60 shadow-sm'
    }`}>
      {/* Best Value Highlight Badge */}
      {isBestValue && (
        <span className="absolute -top-3.5 left-4 inline-flex items-center gap-1 rounded-full bg-gradient-to-r from-brand-550 to-blue-600 px-3 py-1 text-[9px] font-black text-white uppercase tracking-wider shadow-md">
          <Award className="h-3 w-3" /> Top Pick
        </span>
      )}

      {/* Product Image Panel */}
      <div className="relative aspect-[16/10] w-full overflow-hidden rounded-xl bg-slate-50 flex items-center justify-center border border-slate-200/50 mb-4 group">
        {product.imageUrl ? (
          <img 
            src={product.imageUrl} 
            alt={product.name}
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
            onError={(e) => {
              e.target.style.display = 'none';
              e.target.nextSibling.style.display = 'flex';
            }}
          />
        ) : null}
        <div 
          className="absolute inset-0 flex items-center justify-center text-4xl" 
          style={{ display: product.imageUrl ? 'none' : 'flex' }}
        >
          {product.emoji || '📦'}
        </div>
      </div>

      {/* Brand & Rating Bar */}
      <div className="flex items-center justify-between">
        <span className="text-[10px] font-extrabold uppercase tracking-widest text-slate-400">
          {product.brand}
        </span>
        <div className="flex items-center gap-0.5 rounded-lg bg-amber-50 px-2 py-0.5 text-xs font-bold text-amber-700 border border-amber-100/40">
          <Star className="h-3 w-3 fill-amber-500 text-amber-500" />
          {product.rating.toFixed(1)}
        </div>
      </div>

      {/* Product Information */}
      <h3 className="mt-2 text-sm font-bold text-slate-900 line-clamp-1 leading-snug hover:text-brand-550 transition-colors" title={product.name}>
        {product.name}
      </h3>
      <p className="mt-1 text-xs text-slate-500 line-clamp-2 min-h-[34px] leading-relaxed">
        {product.description}
      </p>

      {/* Pricing Header */}
      <div className="mt-3.5 flex items-baseline gap-1 border-t border-slate-100/80 pt-3">
        <span className="text-xl font-black text-slate-900 tracking-tight">
          {formatPrice(product.price)}
        </span>
      </div>

      {/* Specifications Block */}
      <div className="mt-3 bg-slate-50/70 rounded-xl p-3 border border-slate-200/30">
        <span className="text-[9px] font-extrabold text-slate-400 uppercase tracking-widest block mb-1.5">Specifications</span>
        {renderSpecs()}
      </div>

      {/* Key Highlights list */}
      {product.highlights && product.highlights.length > 0 && (
        <div className="mt-3 space-y-1">
          {product.highlights.slice(0, 2).map((hl, i) => (
            <div key={i} className="flex items-center gap-1.5 text-[11px] text-slate-600 font-medium">
              <CheckCircle className="h-3.5 w-3.5 text-brand-550/80 shrink-0" />
              <span className="truncate">{hl}</span>
            </div>
          ))}
        </div>
      )}

      {/* Tag pills bar */}
      {product.tags && product.tags.length > 0 && (
        <div className="flex flex-wrap gap-1 mt-4">
          {product.tags.slice(0, 3).map((tag) => (
            <Badge key={tag} className="text-[9px] font-bold py-0.5 px-2 bg-slate-50 hover:bg-slate-100 transition-colors">
              #{tag}
            </Badge>
          ))}
        </div>
      )}
    </div>
  );
}
