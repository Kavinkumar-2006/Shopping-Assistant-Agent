package com.smartcart.ai.service;

import com.smartcart.ai.dto.ParsedQuery;
import com.smartcart.ai.entity.Product;
import com.smartcart.ai.util.PriceFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationExplanationServiceTest {

    private RecommendationExplanationService explanationService;

    @BeforeEach
    void setUp() {
        ProductScoringService scoringService = new ProductScoringService();
        explanationService = new RecommendationExplanationService(new PriceFormatter(), scoringService);
    }

    @Test
    void testGenerateExplanations() {
        Map<String, String> specs = new HashMap<>();
        specs.put("RAM", "16 GB");
        specs.put("Processor", "Intel i7");

        Product product = Product.builder()
                .id("L001")
                .name("Dell Inspiron 15")
                .brand("Dell")
                .price(52000L)
                .rating(4.2)
                .reviewCount(3840)
                .specs(specs)
                .tags(List.of("coding", "windows"))
                .build();

        ParsedQuery parsed = ParsedQuery.builder()
                .category("laptop")
                .budget(60000L)
                .useCase("coding")
                .build();

        List<String> explanations = explanationService.generateExplanations(List.of(product), parsed);
        assertEquals(1, explanations.size());
        String exp = explanations.get(0);

        // Product name and brand are always present
        assertTrue(exp.contains("Dell Inspiron 15"), "Should contain product name");
        assertTrue(exp.contains("Dell"), "Should contain brand");

        // Price and budget context — the new explanation mentions savings but uses different phrasing
        assertTrue(exp.contains("52,000"), "Should contain price");
        // Budget is 60000, price is 52000, diff = 8000, pct = 13% → within budget phrasing
        assertTrue(exp.contains("60,000") || exp.contains("8,000") || exp.contains("budget"),
                "Should contain budget context");

        // Rating is always present
        assertTrue(exp.contains("4.2"), "Should contain rating");

        // Specification details for laptops - new format uses different phrasing
        assertTrue(exp.contains("16 GB"), "Should mention RAM");
        assertTrue(exp.contains("Intel i7") || exp.contains("i7"), "Should mention processor");

        // Use case reasoning
        assertTrue(exp.contains("coding"), "Should mention use case");
    }
}
