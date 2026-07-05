# ShopSmart AI – Shopping Assistant Agent

![ShopSmart AI Banner](https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?w=1200&auto=format&fit=crop&q=80)

## 📖 Project Overview
ShopSmart AI is an intelligent, natural-language shopping co-pilot designed to streamline the e-commerce experience. It allows users to express their shopping needs in plain English (e.g., *"Suggest a laptop under 60000 for coding"*) and instantly receive highly relevant, ranked product recommendations. The agent parses intents, budgets, and categories without relying on costly external AI APIs, using a fast, deterministic NLP engine.

## ✨ Key Features
- **Natural Language Parsing:** Automatically extracts product categories, budget constraints, and user intents (e.g., "gaming", "camera", "ANC") from conversational queries using Regex and keyword mapping.
- **Smart Ranking Engine:** Scores products dynamically based on a multi-factor algorithm considering intent matching, price efficiency, user ratings, and review popularity.
- **Premium Chat Interface:** A sleek, SaaS-style React interface featuring auto-scrolling chat histories, input helper states, and responsive design.
- **Dynamic Visualizations:** Renders top recommendations in polished product cards and aligns specifications automatically in a side-by-side comparison matrix.
- **Zero External API Cost:** Runs entirely locally on the backend, ensuring low latency and data privacy.

## 🛠️ Tech Stack
- **Frontend:** React 18, Vite, Tailwind CSS (v3), Lucide React (Icons), Axios
- **Backend:** Java 17, Spring Boot 3, Lombok, Maven
- **Architecture:** RESTful API, Client-Server model, In-memory JSON Data Store

## 🧠 Architecture Flow
1. **User Input:** The user types a natural language query in the React chat UI.
2. **API Request:** Axios sends a POST request to the Spring Boot backend (`/api/chat/recommend`).
3. **Query Parsing (`QueryParserService`):** The backend processes the text to extract `category`, `budget`, and `useCase`.
4. **Filtering & Scoring (`RecommendationService`):** The engine filters the local product catalog and scores matches using a weighted recommendation algorithm.
5. **Response Generation:** The top 5 products and top 3 comparisons are packaged into a DTO and sent back to the client.
6. **UI Rendering:** The React frontend renders the response with rich UI components (Message Bubbles, Product Cards, Comparison Tables).

## 🔌 API Endpoint Details
**POST** `/api/chat/recommend`
- **Description:** Parses the user query and returns ranked product recommendations.
- **Request Body:**
  ```json
  {
    "message": "Suggest a phone under 25000 with good camera"
  }
  ```
- **Response:**
  ```json
  {
    "summary": "I found 2 phones under ₹25,000 for camera.",
    "category": "phone",
    "budget": 25000,
    "useCase": "camera",
    "products": [ ... ],
    "topProducts": [ ... ],
    "totalMatches": 2
  }
  ```

## 🚀 How to Run

### Backend (Spring Boot)
1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Clean and run the application using Maven:
   ```bash
   mvn clean spring-boot:run
   ```
3. The API server will start on `http://localhost:8080`.

### Frontend (React + Vite)
1. Open a new terminal and navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the development server:
   ```bash
   npm run dev
   ```
4. Access the web app at `http://localhost:5173`.

## 🔮 Future Improvements
- **LLM Integration:** Integrate OpenAI or local LLMs to handle complex edge cases and conversational memory.
- **Database Integration:** Migrate from the in-memory JSON loader to a scalable PostgreSQL or MongoDB database.
- **Live E-commerce Integration:** Connect to real APIs (Amazon, Flipkart) for live pricing and stock availability.
- **User Accounts:** Add authentication to allow users to save favorite products and review chat history.
