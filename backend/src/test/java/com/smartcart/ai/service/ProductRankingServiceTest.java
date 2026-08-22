package com.smartcart.ai.service;

import com.smartcart.ai.dto.ParsedQuery;
import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.SortPreference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductRankingServiceTest {

    private ProductRankingService rankingService;
    private List<Product> catalog;

    @BeforeEach
    void setUp() {
        ProductScoringService scoringService = new ProductScoringService();
        rankingService = new ProductRankingService(scoringService);
        catalog = new ArrayList<>();

        catalog.add(Product.builder()
                .id("L001")
                .name("Dell Inspiron 15")
                .brand("Dell")
                .price(52000L)
                .rating(4.2)
                .reviewCount(100)
                .tags(List.of("coding", "windows"))
                .build());

        catalog.add(Product.builder()
                .id("L002")
                .name("HP Pavilion 15")
                .brand("HP")
                .price(42000L)
                .rating(4.5)
                .reviewCount(500)
                .tags(List.of("coding", "windows"))
                .build());
    }

    @Test
    void testRankByPriceLowToHigh() {
        ParsedQuery parsed = ParsedQuery.builder()
                .sortPreference(SortPreference.PRICE_LOW_TO_HIGH)
                .build();

        List<Product> results = rankingService.rank(catalog, parsed);
        assertEquals("HP Pavilion 15", results.get(0).getName());
        assertEquals("Dell Inspiron 15", results.get(1).getName());
    }

    @Test
    void testRankByPriceHighToLow() {
        ParsedQuery parsed = ParsedQuery.builder()
                .sortPreference(SortPreference.PRICE_HIGH_TO_LOW)
                .build();

        List<Product> results = rankingService.rank(catalog, parsed);
        assertEquals("Dell Inspiron 15", results.get(0).getName());
        assertEquals("HP Pavilion 15", results.get(1).getName());
    }

    @Test
    void testRankByRating() {
        ParsedQuery parsed = ParsedQuery.builder()
                .sortPreference(SortPreference.RATING)
                .build();

        List<Product> results = rankingService.rank(catalog, parsed);
        assertEquals("HP Pavilion 15", results.get(0).getName());
        assertEquals("Dell Inspiron 15", results.get(1).getName());
    }

    @Test
    void testRankByRelevanceBrandMatch() {
        ParsedQuery parsed = ParsedQuery.builder()
                .brand("Dell")
                .build();

        List<Product> results = rankingService.rank(catalog, parsed);
        assertEquals("Dell Inspiron 15", results.get(0).getName());
    }
}
