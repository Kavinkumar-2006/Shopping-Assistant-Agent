import React, { useState, useRef, useEffect } from 'react';
import { Send } from 'lucide-react';

/** Rotating placeholder hints shown in the chat input */
const PLACEHOLDERS = [
  'Suggest a laptop under ₹60,000 for coding…',
  'Best noise-cancelling headphones under ₹15,000…',
  'Running shoes under ₹5,000 for daily use…',
  'Compare Samsung vs OnePlus phones…',
  'Best smartwatch with GPS under ₹20,000…',
  'Show me a 4K monitor under ₹30,000…',
  'Best Sony camera under ₹50,000…',
  'JBL portable speaker under ₹5,000…',
  'Mechanical keyboard for gaming under ₹3,000…',
  'Wireless mouse for office use…',
];

export default function ChatInput({ onSubmit, isLoading, initialValue }) {
  const [message, setMessage] = useState('');
  const [placeholderIdx, setPlaceholderIdx] = useState(0);
  const textareaRef = useRef(null);

  // Sync external initialValue changes (e.g., pill clicks)
  useEffect(() => {
    if (initialValue !== undefined) {
      setMessage(initialValue);
    }
  }, [initialValue]);

  // Auto-resize textarea
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = `${Math.min(textareaRef.current.scrollHeight, 180)}px`;
    }
  }, [message]);

  // Rotate placeholder every 4 seconds
  useEffect(() => {
    const id = setInterval(() => {
      setPlaceholderIdx((prev) => (prev + 1) % PLACEHOLDERS.length);
    }, 4000);
    return () => clearInterval(id);
  }, []);

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
          placeholder={PLACEHOLDERS[placeholderIdx]}
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
          ShopSmart AI — local catalog search
        </p>
        <p className="text-[10px] text-slate-400 hidden sm:block">
          Press <kbd className="font-sans font-bold border border-slate-200 px-1 py-0.5 rounded bg-slate-50 shadow-sm text-slate-500">Enter</kbd> to submit · <kbd className="font-sans font-bold border border-slate-200 px-1 py-0.5 rounded bg-slate-50 shadow-sm text-slate-500">Shift + Enter</kbd> for newline
        </p>
      </div>
    </form>
  );
}
