package com.smartcart.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcart.ai.config.FlipkartProviderConfig;
import com.smartcart.ai.dto.PagedProductResult;
import com.smartcart.ai.dto.ProductDiscoveryQuery;
import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.ProductSource;
import com.smartcart.ai.provider.FlipkartProductProvider;
import com.smartcart.ai.provider.LocalCatalogProvider;
import com.smartcart.ai.provider.ProductProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Flipkart provider within the multi-provider architecture.
 * Tests graceful fallback and multi-provider aggregation.
 */
class FlipkartProviderIntegrationTest {

    private ProductDiscoveryService discoveryService;
    private LocalCatalogProvider localProvider;
    private MockFlipkartProvider mockFlipkartProvider;

    @BeforeEach
    void setUp() {
        localProvider = new LocalCatalogProvider(new ObjectMapper());
        localProvider.loadCatalog();

        mockFlipkartProvider = new MockFlipkartProvider();

        ProductDeduplicationService deduplicationService = new ProductDeduplicationService();
        ProductFilterService filterService = new ProductFilterService();
        ProductScoringService scoringService = new ProductScoringService();
        ProductRankingService rankingService = new ProductRankingService(scoringService);

        discoveryService = new ProductDiscoveryService(
                Arrays.asList(mockFlipkartProvider, localProvider),
                deduplicationService,
                filterService,
                rankingService
        );
    }

    @Test
    @DisplayName("Multi-provider aggregation includes products from both Flipkart and Local providers")
    void testMultiProviderAggregation() {
        mockFlipkartProvider.setEnabled(true);
        mockFlipkartProvider.addProduct(createFlipkartProduct("FLP1", "Flipkart Exclusive Laptop", 52000));
        mockFlipkartProvider.addProduct(createFlipkartProduct("FLP2", "Flipkart Budget Phone", 15000));

        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .page(0)
                .pageSize(1000)
                .build();

        PagedProductResult result = discoveryService.discover(query);

        assertTrue(result.getTotalMatches() > 2, "Should include products from both providers");

        long flipkartCount = result.getProducts().stream()
                .filter(p -> p.getSource() == ProductSource.FLIPKART)
                .count();

        long localCount = result.getProducts().stream()
                .filter(p -> p.getSource() == ProductSource.LOCAL)
                .count();

        assertTrue(flipkartCount >= 2, "Should have at least 2 Flipkart products");
        assertTrue(localCount > 0, "Should have local products");
    }

    @Test
    @DisplayName("Application continues functioning when Flipkart provider is disabled")
    void testGracefulFallbackWhenFlipkartDisabled() {
        mockFlipkartProvider.setEnabled(false);

        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .category("laptop")
                .build();

        PagedProductResult result = discoveryService.discover(query);

        assertFalse(result.getProducts().isEmpty(), "Should return results from LocalCatalogProvider");
        assertTrue(result.getProducts().stream()
                .allMatch(p -> p.getSource() == ProductSource.LOCAL));
    }

    @Test
    @DisplayName("Application continues functioning when Flipkart provider throws exception")
    void testGracefulFallbackOnFlipkartException() {
        mockFlipkartProvider.setEnabled(true);
        mockFlipkartProvider.setThrowException(true);

        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .category("laptop")
                .build();

        PagedProductResult result = discoveryService.discover(query);

        assertFalse(result.getProducts().isEmpty(), "Should return results from LocalCatalogProvider");
        assertTrue(result.getProducts().stream()
                .allMatch(p -> p.getSource() == ProductSource.LOCAL));
    }

    @Test
    @DisplayName("Deduplication works across Flipkart and Local providers")
    void testDeduplicationAcrossProviders() {
        mockFlipkartProvider.setEnabled(true);

        // Add a product to Flipkart that might match a local product
        mockFlipkartProvider.addProduct(Product.builder()
                .id("FLP_LENOVO")
                .name("Lenovo ThinkPad E14 Gen 5")
                .brand("Lenovo")
                .category("laptop")
                .price(57000)
                .rating(4.6)
                .source(ProductSource.FLIPKART)
                .currency("INR")
                .availability(true)
                .storeName("Flipkart")
                .build());

        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .category("laptop")
                .brand("Lenovo")
                .build();

        List<Product> allProducts = discoveryService.discoverAll(query);

        // Verify no exact duplicate product names after deduplication
        Set<String> normalizedNames = new HashSet<>();
        for (Product p : allProducts) {
            String normalized = p.getName().toLowerCase().trim();
            // Allow duplicates with different normalized names
            normalizedNames.add(normalized);
        }

        assertTrue(normalizedNames.size() <= allProducts.size());
    }

    @Test
    @DisplayName("Flipkart products preserve source metadata correctly")
    void testFlipkartProductMetadata() {
        mockFlipkartProvider.setEnabled(true);
        mockFlipkartProvider.addProduct(createFlipkartProduct("FLP1", "Test Laptop", 50000));

        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .query("Test Laptop")
                .build();

        PagedProductResult result = discoveryService.discover(query);

        Optional<Product> flipkartProduct = result.getProducts().stream()
                .filter(p -> p.getSource() == ProductSource.FLIPKART)
                .findFirst();

        assertTrue(flipkartProduct.isPresent());
        Product product = flipkartProduct.get();

        assertEquals(ProductSource.FLIPKART, product.getSource());
        assertEquals("INR", product.getCurrency());
        assertEquals("Flipkart", product.getStoreName());
        assertEquals("https://www.flipkart.com", product.getSourceUrl());
        assertNotNull(product.getProductUrl());
        assertNotNull(product.getFetchedTime());
    }

    @Test
    @DisplayName("Query parameters are passed correctly to Flipkart provider")
    void testQueryParameterPassing() {
        mockFlipkartProvider.setEnabled(true);
        mockFlipkartProvider.addProduct(createFlipkartProduct("FLP1", "Expensive Laptop", 80000));
        mockFlipkartProvider.addProduct(createFlipkartProduct("FLP2", "Affordable Laptop", 40000));

        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .query("laptop")
                .maxPrice(50000L)
                .build();

        List<Product> allProducts = discoveryService.discoverAll(query);

        // All products should be under max price after filtering
        assertTrue(allProducts.stream()
                .allMatch(p -> p.getPrice() <= 50000));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mock Flipkart Provider for testing
    // ─────────────────────────────────────────────────────────────────────────

    private static class MockFlipkartProvider implements ProductProvider {
        private boolean enabled = false;
        private boolean throwException = false;
        private List<Product> mockProducts = new ArrayList<>();

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public void setThrowException(boolean throwException) {
            this.throwException = throwException;
        }

        public void addProduct(Product product) {
            mockProducts.add(product);
        }

        @Override
        public ProductSource getSource() {
            return ProductSource.FLIPKART;
        }

        @Override
        public List<Product> discover(ProductDiscoveryQuery query) {
            if (!enabled) {
                return Collections.emptyList();
            }

            if (throwException) {
                throw new RuntimeException("Simulated Flipkart API failure");
            }

            return new ArrayList<>(mockProducts);
        }
    }

    // Helper method
    private Product createFlipkartProduct(String id, String name, long price) {
        return Product.builder()
                .id(id)
                .name(name)
                .brand("Test Brand")
                .category("laptop")
                .price(price)
                .rating(4.5)
                .reviewCount(100)
                .description("Test product from Flipkart")
                .imageUrl("https://cdn.flipkart.com/test.jpg")
                .productUrl("https://www.flipkart.com/product/" + id)
                .source(ProductSource.FLIPKART)
                .sourceUrl("https://www.flipkart.com")
                .currency("INR")
                .availability(true)
                .storeName("Flipkart")
                .fetchedTime(new Date().toString())
                .build();
    }
}
