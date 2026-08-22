package com.smartcart.ai.service;

import com.smartcart.ai.dto.RecommendationResponse;
import com.smartcart.ai.dto.ShoppingSessionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConversationalMemoryIntegrationTest {

    @Autowired
    private ShoppingAgentService shoppingAgentService;

    @Autowired
    private RecommendationService recommendationService;

    private String sessionId;

    @BeforeEach
    void setUp() {
        sessionId = "test-session-" + UUID.randomUUID();
    }

    /** Scenario 1: Category followed by budget */
    @Test
    void testCategoryFollowedByBudget() {
        shoppingAgentService.processConversation(sessionId, "I need a laptop");
        RecommendationResponse response = shoppingAgentService.processConversation(sessionId, "Under 60k");

        assertNotNull(response);
        ShoppingSessionContext context = response.getSessionContext();
        assertNotNull(context);
        assertEquals("laptop", context.getCategory());
        assertEquals(60000L, context.getMaxPrice());

        assertFalse(response.getProducts().isEmpty(), "Products list should not be empty");
        assertTrue(response.getProducts().stream().allMatch(p -> "laptop".equalsIgnoreCase(p.getCategory())), "All products must be laptops");
        assertTrue(response.getProducts().stream().allMatch(p -> p.getPrice() <= 60000L), "All products must be <= 60,000");
    }

    /** Scenario 2: Category followed by use case */
    @Test
    void testCategoryFollowedByUseCase() {
        shoppingAgentService.processConversation(sessionId, "I need a phone");
        RecommendationResponse response = shoppingAgentService.processConversation(sessionId, "For camera");

        assertNotNull(response);
        ShoppingSessionContext context = response.getSessionContext();
        assertNotNull(context);
        assertEquals("phone", context.getCategory());
        assertEquals("camera", context.getUseCase());
        assertTrue(response.getProducts().stream().allMatch(p -> "phone".equalsIgnoreCase(p.getCategory())));
    }

    /** Scenario 3: Category + budget + use case */
    @Test
    void testCategoryBudgetAndUseCase() {
        shoppingAgentService.processConversation(sessionId, "I need headphones");
        shoppingAgentService.processConversation(sessionId, "Under 10k");
        RecommendationResponse response = shoppingAgentService.processConversation(sessionId, "with ANC");

        assertNotNull(response);
        ShoppingSessionContext context = response.getSessionContext();
        assertNotNull(context);
        assertEquals("headphones", context.getCategory());
        assertEquals(10000L, context.getMaxPrice());
        assertTrue(response.getProducts().stream().allMatch(p -> "headphones".equalsIgnoreCase(p.getCategory())));
        assertTrue(response.getProducts().stream().allMatch(p -> p.getPrice() <= 10000L));
    }

    /** Scenario 4: Category + brand */
    @Test
    void testCategoryAndBrand() {
        shoppingAgentService.processConversation(sessionId, "I need a laptop");
        RecommendationResponse response = shoppingAgentService.processConversation(sessionId, "Only Lenovo");

        assertNotNull(response);
        ShoppingSessionContext context = response.getSessionContext();
        assertNotNull(context);
        assertEquals("laptop", context.getCategory());
        assertEquals("Lenovo", context.getBrand());
        assertTrue(response.getProducts().stream().allMatch(p -> "Lenovo".equalsIgnoreCase(p.getBrand())));
    }

    /** Scenario 5: Multi-turn context merging (Conversation A example) */
    @Test
    void testMultiTurnContextMerging() {
        shoppingAgentService.processConversation(sessionId, "I need a laptop");
        shoppingAgentService.processConversation(sessionId, "Under 60k");
        shoppingAgentService.processConversation(sessionId, "For coding");
        RecommendationResponse response = shoppingAgentService.processConversation(sessionId, "Only Lenovo");

        assertNotNull(response);
        ShoppingSessionContext context = response.getSessionContext();
        assertNotNull(context);
        assertEquals("laptop", context.getCategory());
        assertEquals(60000L, context.getMaxPrice());
        assertEquals("coding", context.getUseCase());
        assertEquals("Lenovo", context.getBrand());

        assertFalse(response.getProducts().isEmpty());
        assertTrue(response.getProducts().stream().allMatch(p -> "laptop".equalsIgnoreCase(p.getCategory())));
        assertTrue(response.getProducts().stream().allMatch(p -> p.getPrice() <= 60000L));
        assertTrue(response.getProducts().stream().allMatch(p -> "Lenovo".equalsIgnoreCase(p.getBrand())));
    }

    /** Scenario 6: Context override */
    @Test
    void testContextOverride() {
        shoppingAgentService.processConversation(sessionId, "I need a laptop");
        shoppingAgentService.processConversation(sessionId, "Under 60k");

        // User explicitly switches category and budget
        RecommendationResponse response = shoppingAgentService.processConversation(sessionId, "Show me phones under 20k");

        assertNotNull(response);
        ShoppingSessionContext context = response.getSessionContext();
        assertNotNull(context);
        assertEquals("phone", context.getCategory(), "Category should be overridden to phone");
        assertEquals(20000L, context.getMaxPrice(), "Max price should be overridden to 20,000");

        assertTrue(response.getProducts().stream().allMatch(p -> "phone".equalsIgnoreCase(p.getCategory())));
        assertTrue(response.getProducts().stream().allMatch(p -> p.getPrice() <= 20000L));
    }

    /** Scenario 7: Reset conversation (Conversation D example) */
    @Test
    void testResetConversation() {
        shoppingAgentService.processConversation(sessionId, "I need a laptop");
        shoppingAgentService.processConversation(sessionId, "Under 60k");

        // Clear context explicitly
        shoppingAgentService.processConversation(sessionId, "Reset conversation");

        // New request after reset
        RecommendationResponse response = shoppingAgentService.processConversation(sessionId, "Show me phones under 20k");

        assertNotNull(response);
        ShoppingSessionContext context = response.getSessionContext();
        assertNotNull(context);
        assertEquals("phone", context.getCategory(), "Must NOT retain laptop context");
        assertEquals(20000L, context.getMaxPrice());
        assertTrue(response.getProducts().stream().allMatch(p -> "phone".equalsIgnoreCase(p.getCategory())));
    }

    /** Scenario 8: Independent sessions not sharing context */
    @Test
    void testIndependentSessionsDoNotShareContext() {
        String sessionA = "session-A-" + UUID.randomUUID();
        String sessionB = "session-B-" + UUID.randomUUID();

        shoppingAgentService.processConversation(sessionA, "I need a laptop");
        shoppingAgentService.processConversation(sessionB, "I need a phone");

        RecommendationResponse respA = shoppingAgentService.processConversation(sessionA, "Under 60k");
        RecommendationResponse respB = shoppingAgentService.processConversation(sessionB, "Under 20k");

        assertEquals("laptop", respA.getSessionContext().getCategory());
        assertEquals(60000L, respA.getSessionContext().getMaxPrice());

        assertEquals("phone", respB.getSessionContext().getCategory());
        assertEquals(20000L, respB.getSessionContext().getMaxPrice());
    }

    /** Scenario 9: Legacy endpoint compatibility */
    @Test
    void testLegacyEndpointCompatibility() {
        RecommendationResponse response = recommendationService.recommend("laptop under 60000");
        assertNotNull(response);
        assertEquals("laptop", response.getCategory());
        assertEquals(60000L, response.getBudget());
        assertFalse(response.getProducts().isEmpty());
    }
}
