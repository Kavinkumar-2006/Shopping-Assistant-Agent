import axios from 'axios';

const api = axios.create({
  baseURL: '', // Empty because Vite dev server proxy redirects '/api' to localhost:8080
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 8000,
});

/**
 * Fetches product recommendations based on a natural-language query message.
 *
 * @param {string} message The query typed by the user.
 * @returns {Promise<object>} RecommendationResponse matching the backend DTO format.
 */
export const getRecommendations = async (message) => {
  try {
    const response = await api.post('/api/chat/recommend', { message });
    return response.data;
  } catch (error) {
    console.error('API request error:', error);
    // Standardize error formats for consistent UI display
    throw new Error(
      error.response?.data?.error ||
      error.response?.data?.message ||
      'Failed to connect to ShopSmart AI server. Please make sure the backend is running on port 8080.'
    );
  }
};
