import React, { useMemo, useState } from 'react';
import { SlidersHorizontal } from 'lucide-react';
import ProductCard from './ProductCard';

export default function ProductGrid({ products, wishlistIds = [], comparedIds = [], onToggleWishlist, onToggleCompare, onViewDetails }) {
  const [sortBy, setSortBy] = useState('recommended');
  const [brand, setBrand] = useState('all');
  const [rating, setRating] = useState('all');
  const brands = useMemo(() => [...new Set(products.map((product) => product.brand).filter(Boolean))], [products]);
  const visibleProducts = useMemo(() => products.filter((product) => (brand === 'all' || product.brand === brand) && (rating === 'all' || Number(product.rating || 0) >= Number(rating))).sort((left, right) => {
    if (sortBy === 'price-asc') return left.price - right.price;
    if (sortBy === 'price-desc') return right.price - left.price;
    if (sortBy === 'rating') return right.rating - left.rating;
    if (sortBy === 'popularity') return (right.reviewCount || 0) - (left.reviewCount || 0);
    return 0;
  }), [products, brand, rating, sortBy]);

  if (!products?.length) return null;
  return <section aria-label="Recommended products">
    <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-xl border border-slate-200/70 bg-white p-3 shadow-sm">
      <div className="flex items-center gap-2"><SlidersHorizontal className="h-4 w-4 text-brand-550" /><span className="text-xs font-bold text-slate-700">Recommended for you</span><span className="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-bold text-slate-500">{visibleProducts.length} results</span></div>
      <div className="flex flex-wrap items-center gap-2"><label className="sr-only" htmlFor="product-brand">Filter by brand</label><select id="product-brand" value={brand} onChange={(event) => setBrand(event.target.value)} className="rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-[11px] font-semibold text-slate-600 focus:border-brand-550 focus:outline-none"><option value="all">All brands</option>{brands.map((item) => <option key={item} value={item}>{item}</option>)}</select><label className="sr-only" htmlFor="product-rating">Minimum rating</label><select id="product-rating" value={rating} onChange={(event) => setRating(event.target.value)} className="rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-[11px] font-semibold text-slate-600 focus:border-brand-550 focus:outline-none"><option value="all">Any rating</option><option value="4">4.0+ rating</option><option value="4.5">4.5+ rating</option></select><label className="sr-only" htmlFor="product-sort">Sort products</label><select id="product-sort" value={sortBy} onChange={(event) => setSortBy(event.target.value)} className="rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-[11px] font-semibold text-slate-600 focus:border-brand-550 focus:outline-none"><option value="recommended">Recommended</option><option value="price-asc">Price: low to high</option><option value="price-desc">Price: high to low</option><option value="rating">Rating</option><option value="popularity">Popularity</option></select></div>
    </div>
    {visibleProducts.length ? <div className="grid gap-5 sm:grid-cols-2 xl:grid-cols-3">{visibleProducts.map((product, index) => <ProductCard key={product.id || index} product={product} isBestValue={index === 0 && sortBy === 'recommended'} isWishlisted={wishlistIds.includes(product.id)} isCompared={comparedIds.includes(product.id)} onToggleWishlist={onToggleWishlist} onToggleCompare={onToggleCompare} onViewDetails={onViewDetails} />)}</div> : <div className="rounded-2xl border border-dashed border-slate-200 bg-white p-8 text-center text-sm text-slate-500">No products match these filters. Try a different brand or rating.</div>}
  </section>;
}
