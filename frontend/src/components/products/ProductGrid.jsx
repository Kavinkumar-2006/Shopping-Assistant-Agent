import React from 'react';
import ProductCard from './ProductCard';

export default function ProductGrid({ products }) {
  if (!products || products.length === 0) return null;

  return (
    <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      {products.map((product, index) => (
        <ProductCard 
          key={product.id || index} 
          product={product} 
          isBestValue={index === 0} 
        />
      ))}
    </div>
  );
}
