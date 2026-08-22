package com.smartcart.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcart.ai.dto.PagedProductResult;
import com.smartcart.ai.dto.ProductDiscoveryQuery;
import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.ProductSource;
import com.smartcart.ai.entity.SortPreference;
import com.smartcart.ai.provider.LocalCatalogProvider;
import com.smartcart.ai.provider.ProductProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProductDiscoveryArchitectureTest {

    private LocalCatalogProvider localCatalogProvider;
    private ProductDeduplicationService deduplicationService;
    private ProductFilterService filterService;
    private ProductRankingService rankingService;
    private ProductDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        localCatalogProvider = new LocalCatalogProvider(new ObjectMapper());
        localCatalogProvider.loadCatalog();

        deduplicationService = new ProductDeduplicationService();
        filterService = new ProductFilterService();
        ProductScoringService scoringService = new ProductScoringService();
        rankingService = new ProductRankingService(scoringService);

        discoveryService = new ProductDiscoveryService(
                List.of(localCatalogProvider),
                deduplicationService,
                filterService,
                rankingService
        );
    }

    @Test
    @DisplayName("LocalCatalogProvider exposes ProductSource.LOCAL and populates source metadata")
    void testLocalCatalogProviderMetadata() {
        assertEquals(ProductSource.LOCAL, localCatalogProvider.getSource());

        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder().category("laptop").build();
        List<Product> products = localCatalogProvider.discover(query);

        assertFalse(products.isEmpty());
        for (Product p : products) {
            assertEquals(ProductSource.LOCAL, p.getSource());
            assertEquals("INR", p.getCurrency());
            assertTrue(p.getAvailability());
            assertNotNull(p.getStoreName());
            assertNotNull(p.getSourceProductId());
            assertNull(p.getSourceUrl());
            assertNull(p.getProductUrl(), "Local catalog items must not claim a retailer product link");
            assertNull(p.getSourceProductUrl());
            assertNotNull(p.getFetchedTime());
        }
    }

    @Test
    @DisplayName("ProductDeduplicationService deduplicates identical products across providers")
    void testDeduplicationService() {
        Product p1 = Product.builder()
                .id("p1")
                .name("Lenovo ThinkPad E14 Gen 5")
                .brand("Lenovo")
                .category("laptop")
                .price(58000)
                .rating(4.5)
                .source(ProductSource.LOCAL)
                .build();

        Product p2 = Product.builder()
                .id("p2")
                .name("lenovo thinkpad e14 gen 5") // same product normalized
                .brand("lenovo")
                .category("laptop")
                .price(57500)
                .rating(4.6) // higher rating
                .source(ProductSource.RETAILER_A)
                .build();

        List<Product> result = deduplicationService.deduplicate(List.of(p1, p2));
        assertEquals(1, result.size());
        assertEquals(4.6, result.get(0).getRating()); // higher rating duplicate chosen
    }

    @Test
    @DisplayName("ProductDiscoveryService supports pagination correctly")
    void testPagination() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .page(1)
                .pageSize(5)
                .build();

        PagedProductResult result = discoveryService.discover(query);

        assertEquals(1, result.getPage());
        assertEquals(5, result.getPageSize());
        assertEquals(5, result.getProducts().size());
        assertTrue(result.getTotalMatches() > 5);
        assertTrue(result.getTotalPages() >= 2);
        assertTrue(result.isHasNextPage());
        assertFalse(result.isHasPreviousPage());

        // Test Page 2
        ProductDiscoveryQuery query2 = ProductDiscoveryQuery.builder()
                .page(2)
                .pageSize(5)
                .build();
        PagedProductResult page2 = discoveryService.discover(query2);
        assertEquals(2, page2.getPage());
        assertTrue(page2.isHasPreviousPage());
    }

    @Test
    @DisplayName("ProductDiscoveryService applies sorting preferences correctly")
    void testSortingPreferences() {
        ProductDiscoveryQuery queryLowToHigh = ProductDiscoveryQuery.builder()
                .category("laptop")
                .sortPreference(SortPreference.PRICE_LOW_TO_HIGH)
                .build();

        PagedProductResult result = discoveryService.discover(queryLowToHigh);
        List<Product> products = result.getProducts();

        for (int i = 0; i < products.size() - 1; i++) {
            assertTrue(products.get(i).getPrice() <= products.get(i + 1).getPrice());
        }
    }

    @Test
    @DisplayName("ProductDiscoveryService aggregates multiple providers cleanly")
    void testMultiProviderAggregation() {
        ProductProvider mockRetailerA = new ProductProvider() {
            @Override
            public ProductSource getSource() {
                return ProductSource.RETAILER_A;
            }

            @Override
            public List<Product> discover(ProductDiscoveryQuery query) {
                return List.of(Product.builder()
                        .id("ret_a_1")
                        .name("Mock Retailer Laptop")
                        .brand("Lenovo")
                        .category("laptop")
                        .price(55000)
                        .rating(4.7)
                        .source(ProductSource.RETAILER_A)
                        .build());
            }
        };

        ProductDiscoveryService multiService = new ProductDiscoveryService(
                List.of(localCatalogProvider, mockRetailerA),
                deduplicationService,
                filterService,
                rankingService
        );

        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder().category("laptop").brand("Lenovo").build();
        PagedProductResult result = multiService.discover(query);

        assertTrue(result.getTotalMatches() >= 2);
        boolean containsRetailerA = result.getProducts().stream()
                .anyMatch(p -> p.getSource() == ProductSource.RETAILER_A);
        assertTrue(containsRetailerA, "Results must aggregate candidates from RETAILER_A provider");
    }
}
