import { useState } from 'react';
import { getConversationResponse, resetConversationSession } from '../services/api';

/**
 * Custom hook for managing the state of product recommendation searches.
 */
export default function useRecommendations() {
  const [messages, setMessages] = useState([]);
  const [activeQuery, setActiveQuery] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [sessionId] = useState(() => crypto.randomUUID?.() || `${Date.now()}-${Math.random()}`);
  const [shoppingContext, setShoppingContext] = useState(null);

  /**
   * Triggers a recommendation request to the backend.
   *
   * @param {string} rawQuery The query typed or clicked by the user.
   */
  const fetchRecommendations = async (rawQuery) => {
    if (!rawQuery.trim() || isLoading) return;

    setError(null);
    setIsLoading(true);
    // Optimistically set activeQuery to display in the ChatHistory as a user message bubble
    setActiveQuery(rawQuery);

    try {
      const data = await getConversationResponse(rawQuery, sessionId);
      
      setMessages((prev) => [
        ...prev,
        {
          sender: 'user',
          text: rawQuery,
        },
        {
          sender: 'assistant',
          text: data.summary,
          category: data.category,
          budget: data.budget,
          useCase: data.useCase,
          products: data.products || [],
          topProducts: data.topProducts || [],
          appliedFilters: data.appliedFilters || [],
          recommendationReasons: data.recommendationReasons || [],
          followUpSuggestions: data.followUpSuggestions || [],
          awaitingInput: data.awaitingInput,
        },
      ]);
      setShoppingContext(data.sessionContext || null);
    } catch (err) {
      setError(err.message || 'Something went wrong while fetching recommendations.');
    } finally {
      setIsLoading(false);
      setActiveQuery(null);
    }
  };

  const clearConversation = async () => {
    setMessages([]);
    setError(null);
    setActiveQuery(null);
    setShoppingContext(null);
    try { await resetConversationSession(sessionId); } catch { /* local reset still succeeds */ }
  };

  return {
    messages,
    activeQuery,
    isLoading,
    error,
    setError,
    fetchRecommendations,
    clearConversation,
    shoppingContext
  };
}
