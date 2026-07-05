import React from 'react';
import { User, Sparkles, AlertCircle } from 'lucide-react';
import Badge from '../ui/Badge';
import ProductGrid from '../products/ProductGrid';
import ComparisonTable from '../products/ComparisonTable';

export default function MessageBubble({ message }) {
  const isUser = message.sender === 'user';

  const formatText = (text) => {
    if (!text) return '';
    const parts = text.split(/(\*\*.*?\*\*)/g);
    return parts.map((part, index) => {
      if (part.startsWith('**') && part.endsWith('**')) {
        return <strong key={index} className="font-bold text-slate-900">{part.slice(2, -2)}</strong>;
      }
      return part;
    });
  };

  return (
    <div className={`flex w-full gap-4 ${isUser ? 'justify-end' : 'justify-start'} py-2`}>
      {/* Avatar Redesign */}
      <div className={`flex h-9 w-9 shrink-0 select-none items-center justify-center rounded-xl border text-sm shadow-sm transition-all ${
        isUser 
          ? 'bg-slate-100 text-slate-600 border-slate-200/60 order-2' 
          : 'bg-gradient-to-tr from-brand-550 to-blue-600 text-white border-brand-500 shadow-md shadow-brand-500/10 order-1'
      }`}>
        {isUser ? <User className="h-4.5 w-4.5" /> : <Sparkles className="h-4.5 w-4.5" />}
      </div>

      {/* Bubble Container */}
      <div className={`flex flex-col gap-2.5 max-w-[85%] sm:max-w-[78%] ${isUser ? 'order-1 items-end' : 'order-2 items-start'}`}>
        
        {/* Message bubble card */}
        {message.text && (
          <div className={`rounded-2xl px-5 py-3.5 text-sm leading-relaxed shadow-sm border ${
            isUser
              ? 'bg-slate-800 text-white border-slate-800 font-medium'
              : 'bg-white text-slate-700 border-slate-200/60'
          }`}>
            <p className="whitespace-pre-wrap">{formatText(message.text)}</p>
          </div>
        )}

        {/* Filter Badges Layout */}
        {!isUser && (message.category || message.budget || message.useCase) && (
          <div className="flex flex-wrap items-center gap-2 mt-1">
            <span className="text-[9px] uppercase font-extrabold text-slate-400 tracking-wider">Detected:</span>
            {message.category && <Badge variant="brand">{message.category}</Badge>}
            {message.budget && message.budget > 0 && (
              <Badge variant="success">
                ≤ ₹{message.budget.toLocaleString('en-IN')}
              </Badge>
            )}
            {message.useCase && <Badge variant="warning">{message.useCase}</Badge>}
          </div>
        )}

        {/* Product Grid Render */}
        {!isUser && message.products && message.products.length > 0 && (
          <div className="w-full mt-4">
            <div className="flex items-center gap-2 mb-3.5">
              <span className="h-1 w-8 bg-brand-550 rounded-full"></span>
              <h4 className="text-[10px] font-extrabold uppercase tracking-widest text-slate-400">
                Assistant Recommendations ({message.products.length})
              </h4>
            </div>
            <ProductGrid products={message.products} />
          </div>
        )}

        {/* Comparison Table Render */}
        {!isUser && message.topProducts && message.topProducts.length >= 2 && (
          <div className="w-full mt-6">
            <div className="flex items-center gap-2 mb-3.5">
              <span className="h-1 w-8 bg-brand-550 rounded-full"></span>
              <h4 className="text-[10px] font-extrabold uppercase tracking-widest text-slate-400">
                Top Specs Comparison
              </h4>
            </div>
            <ComparisonTable products={message.topProducts} />
          </div>
        )}

        {/* Empty Search Result Alert */}
        {!isUser && message.products && message.products.length === 0 && (
          <div className="flex items-center gap-2.5 rounded-xl bg-slate-50 border border-slate-200/50 p-4 mt-2 text-xs text-slate-500 shadow-inner">
            <AlertCircle className="h-4.5 w-4.5 text-slate-400 shrink-0" />
            <span className="font-medium">No direct matches. Adjust search params to see recommended choices.</span>
          </div>
        )}
      </div>
    </div>
  );
}
