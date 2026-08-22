package com.smartcart.ai.service;

import com.smartcart.ai.dto.RecommendationResponse;
import com.smartcart.ai.entity.ShoppingIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IntelligentConversationTest {

    @Autowired
    private ShoppingAgentService shoppingAgentService;

    private String sessionId;

    @BeforeEach
    void setUp() {
        sessionId = "test-session-" + UUID.randomUUID();
    }

    @Test
    @DisplayName("1. Initial category detection")
    void testInitialCategoryDetection() {
        RecommendationResponse res = shoppingAgentService.processConversation(sessionId, "I need a laptop under 60k");
        assertNotNull(res);
        assertEquals("laptop", res.getCategory());
        assertFalse(res.getProducts().isEmpty());
    }

    @Test
    @DisplayName("2. Budget refinement preserves category")
    void testBudgetRefinement() {
        shoppingAgentService.processConversation(sessionId, "I need a laptop");
        RecommendationResponse res = shoppingAgentService.processConversation(sessionId, "Under 60k");

        assertEquals("laptop", res.getCategory());
        assertEquals(60000L, res.getBudget());
        assertTrue(res.getProducts().stream().allMatch(p -> p.getPrice() <= 60000));
    }

    @Test
    @DisplayName("3. Use-case refinement preserves category and budget")
    void testUseCaseRefinement() {
        shoppingAgentService.processConversation(sessionId, "I need a laptop under 60k");
        RecommendationResponse res = shoppingAgentService.processConversation(sessionId, "For coding");

        assertEquals("laptop", res.getCategory());
        assertEquals(60000L, res.getBudget());
        assertEquals("coding", res.getUseCase());
    }

    @Test
    @DisplayName("4. Brand refinement preserves category, budget, and use-case")
    void testBrandRefinement() {
        shoppingAgentService.processConversation(sessionId, "I need a laptop under 60k");
        shoppingAgentService.processConversation(sessionId, "For coding");
        RecommendationResponse res = shoppingAgentService.processConversation(sessionId, "Only Lenovo");

        assertEquals("laptop", res.getCategory());
        assertEquals(60000L, res.getBudget());
        assertEquals("coding", res.getUseCase());
        assertTrue(res.getProducts().stream().allMatch(p -> "Lenovo".equalsIgnoreCase(p.getBrand())));
    }

    @Test
    @DisplayName("5. Specification refinement adds RAM requirement to active context")
    void testSpecificationRefinement() {
        shoppingAgentService.processConversation(sessionId, "I need a laptop under 60k");
        RecommendationResponse res = shoppingAgentService.processConversation(sessionId, "Something with 16GB RAM");

        assertEquals("laptop", res.getCategory());
        assertEquals(60000L, res.getBudget());
        assertNotNull(res.getSessionContext().getRequiredFeatures());
        assertTrue(res.getSessionContext().getRequiredFeatures().stream().anyMatch(f -> f.toLowerCase().contains("ram")));
    }

    @Test
    @DisplayName("6. Budget modification updates budget instead of starting new search")
    void testBudgetModification() {
        shoppingAgentService.processConversation(sessionId, "I need a laptop under 60k for coding");
        shoppingAgentService.processConversation(sessionId, "Only Lenovo");

        RecommendationResponse res = shoppingAgentService.processConversation(sessionId, "Actually make it 70k");

        assertEquals("laptop", res.getCategory());
        assertEquals("coding", res.getUseCase());
        assertEquals(70000L, res.getBudget());
        assertTrue(res.getProducts().stream().allMatch(p -> p.getPrice() <= 70000));
    }

    @Test
    @DisplayName("7. Product exclusion rejects specified product from results")
    void testProductExclusion() {
        RecommendationResponse initial = shoppingAgentService.processConversation(sessionId, "I need a laptop under 60k");
        assertFalse(initial.getProducts().isEmpty());
        String rejectedId = initial.getProducts().get(0).getId();

        RecommendationResponse updated = shoppingAgentService.processConversation(sessionId, "I don't like the first one");
        assertTrue(updated.getSessionContext().getRejectedProductIds().contains(rejectedId));
        assertTrue(updated.getProducts().stream().noneMatch(p -> p.getId().equals(rejectedId)));
    }

    @Test
    @DisplayName("8. Show another returns replacement product excluding rejected IDs")
    void testShowAnother() {
        RecommendationResponse initial = shoppingAgentService.processConversation(sessionId, "I need a laptop under 60k");
        assertFalse(initial.getProducts().isEmpty());
        String firstId = initial.getProducts().get(0).getId();

        shoppingAgentService.processConversation(sessionId, "I don't want the first one");
        RecommendationResponse replacement = shoppingAgentService.processConversation(sessionId, "Show another");

        assertNotNull(replacement);
        assertFalse(replacement.getProducts().isEmpty());
        assertTrue(replacement.getProducts().stream().noneMatch(p -> p.getId().equals(firstId)));
    }

    @Test
    @DisplayName("9. Comparison context compares products from current result set")
    void testComparisonContext() {
        shoppingAgentService.processConversation(sessionId, "I need a laptop under 60k");
        RecommendationResponse compareRes = shoppingAgentService.processConversation(sessionId, "Compare the first and third");

        assertEquals(ShoppingIntent.COMPARE_PRODUCTS, compareRes.getIntent());
        assertNotNull(compareRes.getProducts());
        assertTrue(compareRes.getProducts().size() >= 2);
    }

    @Test
    @DisplayName("10. Reset conversation clears session context completely")
    void testResetConversation() {
        shoppingAgentService.processConversation(sessionId, "I need a laptop under 60k");

        RecommendationResponse resetRes = shoppingAgentService.processConversation(sessionId, "reset conversation");
        assertEquals(0, resetRes.getProducts().size());

        RecommendationResponse newRes = shoppingAgentService.processConversation(sessionId, "Show phones under 20k");
        assertEquals("phone", newRes.getCategory());
        assertEquals(20000L, newRes.getBudget());
    }

    @Test
    @DisplayName("11. Multiple refinements in sequence")
    void testMultipleRefinementsInSequence() {
        shoppingAgentService.processConversation(sessionId, "I need a laptop");
        shoppingAgentService.processConversation(sessionId, "Under 60k");
        shoppingAgentService.processConversation(sessionId, "For coding");
        shoppingAgentService.processConversation(sessionId, "Only Lenovo");
        RecommendationResponse turn5 = shoppingAgentService.processConversation(sessionId, "Actually 70k");

        assertEquals("laptop", turn5.getCategory());
        assertEquals(70000L, turn5.getBudget());
        assertEquals("coding", turn5.getUseCase());
    }

    @Test
    @DisplayName("12. Context preservation after several turns")
    void testContextPreservationAfterSeveralTurns() {
        shoppingAgentService.processConversation(sessionId, "I need headphones under 15k");
        shoppingAgentService.processConversation(sessionId, "With ANC");
        shoppingAgentService.processConversation(sessionId, "Only Sony");
        RecommendationResponse res = shoppingAgentService.processConversation(sessionId, "Best rated");

        assertEquals("headphones", res.getCategory());
        assertEquals(15000L, res.getBudget());
        assertEquals("Sony", res.getSessionContext().getBrand());
        assertTrue(res.getProducts().stream().allMatch(p -> "Sony".equalsIgnoreCase(p.getBrand())));
    }
}
