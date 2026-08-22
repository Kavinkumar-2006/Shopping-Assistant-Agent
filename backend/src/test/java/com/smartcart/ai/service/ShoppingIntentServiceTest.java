package com.smartcart.ai.service;

import com.smartcart.ai.dto.ParsedQuery;
import com.smartcart.ai.entity.ShoppingIntent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShoppingIntentServiceTest {

    private ShoppingIntentService intentService;

    @BeforeEach
    void setUp() {
        intentService = new ShoppingIntentService();
    }

    @Test
    void testCompareIntent() {
        ParsedQuery parsed = ParsedQuery.builder().build();
        ShoppingIntent intent = intentService.detectIntent("Compare Dell and Lenovo laptops", parsed);
        assertEquals(ShoppingIntent.COMPARE, intent);

        intent = intentService.detectIntent("Dell Inspiron vs Lenovo IdeaPad", parsed);
        assertEquals(ShoppingIntent.COMPARE, intent);
    }

    @Test
    void testCheaperAlternativeIntent() {
        ParsedQuery parsed = ParsedQuery.builder().build();
        ShoppingIntent intent = intentService.detectIntent("Show me something cheaper", parsed);
        assertEquals(ShoppingIntent.CHEAPER_ALTERNATIVE, intent);

        intent = intentService.detectIntent("Suggest a less expensive phone", parsed);
        assertEquals(ShoppingIntent.CHEAPER_ALTERNATIVE, intent);
    }

    @Test
    void testPremiumAlternativeIntent() {
        ParsedQuery parsed = ParsedQuery.builder().build();
        ShoppingIntent intent = intentService.detectIntent("Suggest a better option", parsed);
        assertEquals(ShoppingIntent.PREMIUM_ALTERNATIVE, intent);

        intent = intentService.detectIntent("Show premium flagship smartphones", parsed);
        assertEquals(ShoppingIntent.PREMIUM_ALTERNATIVE, intent);
    }

    @Test
    void testBestValueIntent() {
        ParsedQuery parsed = ParsedQuery.builder().build();
        ShoppingIntent intent = intentService.detectIntent("best value laptop", parsed);
        assertEquals(ShoppingIntent.BEST_VALUE, intent);

        intent = intentService.detectIntent("affordable coding laptop", parsed);
        assertEquals(ShoppingIntent.BEST_VALUE, intent);
    }

    @Test
    void testPriceCheckIntent() {
        ParsedQuery parsed = ParsedQuery.builder().build();
        ShoppingIntent intent = intentService.detectIntent("how much is the Dell laptop?", parsed);
        assertEquals(ShoppingIntent.PRICE_CHECK, intent);

        intent = intentService.detectIntent("price of samsung s23", parsed);
        assertEquals(ShoppingIntent.PRICE_CHECK, intent);
    }

    @Test
    void testFeatureSearchIntent() {
        ParsedQuery parsed = ParsedQuery.builder()
                .features(List.of("anc"))
                .build();
        ShoppingIntent intent = intentService.detectIntent("headphones with anc", parsed);
        assertEquals(ShoppingIntent.FEATURE_SEARCH, intent);
    }

    @Test
    void testCategorySearchIntent() {
        ParsedQuery parsed = ParsedQuery.builder()
                .category("laptop")
                .build();
        ShoppingIntent intent = intentService.detectIntent("laptops", parsed);
        assertEquals(ShoppingIntent.CATEGORY_SEARCH, intent);
    }

    @Test
    void testSearchIntent() {
        ParsedQuery parsed = ParsedQuery.builder().build();
        ShoppingIntent intent = intentService.detectIntent("find me running shoes", parsed);
        assertEquals(ShoppingIntent.SEARCH, intent);
    }

    @Test
    void testRecommendIntent() {
        ParsedQuery parsed = ParsedQuery.builder().build();
        ShoppingIntent intent = intentService.detectIntent("Suggest a phone under 25000", parsed);
        assertEquals(ShoppingIntent.RECOMMEND, intent);
    }
}
