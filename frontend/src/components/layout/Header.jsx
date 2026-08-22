import React from 'react';
import { Heart, Menu, Scale, Search, ShoppingBag, UserRound } from 'lucide-react';

const navItems = [
  { id: 'home', label: 'Home' },
  { id: 'explore', label: 'Explore' },
  { id: 'compare', label: 'Compare' },
  { id: 'wishlist', label: 'Wishlist' },
  { id: 'history', label: 'History' },
];

export default function Header({ onMenu, onNavigate, activeView = 'home', wishlistCount = 0, compareCount = 0 }) {
  return <header className="sticky top-0 z-50 w-full border-b border-slate-200/70 bg-white/90 backdrop-blur-xl">
    <div className="mx-auto flex h-16 max-w-[1600px] items-center gap-4 px-4 sm:px-6">
      <button type="button" onClick={onMenu} className="rounded-xl p-2 text-slate-500 hover:bg-slate-100 lg:hidden" aria-label="Open shopping history"><Menu className="h-5 w-5" /></button>
      <button type="button" onClick={() => onNavigate('home')} className="flex shrink-0 items-center gap-2.5 text-left" aria-label="ShopSmart AI home"><span className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-brand-550 to-blue-600 text-white shadow-md shadow-brand-550/20"><ShoppingBag className="h-5 w-5" /></span><span className="hidden sm:block"><span className="block text-base font-black tracking-tight text-slate-900">ShopSmart <em className="not-italic text-brand-550">AI</em></span><span className="block text-[9px] font-bold uppercase tracking-[.16em] text-slate-400">Personal shopping assistant</span></span></button>
      <nav className="ml-4 hidden items-center gap-1 xl:flex" aria-label="Main navigation">{navItems.map((item) => <button key={item.id} type="button" onClick={() => onNavigate(item.id)} className={`rounded-lg px-3 py-2 text-xs font-bold transition ${activeView === item.id ? 'bg-brand-50 text-brand-600' : 'text-slate-500 hover:bg-slate-50 hover:text-slate-900'}`}>{item.label}</button>)}</nav>
      <div className="ml-auto flex items-center gap-1.5"><button type="button" onClick={() => onNavigate('explore')} className="hidden items-center gap-2 rounded-xl border border-slate-200 px-3 py-2 text-xs font-bold text-slate-600 hover:border-brand-200 hover:text-brand-600 md:flex"><Search className="h-4 w-4" /> Ask ShopSmart</button><button type="button" onClick={() => onNavigate('compare')} className="relative rounded-xl p-2.5 text-slate-500 hover:bg-slate-100 hover:text-brand-550" aria-label="Compare selected products"><Scale className="h-4.5 w-4.5" />{compareCount > 0 && <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-brand-550 px-1 text-[9px] font-bold text-white">{compareCount}</span>}</button><button type="button" onClick={() => onNavigate('wishlist')} className="relative rounded-xl p-2.5 text-slate-500 hover:bg-slate-100 hover:text-rose-500" aria-label="Open wishlist"><Heart className="h-4.5 w-4.5" />{wishlistCount > 0 && <span className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-rose-500 px-1 text-[9px] font-bold text-white">{wishlistCount}</span>}</button><button type="button" className="hidden rounded-xl border border-slate-200 p-2 text-slate-500 hover:bg-slate-100 sm:block" aria-label="Profile"><UserRound className="h-4.5 w-4.5" /></button></div>
    </div>
  </header>;
}
