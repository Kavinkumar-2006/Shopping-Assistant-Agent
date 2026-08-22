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
class ConversationalExplanationsAndClarificationsTest {

    @Autowired
    private ShoppingAgentService shoppingAgentService;

    private String sessionId;

    @BeforeEach
    void setUp() {
        sessionId = "test-session-part2-" + UUID.randomUUID();
    }

    @Test
    @DisplayName("1. Missing budget clarification question for laptop")
    void testMissingBudgetClarification() {
        RecommendationResponse res = shoppingAgentService.processConversation(sessionId, "I need a laptop");
        assertNotNull(res);
        assertTrue(res.getSummary().contains("approximate budget"));
        assertEquals("budget", res.getAwaitingInput());
        assertTrue(res.getProducts().isEmpty());
    }

    @Test
    @DisplayName("2. Missing use-case clarification question for phone")
    void testMissingUseCaseClarification() {
        RecommendationResponse res = shoppingAgentService.processConversation(sessionId, "I need a phone");
        assertNotNull(res);
        assertTrue(res.getSummary().contains("most important to you"));
        assertEquals("useCase", res.getAwaitingInput());
        assertTrue(res.getProducts().isEmpty());
    }

    @Test
    @DisplayName("3. No unnecessary clarification when complete query is provided")
    void testNoUnnecessaryClarification() {
        RecommendationResponse res = shoppingAgentService.processConversation(sessionId, "I need a Lenovo laptop under 60k for coding");
        assertNotNull(res);
        assertNull(res.getAwaitingInput());
        assertFalse(res.getProducts().isEmpty());
        assertEquals("laptop", res.getCategory());
        assertEquals(60000L, res.getBudget());
    }

    @Test
    @DisplayName("4. Natural recommendation explanation containing actual specs")
    void testNaturalRecommendationExplanation() {
        RecommendationResponse res = shoppingAgentService.processConversation(sessionId, "I need a laptop under 60k for coding");
        assertNotNull(res);
        assertFalse(res.getRecommendationReasons().isEmpty());
        String explanation = res.getRecommendationReasons().get(0);
        assertTrue(explanation.contains("RAM") || explanation.contains("SSD") || explanation.contains("processor") || explanation.contains("priced at"));
    }

    @Test
    @DisplayName("5. Context-aware result summary with top recommendation callout")
    void testContextAwareResultSummary() {
        RecommendationResponse res = shoppingAgentService.processConversation(sessionId, "I need a laptop under 60k for coding");
        assertNotNull(res);
        assertTrue(res.getSummary().contains("found"));
        assertTrue(res.getSummary().contains("top recommendation"));
    }

    @Test
    @DisplayName("6. Intelligent no-result response when requirements yield zero matches")
    void testNoResultResponse() {
        RecommendationResponse res = shoppingAgentService.processConversation(sessionId, "I need a Lenovo laptop under 10k with 64GB RAM");
        assertNotNull(res);
        assertTrue(res.getProducts().isEmpty());
        assertTrue(res.getSummary().contains("couldn't find"));
    }

    @Test
    @DisplayName("7. Alternative suggestion provided on no-result")
    void testAlternativeSuggestion() {
        RecommendationResponse res = shoppingAgentService.processConversation(sessionId, "I need a Lenovo laptop under 10k");
        assertNotNull(res);
        assertTrue(res.getProducts().isEmpty());
        assertFalse(res.getFollowUpSuggestions().isEmpty());
        assertTrue(res.getFollowUpSuggestions().stream().anyMatch(s -> s.contains("budget") || s.contains("brands")));
    }

    @Test
    @DisplayName("8. Budget modification confirmation text in response summary")
    void testBudgetModificationConfirmation() {
        shoppingAgentService.processConversation(sessionId, "I need a laptop under 60k for coding");
        RecommendationResponse res = shoppingAgentService.processConversation(sessionId, "Actually make it 70k");
        assertNotNull(res);
        assertTrue(res.getSummary().toLowerCase().contains("updated your budget") || res.getSummary().toLowerCase().contains("70,000"));
        assertEquals(70000L, res.getBudget());
    }

    @Test
    @DisplayName("9. Brand modification confirmation text in response summary")
    void testBrandModificationConfirmation() {
        shoppingAgentService.processConversation(sessionId, "I need a laptop under 60k");
        RecommendationResponse res = shoppingAgentService.processConversation(sessionId, "Only HP");
        assertNotNull(res);
        assertTrue(res.getSummary().toLowerCase().contains("hp"));
        assertTrue(res.getProducts().stream().allMatch(p -> "HP".equalsIgnoreCase(p.getBrand())));
    }

    @Test
    @DisplayName("10. Textual comparison explanation generated for comparison intent")
    void testComparisonExplanation() {
        shoppingAgentService.processConversation(sessionId, "I need a laptop under 60k");
        RecommendationResponse compareRes = shoppingAgentService.processConversation(sessionId, "Compare the first and second");
        assertNotNull(compareRes);
        assertTrue(compareRes.getSummary().contains("Comparing"));
    }

    @Test
    @DisplayName("11. Multi-turn clarification workflow")
    void testMultiTurnClarification() {
        RecommendationResponse turn1 = shoppingAgentService.processConversation(sessionId, "I need a laptop");
        assertEquals("budget", turn1.getAwaitingInput());

        RecommendationResponse turn2 = shoppingAgentService.processConversation(sessionId, "Under 60k");
        assertNull(turn2.getAwaitingInput());
        assertFalse(turn2.getProducts().isEmpty());
        assertEquals("laptop", turn2.getCategory());
        assertEquals(60000L, turn2.getBudget());
    }

    @Test
    @DisplayName("12. Existing conversation behavior remains intact")
    void testExistingConversationBehaviorRemainsIntact() {
        shoppingAgentService.processConversation(sessionId, "I need headphones under 35k");
        shoppingAgentService.processConversation(sessionId, "With ANC");
        shoppingAgentService.processConversation(sessionId, "Only Sony");
        RecommendationResponse res = shoppingAgentService.processConversation(sessionId, "Best rated");

        assertNotNull(res);
        assertEquals("headphones", res.getCategory());
        assertEquals(35000L, res.getBudget());
        assertEquals("Sony", res.getSessionContext().getBrand());
        assertFalse(res.getProducts().isEmpty());
    }
}
