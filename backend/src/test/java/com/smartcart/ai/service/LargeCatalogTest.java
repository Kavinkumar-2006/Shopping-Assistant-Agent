package com.smartcart.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcart.ai.dto.PagedProductResult;
import com.smartcart.ai.dto.ProductDiscoveryQuery;
import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.ProductSource;
import com.smartcart.ai.entity.SortPreference;
import com.smartcart.ai.provider.LocalCatalogProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for large-scale catalog (500+ products).
 * Tests pagination, search, filtering, sorting, and deduplication.
 */
class LargeCatalogTest {

    private LocalCatalogProvider provider;
    private ProductDeduplicationService deduplicationService;
    private ProductFilterService filterService;
    private ProductRankingService rankingService;
    private ProductDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        provider = new LocalCatalogProvider(new ObjectMapper());
        provider.loadCatalog();

        deduplicationService = new ProductDeduplicationService();
        filterService = new ProductFilterService();
        ProductScoringService scoringService = new ProductScoringService();
        rankingService = new ProductRankingService(scoringService);

        discoveryService = new ProductDiscoveryService(
                List.of(provider),
                deduplicationService,
                filterService,
                rankingService
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Catalog Size & Uniqueness Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Catalog loads 500+ products")
    void testCatalogLoads500PlusProducts() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder().build();
        List<Product> allProducts = provider.discover(query);
        
        assertTrue(allProducts.size() >= 500, 
                "Catalog should contain at least 500 products, found: " + allProducts.size());
        System.out.println("✓ Catalog contains " + allProducts.size() + " products");
    }

    @Test
    @DisplayName("All product IDs are unique")
    void testAllProductIDsAreUnique() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder().build();
        List<Product> allProducts = provider.discover(query);
        
        Set<String> uniqueIds = allProducts.stream()
                .map(Product::getId)
                .collect(Collectors.toSet());
        
        assertEquals(allProducts.size(), uniqueIds.size(), 
                "All product IDs should be unique");
    }

    @Test
    @DisplayName("Catalog contains multiple categories")
    void testCatalogContainsMultipleCategories() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder().build();
        List<Product> allProducts = provider.discover(query);
        
        Set<String> categories = allProducts.stream()
                .map(Product::getCategory)
                .collect(Collectors.toSet());
        
        assertTrue(categories.size() >= 10, 
                "Catalog should contain at least 10 categories, found: " + categories);
        System.out.println("✓ Categories: " + categories);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Search Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Search by product name")
    void testSearchByProductName() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .query("Lenovo")
                .build();
        
        List<Product> results = provider.discover(query);
        
        assertFalse(results.isEmpty(), "Should find Lenovo products");
        assertTrue(results.stream().anyMatch(p -> 
                p.getName().toLowerCase().contains("lenovo") || 
                p.getBrand().equalsIgnoreCase("Lenovo")));
    }

    @Test
    @DisplayName("Search by brand")
    void testSearchByBrand() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .query("Samsung")
                .build();
        
        List<Product> results = provider.discover(query);
        
        assertFalse(results.isEmpty(), "Should find Samsung products");
        assertTrue(results.stream().anyMatch(p -> 
                p.getBrand().equalsIgnoreCase("Samsung")));
    }

    @Test
    @DisplayName("Search by category")
    void testSearchByCategory() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .query("laptop")
                .build();
        
        List<Product> results = provider.discover(query);
        
        assertFalse(results.isEmpty(), "Should find laptop products");
        assertTrue(results.stream().anyMatch(p -> 
                p.getCategory().equalsIgnoreCase("laptop")));
    }

    @Test
    @DisplayName("Case-insensitive search")
    void testCaseInsensitiveSearch() {
        // Use a common term that should exist
        ProductDiscoveryQuery queryLower = ProductDiscoveryQuery.builder()
                .query("laptop")
                .build();
        
        ProductDiscoveryQuery queryUpper = ProductDiscoveryQuery.builder()
                .query("LAPTOP")
                .build();
        
        List<Product> resultsLower = provider.discover(queryLower);
        List<Product> resultsUpper = provider.discover(queryUpper);
        
        assertFalse(resultsLower.isEmpty(), "Should find products with lowercase query");
        assertFalse(resultsUpper.isEmpty(), "Should find products with uppercase query");
        assertEquals(resultsLower.size(), resultsUpper.size(), 
                "Case-insensitive search should return same number of results");
    }

    @Test
    @DisplayName("Search by tags")
    void testSearchByTags() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .query("wireless")
                .build();
        
        List<Product> results = provider.discover(query);
        
        assertFalse(results.isEmpty(), "Should find wireless products");
        assertTrue(results.stream().anyMatch(p -> 
                p.getTags() != null && p.getTags().stream()
                        .anyMatch(tag -> tag.toLowerCase().contains("wireless"))));
    }

    @Test
    @DisplayName("Empty search returns all products")
    void testEmptySearchReturnsAllProducts() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder().build();
        List<Product> allProducts = provider.discover(query);
        
        ProductDiscoveryQuery emptyQuery = ProductDiscoveryQuery.builder()
                .query("")
                .build();
        List<Product> emptyResults = provider.discover(emptyQuery);
        
        assertEquals(allProducts.size(), emptyResults.size(), 
                "Empty search should return all products");
    }

    @Test
    @DisplayName("Unknown product search returns empty or minimal results")
    void testUnknownProductSearch() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .query("XYZ9999NonExistentProduct")
                .build();
        
        List<Product> results = provider.discover(query);
        
        // Should return empty or very few results
        assertTrue(results.size() < 5, 
                "Unknown product search should return empty or minimal results");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filtering Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Category filtering")
    void testCategoryFiltering() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .category("laptop")
                .build();
        
        List<Product> results = provider.discover(query);
        
        assertFalse(results.isEmpty(), "Should find laptop products");
        assertTrue(results.stream()
                .allMatch(p -> p.getCategory().equalsIgnoreCase("laptop")));
    }

    @Test
    @DisplayName("Price filtering - max price")
    void testMaxPriceFiltering() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .maxPrice(10000L)
                .build();
        
        List<Product> results = provider.discover(query);
        
        assertFalse(results.isEmpty(), "Should find products under 10000");
        assertTrue(results.stream()
                .allMatch(p -> p.getPrice() <= 10000));
    }

    @Test
    @DisplayName("Price filtering - min price")
    void testMinPriceFiltering() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .minPrice(50000L)
                .build();
        
        List<Product> results = provider.discover(query);
        
        assertFalse(results.isEmpty(), "Should find products above 50000");
        assertTrue(results.stream()
                .allMatch(p -> p.getPrice() >= 50000));
    }

    @Test
    @DisplayName("Price range filtering")
    void testPriceRangeFiltering() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .minPrice(30000L)
                .maxPrice(60000L)
                .build();
        
        List<Product> results = provider.discover(query);
        
        assertFalse(results.isEmpty(), "Should find products in price range");
        assertTrue(results.stream()
                .allMatch(p -> p.getPrice() >= 30000 && p.getPrice() <= 60000));
    }

    @Test
    @DisplayName("Brand filtering")
    void testBrandFiltering() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .brand("Samsung")
                .build();
        
        List<Product> results = provider.discover(query);
        
        assertFalse(results.isEmpty(), "Should find Samsung products");
        assertTrue(results.stream()
                .allMatch(p -> p.getBrand().equalsIgnoreCase("Samsung")));
    }

    @Test
    @DisplayName("Combined filters - category and price")
    void testCombinedCategoryAndPriceFilter() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .category("phone")
                .maxPrice(25000L)
                .build();
        
        List<Product> results = provider.discover(query);
        
        assertFalse(results.isEmpty(), "Should find phones under 25000");
        assertTrue(results.stream()
                .allMatch(p -> p.getCategory().equalsIgnoreCase("phone") 
                        && p.getPrice() <= 25000));
    }

    @Test
    @DisplayName("Combined filters - category, brand, and price")
    void testCombinedMultipleFilters() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .category("headphones")
                .brand("Sony")
                .minPrice(5000L)
                .maxPrice(30000L)
                .build();
        
        List<Product> results = provider.discover(query);
        
        // May or may not find results depending on catalog, but should not crash
        assertTrue(results.stream()
                .allMatch(p -> p.getCategory().equalsIgnoreCase("headphones") 
                        && p.getBrand().equalsIgnoreCase("Sony")
                        && p.getPrice() >= 5000 
                        && p.getPrice() <= 30000));
    }

    @Test
    @DisplayName("Rating filtering")
    void testRatingFiltering() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .minRating(4.5)
                .build();
        
        List<Product> results = provider.discover(query);
        
        assertFalse(results.isEmpty(), "Should find highly rated products");
        assertTrue(results.stream()
                .allMatch(p -> p.getRating() >= 4.5));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sorting Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Sort by price ascending")
    void testSortByPriceAscending() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .category("laptop")
                .sortPreference(SortPreference.PRICE_LOW_TO_HIGH)
                .build();
        
        PagedProductResult result = discoveryService.discover(query);
        List<Product> products = result.getProducts();
        
        assertFalse(products.isEmpty());
        for (int i = 0; i < products.size() - 1; i++) {
            assertTrue(products.get(i).getPrice() <= products.get(i + 1).getPrice(),
                    "Products should be sorted by price ascending");
        }
    }

    @Test
    @DisplayName("Sort by price descending")
    void testSortByPriceDescending() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .category("phone")
                .sortPreference(SortPreference.PRICE_HIGH_TO_LOW)
                .build();
        
        PagedProductResult result = discoveryService.discover(query);
        List<Product> products = result.getProducts();
        
        assertFalse(products.isEmpty());
        for (int i = 0; i < products.size() - 1; i++) {
            assertTrue(products.get(i).getPrice() >= products.get(i + 1).getPrice(),
                    "Products should be sorted by price descending");
        }
    }

    @Test
    @DisplayName("Sort by rating descending")
    void testSortByRating() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .category("headphones")
                .sortPreference(SortPreference.RATING)
                .build();
        
        PagedProductResult result = discoveryService.discover(query);
        List<Product> products = result.getProducts();
        
        assertFalse(products.isEmpty());
        for (int i = 0; i < products.size() - 1; i++) {
            assertTrue(products.get(i).getRating() >= products.get(i + 1).getRating(),
                    "Products should be sorted by rating descending");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pagination Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Pagination - page 0 (first page)")
    void testPaginationPage0() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .page(1)
                .pageSize(20)
                .build();
        
        PagedProductResult result = discoveryService.discover(query);
        
        assertEquals(1, result.getPage());
        assertEquals(20, result.getPageSize());
        assertTrue(result.getProducts().size() <= 20);
        assertTrue(result.getTotalMatches() >= 500);
        assertTrue(result.isHasNextPage());
        assertFalse(result.isHasPreviousPage());
    }

    @Test
    @DisplayName("Pagination - page 1 (second page)")
    void testPaginationPage1() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .page(2)
                .pageSize(20)
                .build();
        
        PagedProductResult result = discoveryService.discover(query);
        
        assertEquals(2, result.getPage());
        assertEquals(20, result.getPageSize());
        assertTrue(result.getProducts().size() <= 20);
        assertTrue(result.isHasPreviousPage());
    }

    @Test
    @DisplayName("Pagination - boundary (last page)")
    void testPaginationBoundary() {
        // Get total count first
        ProductDiscoveryQuery countQuery = ProductDiscoveryQuery.builder()
                .page(1)
                .pageSize(20)
                .build();
        PagedProductResult countResult = discoveryService.discover(countQuery);
        int totalPages = countResult.getTotalPages();
        
        // Request last page
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .page(totalPages)
                .pageSize(20)
                .build();
        
        PagedProductResult result = discoveryService.discover(query);
        
        assertEquals(totalPages, result.getPage());
        assertFalse(result.isHasNextPage());
        assertTrue(result.isHasPreviousPage());
    }

    @Test
    @DisplayName("Pagination - different page sizes")
    void testDifferentPageSizes() {
        int[] pageSizes = {10, 20, 50, 100};
        
        for (int pageSize : pageSizes) {
            ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                    .page(1)
                    .pageSize(pageSize)
                    .build();
            
            PagedProductResult result = discoveryService.discover(query);
            
            assertEquals(pageSize, result.getPageSize());
            assertTrue(result.getProducts().size() <= pageSize,
                    "Page size " + pageSize + " should limit results");
        }
    }

    @Test
    @DisplayName("Pagination does not return duplicate products")
    void testPaginationNoDuplicates() {
        Set<String> seenIds = new HashSet<>();
        
        for (int page = 1; page <= 5; page++) {
            ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                    .page(page)
                    .pageSize(20)
                    .build();
            
            PagedProductResult result = discoveryService.discover(query);
            
            for (Product product : result.getProducts()) {
                assertFalse(seenIds.contains(product.getId()),
                        "Product ID " + product.getId() + " appears in multiple pages");
                seenIds.add(product.getId());
            }
        }
    }

    @Test
    @DisplayName("Pagination maintains consistent order across pages")
    void testPaginationConsistentOrder() {
        ProductDiscoveryQuery query1 = ProductDiscoveryQuery.builder()
                .category("laptop")
                .sortPreference(SortPreference.PRICE_LOW_TO_HIGH)
                .page(1)
                .pageSize(10)
                .build();
        
        ProductDiscoveryQuery query2 = ProductDiscoveryQuery.builder()
                .category("laptop")
                .sortPreference(SortPreference.PRICE_LOW_TO_HIGH)
                .page(2)
                .pageSize(10)
                .build();
        
        PagedProductResult result1 = discoveryService.discover(query1);
        PagedProductResult result2 = discoveryService.discover(query2);
        
        // Last product of page 1 should have price <= first product of page 2
        if (!result1.getProducts().isEmpty() && !result2.getProducts().isEmpty()) {
            Product lastOfPage1 = result1.getProducts().get(result1.getProducts().size() - 1);
            Product firstOfPage2 = result2.getProducts().get(0);
            
            assertTrue(lastOfPage1.getPrice() <= firstOfPage2.getPrice(),
                    "Pagination order should be consistent across pages");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Deduplication Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("No duplicate results in single provider")
    void testNoDuplicateResults() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .category("laptop")
                .build();
        
        List<Product> results = provider.discover(query);
        
        Set<String> uniqueIds = results.stream()
                .map(Product::getId)
                .collect(Collectors.toSet());
        
        assertEquals(results.size(), uniqueIds.size(), 
                "Should not have duplicate products in results");
    }

    @Test
    @DisplayName("Product source metadata is set correctly")
    void testProductSourceMetadata() {
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .page(1)
                .pageSize(50)
                .build();
        
        List<Product> results = provider.discover(query);
        
        assertTrue(results.stream()
                .allMatch(p -> p.getSource() == ProductSource.LOCAL),
                "All products should have LOCAL source");
        
        assertTrue(results.stream()
                .allMatch(p -> "INR".equals(p.getCurrency())),
                "All products should have INR currency");
        
        assertTrue(results.stream()
                .allMatch(p -> p.getAvailability() != null && p.getAvailability()),
                "All products should be available");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Performance Tests
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Large catalog search completes efficiently")
    void testSearchPerformance() {
        long startTime = System.currentTimeMillis();
        
        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .query("gaming")
                .maxPrice(50000L)
                .minRating(4.0)
                .page(1)
                .pageSize(20)
                .build();
        
        PagedProductResult result = discoveryService.discover(query);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertNotNull(result);
        assertTrue(duration < 5000, 
                "Search should complete in under 5 seconds, took: " + duration + "ms");
        System.out.println("✓ Search completed in " + duration + "ms");
    }

    @Test
    @DisplayName("Catalog loads only once (not repeatedly)")
    void testCatalogLoadedOnce() {
        // If catalog wasn't loaded once and cached, this would be very slow
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10; i++) {
            ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                    .category("laptop")
                    .page(1)
                    .pageSize(10)
                    .build();
            provider.discover(query);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        assertTrue(duration < 1000, 
                "10 searches should complete quickly if catalog is cached, took: " + duration + "ms");
        System.out.println("✓ 10 searches completed in " + duration + "ms (catalog cached)");
    }
}
