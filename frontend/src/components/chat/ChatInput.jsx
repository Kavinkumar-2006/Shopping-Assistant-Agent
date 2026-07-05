import React, { useState, useRef, useEffect } from 'react';
import { Send } from 'lucide-react';

export default function ChatInput({ onSubmit, isLoading, initialValue }) {
  const [message, setMessage] = useState('');
  const textareaRef = useRef(null);

  useEffect(() => {
    if (initialValue !== undefined) {
      setMessage(initialValue);
    }
  }, [initialValue]);

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 180)}px`;
    }
  }, [message]);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!message.trim() || isLoading) return;
    onSubmit(message.trim());
    setMessage('');
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="w-full">
      <div className="flex items-end gap-3 rounded-2xl border border-slate-200 bg-white p-3 shadow-premium transition-all duration-300 focus-within:border-brand-550 focus-within:ring-4 focus-within:ring-brand-100/20">
        <textarea
          ref={textareaRef}
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Suggest a laptop under 60000 for coding..."
          rows={1}
          disabled={isLoading}
          className="flex-grow resize-none border-0 bg-transparent py-1.5 px-2 text-sm text-slate-800 placeholder-slate-400 focus:ring-0 focus:outline-none min-h-[38px] max-h-[160px] custom-scrollbar leading-relaxed font-medium"
        />
        <button
          type="submit"
          disabled={!message.trim() || isLoading}
          className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-gradient-to-r from-brand-550 to-blue-600 text-white shadow-md shadow-brand-550/10 hover:brightness-110 active:scale-95 disabled:bg-slate-100 disabled:text-slate-300 disabled:scale-100 disabled:shadow-none transition-all cursor-pointer"
        >
          <Send className="h-4.5 w-4.5" />
        </button>
      </div>
      <div className="flex justify-between items-center px-2 mt-2">
        <p className="text-[10px] text-slate-400 font-semibold uppercase tracking-wider">
          ShopSmart local dataset search
        </p>
        <p className="text-[10px] text-slate-400 hidden sm:block">
          Press <kbd className="font-sans font-bold border border-slate-200 px-1 py-0.5 rounded bg-slate-50 shadow-sm text-slate-500">Enter</kbd> to submit · <kbd className="font-sans font-bold border border-slate-200 px-1 py-0.5 rounded bg-slate-50 shadow-sm text-slate-500">Shift + Enter</kbd> for newline
        </p>
      </div>
    </form>
  );
}
