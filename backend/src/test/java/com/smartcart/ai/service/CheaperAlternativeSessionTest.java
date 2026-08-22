package com.smartcart.ai.service;

import com.smartcart.ai.dto.RecommendationResponse;
import com.smartcart.ai.dto.SessionChatRequest;
import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.ShoppingIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheaperAlternativeSessionTest {

    @Autowired
    private ShoppingAgentService shoppingAgentService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TEST_SESSION_ID = "2b4840dd-8e03-4c57-af2d-b8cfb9ad808a";

    @BeforeEach
    void cleanSession() {
        shoppingAgentService.resetConversation(TEST_SESSION_ID);
    }

    @Test
    @DisplayName("Turn 1 + Turn 2: 'Show me something cheaper' returns new cheaper products and preserves sessionId")
    void testCheaperAlternativeExcludesPreviousAndReturnsCheaper() {
        // Turn 1: initial recommendation query
        RecommendationResponse turn1 = shoppingAgentService.processConversation(
                TEST_SESSION_ID,
                "Suggest a laptop under 60000 for coding"
        );

        assertNotNull(turn1);
        assertEquals(TEST_SESSION_ID, turn1.getSessionId(), "Turn 1 must return the same sessionId as requested");
        assertFalse(turn1.getProducts().isEmpty(), "Turn 1 must return products");
        assertEquals("laptop", turn1.getCategory());
        assertEquals("coding", turn1.getUseCase());

        List<String> turn1ProductIds = turn1.getProducts().stream().map(Product::getId).toList();
        long turn1MinPrice = turn1.getProducts().stream().mapToLong(Product::getPrice).min().orElse(0L);
        long turn1MaxPrice = turn1.getProducts().stream().mapToLong(Product::getPrice).max().orElse(0L);

        // Turn 2: "Show me something cheaper"
        RecommendationResponse turn2 = shoppingAgentService.processConversation(
                TEST_SESSION_ID,
                "Show me something cheaper"
        );

        assertNotNull(turn2);
        assertEquals(TEST_SESSION_ID, turn2.getSessionId(), "Turn 2 must preserve the same sessionId as requested");
        assertEquals(ShoppingIntent.CHEAPER_ALTERNATIVE, turn2.getIntent(), "Intent must be CHEAPER_ALTERNATIVE");
        assertEquals("laptop", turn2.getCategory(), "Category must be preserved as laptop");
        assertEquals("coding", turn2.getUseCase(), "Use case must be preserved as coding");

        // Verify rejectedProductIds contains all turn 1 product IDs
        assertNotNull(turn2.getSessionContext());
        List<String> rejectedIds = turn2.getSessionContext().getRejectedProductIds();
        assertNotNull(rejectedIds);
        assertTrue(rejectedIds.containsAll(turn1ProductIds),
                "rejectedProductIds must contain all previous recommendation IDs: " + turn1ProductIds);

        // Verify NONE of turn 1 products appear in turn 2
        assertFalse(turn2.getProducts().isEmpty(), "Turn 2 must return replacement cheaper products");
        List<String> turn2ProductIds = turn2.getProducts().stream().map(Product::getId).toList();
        for (String id : turn2ProductIds) {
            assertFalse(turn1ProductIds.contains(id),
                    "Product " + id + " from Turn 1 must NOT appear in Turn 2 recommendations!");
        }

        // Verify products are cheaper than the previous recommendations (or under the cheaper budget)
        for (Product p : turn2.getProducts()) {
            assertTrue(p.getPrice() <= turn1MaxPrice,
                    "Product price " + p.getPrice() + " should be cheaper than turn 1 max price " + turn1MaxPrice);
        }
    }

    @Test
    @DisplayName("REST Endpoint POST /api/agent/session/chat preserves sessionId and excludes previous products")
    void testSessionChatEndpointPreservesSessionIdAndRejectsPrevious() throws Exception {
        SessionChatRequest req1 = new SessionChatRequest();
        req1.setSessionId(TEST_SESSION_ID);
        req1.setMessage("Suggest a laptop under 60000 for coding");

        String respJson1 = mockMvc.perform(post("/api/agent/session/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        RecommendationResponse turn1 = objectMapper.readValue(respJson1, RecommendationResponse.class);
        assertEquals(TEST_SESSION_ID, turn1.getSessionId());
        assertFalse(turn1.getProducts().isEmpty());
        List<String> turn1Ids = turn1.getProducts().stream().map(Product::getId).toList();

        // Turn 2 via REST
        SessionChatRequest req2 = new SessionChatRequest();
        req2.setSessionId(TEST_SESSION_ID);
        req2.setMessage("Show me something cheaper");

        String respJson2 = mockMvc.perform(post("/api/agent/session/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        RecommendationResponse turn2 = objectMapper.readValue(respJson2, RecommendationResponse.class);
        assertEquals(TEST_SESSION_ID, turn2.getSessionId(), "Response sessionId must match request sessionId");
        assertEquals(ShoppingIntent.CHEAPER_ALTERNATIVE, turn2.getIntent());

        // Check rejected IDs
        assertNotNull(turn2.getSessionContext());
        assertThat(turn2.getSessionContext().getRejectedProductIds()).containsAll(turn1Ids);

        // Check products are disjoint
        Set<String> turn2Ids = turn2.getProducts().stream().map(Product::getId).collect(Collectors.toSet());
        for (String id : turn1Ids) {
            assertThat(turn2Ids).doesNotContain(id);
        }
    }
}
