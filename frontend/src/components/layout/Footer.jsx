import React from 'react';

export default function Footer() {
  return (
    <footer className="w-full border-t border-slate-100 bg-white py-6">
      <div className="mx-auto max-w-7xl px-4 text-center sm:px-6 lg:px-8">
        <p className="text-xs text-slate-400">
          &copy; {new Date().getFullYear()} ShopSmart AI. Built with Java Spring Boot &amp; React.
        </p>
      </div>
    </footer>
  );
}
