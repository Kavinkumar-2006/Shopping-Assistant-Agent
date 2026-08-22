import React, { useMemo, useState } from 'react';
import { Heart, Trash2 } from 'lucide-react';
import Header from '../components/layout/Header';
import Footer from '../components/layout/Footer';
import ChatInput from '../components/chat/ChatInput';
import ChatHistory from '../components/chat/ChatHistory';
import ShoppingContext from '../components/chat/ShoppingContext';
import EmptyState from '../components/ui/EmptyState';
import LoadingState from '../components/ui/LoadingState';
import ErrorBanner from '../components/ui/ErrorBanner';
import HistorySidebar from '../components/history/HistorySidebar';
import CompareDrawer from '../components/comparison/CompareDrawer';
import ProductDetailsDialog from '../components/products/ProductDetailsDialog';
import ProductGrid from '../components/products/ProductGrid';
import useRecommendations from '../hooks/useRecommendations';
import useLocalStorageState from '../hooks/useLocalStorageState';

const uid = () => `${Date.now()}-${Math.random().toString(36).slice(2)}`;

export default function Home() {
  const { messages, activeQuery, isLoading, error, setError, fetchRecommendations, clearConversation, shoppingContext } = useRecommendations();
  const [inputVal, setInputVal] = useState('');
  const [history, setHistory] = useLocalStorageState('shopsmart-search-history', []);
  const [wishlist, setWishlist] = useLocalStorageState('shopsmart-wishlist', []);
  const [compared, setCompared] = useState([]);
  const [detailsProduct, setDetailsProduct] = useState(null);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [activeView, setActiveView] = useState('home');
  const [lastQuery, setLastQuery] = useState('');

  const wishlistIds = useMemo(() => wishlist.map((product) => product.id), [wishlist]);
  const comparedIds = useMemo(() => compared.map((product) => product.id), [compared]);
  const showEmptyState = messages.length === 0 && !activeQuery && activeView === 'home';

  const submitQuery = async (query) => {
    if (!query?.trim()) return;
    const cleanQuery = query.trim();
    setLastQuery(cleanQuery);
    setInputVal('');
    setActiveView('home');
    setHistory((previous) => [{ id: uid(), query: cleanQuery, createdAt: new Date().toISOString() }, ...previous.filter((item) => item.query.toLowerCase() !== cleanQuery.toLowerCase())].slice(0, 20));
    await fetchRecommendations(cleanQuery);
  };

  const toggleWishlist = (product) => setWishlist((previous) => previous.some((item) => item.id === product.id) ? previous.filter((item) => item.id !== product.id) : [product, ...previous]);
  const toggleCompare = (product) => setCompared((previous) => {
    if (previous.some((item) => item.id === product.id)) return previous.filter((item) => item.id !== product.id);
    return previous.length >= 3 ? previous : [...previous, product];
  });
  const productActions = { wishlistIds, comparedIds, onToggleWishlist: toggleWishlist, onToggleCompare: toggleCompare, onViewDetails: setDetailsProduct, onFollowUp: submitQuery };
  const removeContextConstraint = (key, value) => {
    const command = {
      maxPrice: 'Forget the budget',
      category: `Forget ${value}`,
      useCase: `Forget ${value}`,
      brand: `Remove ${value}`,
      preferredOperatingSystem: `Forget ${value}`,
      feature: `Don't care about ${value}`,
    }[key];
    submitQuery(command || `Forget ${value}`);
  };

  const navigate = (view) => {
    if (view === 'history') {
      setSidebarOpen(true);
      return;
    }
    setActiveView(view === 'wishlist' ? 'wishlist' : 'home');
  };

  const emptyWishlist = <div className="mx-auto max-w-md py-20 text-center"><Heart className="mx-auto h-10 w-10 text-slate-300" /><h1 className="mt-4 text-xl font-bold text-slate-900">Your wishlist is empty</h1><p className="mt-2 text-sm text-slate-500">Save products you want to revisit and compare later.</p><button type="button" onClick={() => setActiveView('home')} className="mt-5 rounded-xl bg-slate-900 px-4 py-2.5 text-sm font-bold text-white">Explore products</button></div>;

  return <div className="flex min-h-screen flex-col bg-slate-50">
    <Header onMenu={() => setSidebarOpen(true)} onNavigate={navigate} activeView={activeView} wishlistCount={wishlist.length} compareCount={compared.length} />
    {sidebarOpen && <button type="button" className="fixed inset-0 z-40 bg-slate-950/20 lg:hidden" onClick={() => setSidebarOpen(false)} aria-label="Close history menu" />}
    <div className="mx-auto flex w-full max-w-[1600px] flex-1 overflow-hidden border-x border-slate-100 bg-white">
      <HistorySidebar history={history} open={sidebarOpen} onClose={() => setSidebarOpen(false)} onReopen={(query) => { setSidebarOpen(false); submitQuery(query); }} onDelete={(id) => setHistory((items) => items.filter((item) => item.id !== id))} onClear={() => setHistory([])} />
      <main className="relative flex min-w-0 flex-1 flex-col bg-slate-50/40">
        <ShoppingContext context={shoppingContext} onRemove={removeContextConstraint} />
        {error && <div className="p-4 sm:px-6"><ErrorBanner message={error} onDismiss={() => setError(null)} onRetry={() => submitQuery(lastQuery)} /></div>}
        <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
          {activeView === 'wishlist' ? <section className="flex-1 overflow-y-auto p-5 sm:p-8"><div className="mb-6 flex items-end justify-between"><div><p className="text-xs font-extrabold uppercase tracking-widest text-rose-500">Saved for later</p><h1 className="mt-1 text-2xl font-black text-slate-900">Your wishlist</h1></div>{wishlist.length > 0 && <button type="button" onClick={() => setWishlist([])} className="inline-flex items-center gap-2 rounded-lg px-3 py-2 text-xs font-bold text-slate-500 hover:bg-red-50 hover:text-red-600"><Trash2 className="h-3.5 w-3.5" /> Clear</button>}</div>{wishlist.length ? <ProductGrid products={wishlist} {...productActions} /> : emptyWishlist}</section> : showEmptyState ? <div className="flex-1 overflow-y-auto custom-scrollbar animate-enter"><EmptyState onSelectSuggestion={submitQuery} /></div> : <ChatHistory messages={messages} activeQuery={activeQuery} productActions={productActions} />}
          {isLoading && <div className="px-5 pb-4 sm:px-6"><LoadingState /></div>}
        </div>
        <div className="border-t border-slate-200/70 bg-white p-4 sm:p-5"><div className="mx-auto max-w-4xl"><div className="mb-2 flex justify-end">{messages.length > 0 && <button type="button" onClick={() => { clearConversation(); setCompared([]); }} className="text-xs font-bold text-slate-400 hover:text-red-600">Reset conversation</button>}</div><ChatInput onSubmit={submitQuery} isLoading={isLoading} initialValue={inputVal} /></div></div>
      </main>
    </div>
    {compared.length > 0 && <CompareDrawer products={compared} onRemove={(id) => setCompared((items) => items.filter((item) => item.id !== id))} onClear={() => setCompared([])} onClose={() => setCompared([])} onViewProduct={setDetailsProduct} />}
    <ProductDetailsDialog product={detailsProduct} onClose={() => setDetailsProduct(null)} isWishlisted={detailsProduct && wishlistIds.includes(detailsProduct.id)} isCompared={detailsProduct && comparedIds.includes(detailsProduct.id)} onToggleWishlist={toggleWishlist} onToggleCompare={toggleCompare} />
    <Footer />
  </div>;
}
