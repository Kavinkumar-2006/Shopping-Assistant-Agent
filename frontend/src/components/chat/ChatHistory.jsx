import React, { useEffect, useRef } from 'react';
import MessageBubble from './MessageBubble';

export default function ChatHistory({ messages, activeQuery }) {
  const containerRef = useRef(null);

  // Auto-scroll to bottom of conversation whenever history changes
  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [messages, activeQuery]);

  return (
    <div 
      ref={containerRef}
      className="flex-grow overflow-y-auto px-4 py-6 space-y-6 custom-scrollbar"
    >
      {messages.map((msg, index) => (
        <MessageBubble key={index} message={msg} />
      ))}
      {activeQuery && (
        <MessageBubble 
          message={{
            sender: 'user',
            text: activeQuery
          }} 
        />
      )}
    </div>
  );
}
