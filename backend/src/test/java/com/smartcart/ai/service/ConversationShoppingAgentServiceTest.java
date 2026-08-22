package com.smartcart.ai.service;

import com.smartcart.ai.dto.RecommendationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConversationShoppingAgentServiceTest {
    @Autowired
    private ShoppingAgentService shoppingAgentService;

    @Test
    void retainsCategoryBudgetAndUseCaseAcrossTurns() {
        String session = "conversation-refinement";
        shoppingAgentService.processConversation(session, "I need a laptop");
        shoppingAgentService.processConversation(session, "under 60000");
        RecommendationResponse response = shoppingAgentService.processConversation(session, "for coding");

        assertEquals("laptop", response.getSessionContext().getCategory());
        assertEquals(60000L, response.getSessionContext().getMaxPrice());
        assertEquals("coding", response.getSessionContext().getUseCase());
        assertTrue(response.getProducts().stream().allMatch(product -> product.getPrice() <= 60000L));
    }

    @Test
    void supportsBrandRefinementAndProductReferenceComparison() {
        String session = "conversation-compare";
        shoppingAgentService.processConversation(session, "show laptops under 80000 for coding");
        RecommendationResponse refined = shoppingAgentService.processConversation(session, "only Lenovo");
        assertTrue(refined.getProducts().stream().allMatch(product -> "Lenovo".equalsIgnoreCase(product.getBrand())));

        RecommendationResponse compared = shoppingAgentService.processConversation(session, "compare the first and second");
        assertTrue(compared.getTopProducts().size() <= 2);
    }

    @Test
    void resetClearsContext() {
        String session = "conversation-reset";
        shoppingAgentService.processConversation(session, "laptop under 60000");
        shoppingAgentService.resetConversation(session);
        RecommendationResponse response = shoppingAgentService.processConversation(session, "for coding");
        assertNull(response.getSessionContext().getCategory());
        assertNotNull(response.getAwaitingInput());
    }
}
