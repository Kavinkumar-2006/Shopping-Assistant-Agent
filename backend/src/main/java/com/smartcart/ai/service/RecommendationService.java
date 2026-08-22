package com.smartcart.ai.service;

import com.smartcart.ai.dto.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Legacy recommendation service, updated to delegate to the new orchestrator {@link ShoppingAgentService}
 * for V2 intent-based recommendations while preserving backward compatibility.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final ShoppingAgentService shoppingAgentService;

    /**
     * Delegates recommendation workflow execution to {@link ShoppingAgentService}.
     *
     * @param userMessage raw conversational query from user
     * @return RecommendationResponse containing results and structured agent details
     */
    public RecommendationResponse recommend(String userMessage) {
        return recommend(userMessage, null);
    }

    /**
     * Delegates recommendation workflow with optional session ID.
     */
    public RecommendationResponse recommend(String userMessage, String sessionId) {
        log.info("RecommendationService delegating recommend call for: '{}' (sessionId={})", userMessage, sessionId);
        if (sessionId != null && !sessionId.isBlank()) {
            return shoppingAgentService.processConversation(sessionId, userMessage);
        } else {
            return shoppingAgentService.processQuery(userMessage);
        }
    }
}

