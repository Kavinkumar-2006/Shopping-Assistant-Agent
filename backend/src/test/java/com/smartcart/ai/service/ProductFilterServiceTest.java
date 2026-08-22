package com.smartcart.ai.service;

import com.smartcart.ai.dto.ParsedQuery;
import com.smartcart.ai.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductFilterServiceTest {

    private ProductFilterService filterService;
    private List<Product> catalog;

    @BeforeEach
    void setUp() {
        filterService = new ProductFilterService();
        catalog = new ArrayList<>();

        Map<String, String> lapSpecs = new HashMap<>();
        lapSpecs.put("RAM", "16 GB");
        lapSpecs.put("Processor", "Intel i7");

        catalog.add(Product.builder()
                .id("L001")
                .name("Dell Inspiron 15")
                .brand("Dell")
                .category("laptop")
                .price(52000L)
                .rating(4.2)
                .specs(lapSpecs)
                .tags(List.of("coding", "windows"))
                .build());

        catalog.add(Product.builder()
                .id("L002")
                .name("HP Pavilion 15")
                .brand("HP")
                .category("laptop")
                .price(58000L)
                .rating(4.5)
                .tags(List.of("coding", "windows"))
                .build());

        catalog.add(Product.builder()
                .id("P001")
                .name("Samsung Galaxy S23")
                .brand("Samsung")
                .category("phone")
                .price(75000L)
                .rating(4.7)
                .tags(List.of("camera", "android"))
                .build());
    }

    @Test
    void testFilterByCategory() {
        ParsedQuery parsed = ParsedQuery.builder()
                .category("laptop")
                .build();

        List<Product> results = filterService.filter(catalog, parsed);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(p -> p.getCategory().equals("laptop")));
    }

    @Test
    void testFilterByBrand() {
        ParsedQuery parsed = ParsedQuery.builder()
                .brand("Dell")
                .build();

        List<Product> results = filterService.filter(catalog, parsed);
        assertEquals(1, results.size());
        assertEquals("Dell Inspiron 15", results.get(0).getName());
    }

    @Test
    void testFilterByPriceCeiling() {
        ParsedQuery parsed = ParsedQuery.builder()
                .budget(60000L)
                .build();

        List<Product> results = filterService.filter(catalog, parsed);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(p -> p.getPrice() <= 60000L));
    }

    @Test
    void testFilterByPriceFloor() {
        ParsedQuery parsed = ParsedQuery.builder()
                .minPrice(55000L)
                .build();

        List<Product> results = filterService.filter(catalog, parsed);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(p -> p.getPrice() >= 55000L));
    }

    @Test
    void testFilterByRating() {
        ParsedQuery parsed = ParsedQuery.builder()
                .minRating(4.3)
                .build();

        List<Product> results = filterService.filter(catalog, parsed);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(p -> p.getRating() >= 4.3));
    }

    @Test
    void testFilterByExclusion() {
        ParsedQuery parsed = ParsedQuery.builder()
                .excludedFeatures(List.of("Samsung"))
                .build();

        List<Product> results = filterService.filter(catalog, parsed);
        assertEquals(2, results.size());
        assertTrue(results.stream().noneMatch(p -> p.getBrand().equals("Samsung")));
    }

    @Test
    void testFilterByFeatures() {
        ParsedQuery parsed = ParsedQuery.builder()
                .features(List.of("Intel i7"))
                .build();

        List<Product> results = filterService.filter(catalog, parsed);
        assertEquals(1, results.size());
        assertEquals("Dell Inspiron 15", results.get(0).getName());
    }
}
