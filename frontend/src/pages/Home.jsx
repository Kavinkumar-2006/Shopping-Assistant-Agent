import React, { useState } from 'react';
import Header from '../components/layout/Header';
import Footer from '../components/layout/Footer';
import ChatInput from '../components/chat/ChatInput';
import ChatHistory from '../components/chat/ChatHistory';
import EmptyState from '../components/ui/EmptyState';
import LoadingState from '../components/ui/LoadingState';
import ErrorBanner from '../components/ui/ErrorBanner';
import { Trash2, AlertCircle } from 'lucide-react';
import useRecommendations from '../hooks/useRecommendations';

export default function Home() {
  const {
    messages,
    activeQuery,
    isLoading,
    error,
    setError,
    fetchRecommendations,
    clearConversation
  } = useRecommendations();

  const [inputVal, setInputVal] = useState('');

  const handleSelectSuggestion = (query) => {
    setInputVal(query);
    fetchRecommendations(query);
  };

  const handleSubmitQuery = (query) => {
    setInputVal('');
    fetchRecommendations(query);
  };

  const showEmptyState = messages.length === 0 && !activeQuery;

  return (
    <div className="flex flex-col h-screen overflow-hidden bg-slate-50">
      <Header />

      {/* Main container panel redesign */}
      <main className="flex-grow flex flex-col max-w-7xl w-full mx-auto overflow-hidden bg-white sm:border-x border-slate-200/60 relative">
        
        {/* Error notifications block */}
        {error && (
          <div className="px-6 py-4 bg-white border-b border-slate-200/50">
            <ErrorBanner message={error} onDismiss={() => setError(null)} />
          </div>
        )}

        {/* Section session title bar redesign */}
        {!showEmptyState && (
          <div className="px-6 py-4 border-b border-slate-200/50 flex items-center justify-between bg-white/80 backdrop-blur-sm z-10 shrink-0">
            <div>
              <div className="flex items-center gap-1.5">
                <span className="h-1.5 w-1.5 rounded-full bg-brand-550"></span>
                <h2 className="text-xs font-black uppercase tracking-widest text-slate-800">Shopping Session</h2>
              </div>
              <p className="text-[10px] text-slate-400 font-bold tracking-wide mt-0.5 leading-none">
                Laptops · Phones · Headphones · Running Shoes
              </p>
            </div>
            <button
              onClick={clearConversation}
              className="inline-flex items-center gap-1.5 text-xs font-bold text-slate-400 hover:text-red-500 hover:bg-red-50 px-3 py-2 rounded-xl transition-all border border-slate-100 hover:border-red-100 cursor-pointer shadow-sm active:scale-95 duration-200"
              title="Reset conversation thread"
            >
              <Trash2 className="h-4 w-4" /> Reset Thread
            </button>
          </div>
        )}

        {/* Main Conversation viewport */}
        <div className="flex-grow flex flex-col overflow-hidden relative bg-slate-50/20">
          {showEmptyState ? (
            <div className="flex-grow overflow-y-auto custom-scrollbar flex items-center justify-center">
              <EmptyState onSelectSuggestion={handleSelectSuggestion} />
            </div>
          ) : (
            <ChatHistory messages={messages} activeQuery={activeQuery} />
          )}

          {/* Typing animation layout */}
          {isLoading && (
            <div className="px-6 py-4 bg-transparent shrink-0 max-w-[85%] sm:max-w-[78%]">
              <LoadingState />
            </div>
          )}
        </div>

        {/* Prompt Input block */}
        <div className="p-4 sm:p-5 border-t border-slate-200/60 bg-white shrink-0">
          <div className="max-w-4xl mx-auto">
            <ChatInput 
              onSubmit={handleSubmitQuery} 
              isLoading={isLoading} 
              initialValue={inputVal} 
            />
          </div>
        </div>

      </main>

      <Footer />
    </div>
  );
}
