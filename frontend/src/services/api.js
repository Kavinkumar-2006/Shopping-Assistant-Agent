import axios from 'axios';

const api = axios.create({
  baseURL: 'https://shopsmart-backend-rmqv.onrender.com',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

export const getRecommendations = async (message) => {
  try {
    const response = await api.post('/chat/recommend', { message });
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