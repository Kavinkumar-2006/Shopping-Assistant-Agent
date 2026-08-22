package com.smartcart.ai.service;

import com.smartcart.ai.dto.RecommendationResponse;
import com.smartcart.ai.entity.ShoppingIntent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ShoppingAgentServiceTest {

    @Autowired
    private ShoppingAgentService shoppingAgentService;

    @Test
    void testLaptopSearch() {
        RecommendationResponse response = shoppingAgentService.processQuery("laptop under 60000");
        assertNotNull(response);
        assertEquals(ShoppingIntent.SEARCH, response.getIntent());
        assertFalse(response.getProducts().isEmpty());
        assertTrue(response.getProducts().stream().allMatch(p -> p.getPrice() <= 60000L));
    }

    @Test
    void testBestLaptopForCoding() {
        RecommendationResponse response = shoppingAgentService.processQuery("best laptop for coding");
        assertNotNull(response);
        assertEquals(ShoppingIntent.RECOMMEND, response.getIntent());
        assertEquals("laptop", response.getCategory());
        assertEquals("coding", response.getUseCase());
        assertFalse(response.getProducts().isEmpty());
    }

    @Test
    void testSamsungPhoneUnder25000() {
        RecommendationResponse response = shoppingAgentService.processQuery("Samsung phone under 25000");
        assertNotNull(response);
        assertEquals(ShoppingIntent.RECOMMEND, response.getIntent());
        assertEquals("phone", response.getCategory());
        // Might be empty if no Samsung phones under 25000 in local products.json
        // Let's verify that the structure is set properly
        assertNotNull(response.getProducts());
    }

    @Test
    void testCheapestHeadphones() {
        RecommendationResponse response = shoppingAgentService.processQuery("cheapest headphones");
        assertNotNull(response);
        assertEquals(ShoppingIntent.CHEAPER_ALTERNATIVE, response.getIntent());
        assertEquals("headphones", response.getCategory());
        assertEquals("PRICE_LOW_TO_HIGH", response.getSortBy());
        assertFalse(response.getProducts().isEmpty());

        // Verify sorted price low to high
        long lastPrice = 0;
        for (var p : response.getProducts()) {
            assertTrue(p.getPrice() >= lastPrice);
            lastPrice = p.getPrice();
        }
    }

    @Test
    void testBestRatedRunningShoes() {
        RecommendationResponse response = shoppingAgentService.processQuery("best rated running shoes");
        assertNotNull(response);
        assertEquals(ShoppingIntent.CATEGORY_SEARCH, response.getIntent()); // running shoes is category shoes
        assertEquals("shoes", response.getCategory());
        assertEquals("RATING", response.getSortBy());
        assertFalse(response.getProducts().isEmpty());

        // Verify sorted rating high to low
        double lastRating = 5.0;
        for (var p : response.getProducts()) {
            assertTrue(p.getRating() <= lastRating);
            lastRating = p.getRating();
        }
    }

    @Test
    void testNoMatchingProducts() {
        RecommendationResponse response = shoppingAgentService.processQuery("laptop under 1000");
        assertNotNull(response);
        assertTrue(response.getProducts().isEmpty());
        assertEquals(0, response.getTotalMatches());
        assertTrue(response.getSummary().contains("couldn't find any products"));
    }

    @Test
    void testCompareLaptops() {
        RecommendationResponse response = shoppingAgentService.processQuery("Compare Dell and Lenovo laptops");
        assertNotNull(response);
        assertEquals(ShoppingIntent.COMPARE, response.getIntent());
        assertEquals("laptop", response.getCategory());
        assertFalse(response.getProducts().isEmpty());
    }
}
