import axios from 'axios';

const api = axios.create({
  // In development, leave baseURL empty so Vite's /api proxy routes
  // requests to http://localhost:8080. In production deployments,
  // set VITE_API_BASE_URL to the hosted backend URL (e.g. Render).
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

export const getRecommendations = async (message) => {
  try {
    const response = await api.post('/api/chat/recommend', { message });
    return response.data;
  } catch (error) {
    console.error('API request error:', error);
    throw new Error(
      error.response?.data?.error ||
      error.response?.data?.message ||
      'Failed to connect to ShopSmart AI server.'
    );
  }
};

export const getConversationResponse = async (message, sessionId) => {
  try {
    const response = await api.post('/api/agent/session/chat', { message, sessionId });
    return response.data;
  } catch (error) {
    throw new Error(error.response?.data?.error || error.response?.data?.message || 'Failed to connect to ShopSmart AI server.');
  }
};

export const resetConversationSession = async (sessionId) => {
  if (!sessionId) return;
  await api.delete(`/api/agent/session/${sessionId}`);
};
