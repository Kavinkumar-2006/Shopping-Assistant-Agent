package com.smartcart.ai.service;

import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.ProductSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class FlipkartProductNormalizerTest {

    private FlipkartProductNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new FlipkartProductNormalizer();
    }

    @Test
    @DisplayName("Normalizes complete Flipkart product correctly")
    void testNormalizeCompleteProduct() {
        Map<String, Object> flipkartProduct = new HashMap<>();
        flipkartProduct.put("productId", "FLP12345");
        flipkartProduct.put("title", "Lenovo ThinkPad E14 Gen 5");
        flipkartProduct.put("brand", "Lenovo");
        flipkartProduct.put("category", "Laptop");
        flipkartProduct.put("sellingPrice", 58000);
        flipkartProduct.put("rating", 4.5);
        flipkartProduct.put("reviewCount", 234);
        flipkartProduct.put("description", "Powerful business laptop");
        flipkartProduct.put("imageUrl", "https://cdn.flipkart.com/image.jpg");
        flipkartProduct.put("productUrl", "https://www.flipkart.com/product/FLP12345");
        flipkartProduct.put("inStock", true);

        Product product = normalizer.normalize(flipkartProduct);

        assertNotNull(product);
        assertEquals("FLP12345", product.getId());
        assertEquals("Lenovo ThinkPad E14 Gen 5", product.getName());
        assertEquals("Lenovo", product.getBrand());
        assertEquals("laptop", product.getCategory());
        assertEquals(58000, product.getPrice());
        assertEquals(4.5, product.getRating());
        assertEquals(234, product.getReviewCount());
        assertEquals("Powerful business laptop", product.getDescription());
        assertEquals("https://cdn.flipkart.com/image.jpg", product.getImageUrl());
        assertEquals("https://www.flipkart.com/product/FLP12345", product.getProductUrl());
        assertEquals(ProductSource.FLIPKART, product.getSource());
        assertEquals("INR", product.getCurrency());
        assertTrue(product.getAvailability());
        assertEquals("Flipkart", product.getStoreName());
        assertEquals("https://www.flipkart.com", product.getSourceUrl());
        assertNotNull(product.getFetchedTime());
    }

    @Test
    @DisplayName("Returns null for null input")
    void testNormalizeNullInput() {
        Product product = normalizer.normalize(null);
        assertNull(product);
    }

    @Test
    @DisplayName("Returns null for empty map")
    void testNormalizeEmptyMap() {
        Product product = normalizer.normalize(new HashMap<>());
        assertNull(product);
    }

    @Test
    @DisplayName("Returns null when both productId and productUrl are missing")
    void testNormalizeMissingIdentifiers() {
        Map<String, Object> flipkartProduct = new HashMap<>();
        flipkartProduct.put("title", "Some Product");
        flipkartProduct.put("sellingPrice", 10000);

        Product product = normalizer.normalize(flipkartProduct);
        assertNull(product);
    }

    @Test
    @DisplayName("Generates ID from URL when productId is missing")
    void testGenerateIdFromUrl() {
        Map<String, Object> flipkartProduct = new HashMap<>();
        flipkartProduct.put("title", "Test Product");
        flipkartProduct.put("productUrl", "https://www.flipkart.com/product/xyz123");
        flipkartProduct.put("sellingPrice", 10000);

        Product product = normalizer.normalize(flipkartProduct);

        assertNotNull(product);
        assertTrue(product.getId().contains("flipkart_xyz123") || product.getId().startsWith("flipkart_"));
    }

    @Test
    @DisplayName("Extracts price from various field names")
    void testExtractPriceVariations() {
        Map<String, Object> product1 = new HashMap<>();
        product1.put("productId", "1");
        product1.put("title", "Product 1");
        product1.put("productUrl", "http://test.com/1");
        product1.put("sellingPrice", 10000);
        assertEquals(10000, normalizer.normalize(product1).getPrice());

        Map<String, Object> product2 = new HashMap<>();
        product2.put("productId", "2");
        product2.put("title", "Product 2");
        product2.put("productUrl", "http://test.com/2");
        product2.put("price", 15000);
        assertEquals(15000, normalizer.normalize(product2).getPrice());

        Map<String, Object> product3 = new HashMap<>();
        product3.put("productId", "3");
        product3.put("title", "Product 3");
        product3.put("productUrl", "http://test.com/3");
        product3.put("finalPrice", 20000);
        assertEquals(20000, normalizer.normalize(product3).getPrice());
    }

    @Test
    @DisplayName("Parses string prices correctly")
    void testParseStringPrice() {
        Map<String, Object> flipkartProduct = new HashMap<>();
        flipkartProduct.put("productId", "123");
        flipkartProduct.put("title", "Test Product");
        flipkartProduct.put("productUrl", "http://test.com");
        flipkartProduct.put("sellingPrice", "₹25,000.00");

        Product product = normalizer.normalize(flipkartProduct);
        assertEquals(25000, product.getPrice());
    }

    @Test
    @DisplayName("Extracts rating from various field names")
    void testExtractRatingVariations() {
        Map<String, Object> product1 = new HashMap<>();
        product1.put("productId", "1");
        product1.put("productUrl", "http://test.com/1");
        product1.put("title", "Product 1");
        product1.put("rating", 4.5);
        assertEquals(4.5, normalizer.normalize(product1).getRating());

        Map<String, Object> product2 = new HashMap<>();
        product2.put("productId", "2");
        product2.put("productUrl", "http://test.com/2");
        product2.put("title", "Product 2");
        product2.put("productRating", 4.7);
        assertEquals(4.7, normalizer.normalize(product2).getRating());

        Map<String, Object> product3 = new HashMap<>();
        product3.put("productId", "3");
        product3.put("productUrl", "http://test.com/3");
        product3.put("title", "Product 3");
        product3.put("starRating", "4.2");
        assertEquals(4.2, normalizer.normalize(product3).getRating());
    }

    @Test
    @DisplayName("Normalizes category to lowercase single word")
    void testNormalizeCategory() {
        Map<String, Object> flipkartProduct = new HashMap<>();
        flipkartProduct.put("productId", "123");
        flipkartProduct.put("title", "Test Product");
        flipkartProduct.put("productUrl", "http://test.com");
        flipkartProduct.put("category", "Electronics & Accessories");

        Product product = normalizer.normalize(flipkartProduct);
        assertEquals("electronics", product.getCategory());
    }

    @Test
    @DisplayName("Extracts category from categoryPath")
    void testExtractCategoryFromPath() {
        Map<String, Object> flipkartProduct = new HashMap<>();
        flipkartProduct.put("productId", "123");
        flipkartProduct.put("title", "Test Product");
        flipkartProduct.put("productUrl", "http://test.com");
        flipkartProduct.put("categoryPath", "Home > Electronics > Laptops");

        Product product = normalizer.normalize(flipkartProduct);
        assertEquals("laptops", product.getCategory());
    }

    @Test
    @DisplayName("Extracts image URL from list")
    void testExtractImageUrlFromList() {
        Map<String, Object> flipkartProduct = new HashMap<>();
        flipkartProduct.put("productId", "123");
        flipkartProduct.put("title", "Test Product");
        flipkartProduct.put("productUrl", "http://test.com");
        flipkartProduct.put("imageUrls", Arrays.asList(
                "https://cdn.flipkart.com/image1.jpg",
                "https://cdn.flipkart.com/image2.jpg"
        ));

        Product product = normalizer.normalize(flipkartProduct);
        assertEquals("https://cdn.flipkart.com/image1.jpg", product.getImageUrl());
    }

    @Test
    @DisplayName("Extracts specifications map")
    void testExtractSpecifications() {
        Map<String, Object> flipkartProduct = new HashMap<>();
        flipkartProduct.put("productId", "123");
        flipkartProduct.put("title", "Test Product");
        flipkartProduct.put("productUrl", "http://test.com");

        Map<String, String> specs = new HashMap<>();
        specs.put("RAM", "16GB");
        specs.put("Processor", "Intel i7");
        flipkartProduct.put("specifications", specs);

        Product product = normalizer.normalize(flipkartProduct);
        assertNotNull(product.getSpecs());
        assertEquals("16GB", product.getSpecs().get("RAM"));
        assertEquals("Intel i7", product.getSpecs().get("Processor"));
    }

    @Test
    @DisplayName("Extracts tags from list")
    void testExtractTags() {
        Map<String, Object> flipkartProduct = new HashMap<>();
        flipkartProduct.put("productId", "123");
        flipkartProduct.put("title", "Test Product");
        flipkartProduct.put("productUrl", "http://test.com");
        flipkartProduct.put("tags", Arrays.asList("coding", "business", "lightweight"));

        Product product = normalizer.normalize(flipkartProduct);
        assertNotNull(product.getTags());
        assertEquals(3, product.getTags().size());
        assertTrue(product.getTags().contains("coding"));
    }

    @Test
    @DisplayName("Extracts highlights from list")
    void testExtractHighlights() {
        Map<String, Object> flipkartProduct = new HashMap<>();
        flipkartProduct.put("productId", "123");
        flipkartProduct.put("title", "Test Product");
        flipkartProduct.put("productUrl", "http://test.com");
        flipkartProduct.put("highlights", Arrays.asList(
                "8GB RAM",
                "256GB SSD",
                "14 inch display"
        ));

        Product product = normalizer.normalize(flipkartProduct);
        assertNotNull(product.getHighlights());
        assertEquals(3, product.getHighlights().size());
    }

    @Test
    @DisplayName("Handles availability variations")
    void testExtractAvailability() {
        Map<String, Object> inStockProduct = new HashMap<>();
        inStockProduct.put("productId", "1");
        inStockProduct.put("title", "Product 1");
        inStockProduct.put("productUrl", "http://test.com/1");
        inStockProduct.put("inStock", true);
        assertTrue(normalizer.normalize(inStockProduct).getAvailability());

        Map<String, Object> availableProduct = new HashMap<>();
        availableProduct.put("productId", "2");
        availableProduct.put("title", "Product 2");
        availableProduct.put("productUrl", "http://test.com/2");
        availableProduct.put("availability", "In Stock");
        assertTrue(normalizer.normalize(availableProduct).getAvailability());

        Map<String, Object> outOfStockProduct = new HashMap<>();
        outOfStockProduct.put("productId", "3");
        outOfStockProduct.put("title", "Product 3");
        outOfStockProduct.put("productUrl", "http://test.com/3");
        outOfStockProduct.put("inStock", false);
        assertFalse(normalizer.normalize(outOfStockProduct).getAvailability());
    }

    @Test
    @DisplayName("Normalizes list of products correctly")
    void testNormalizeAll() {
        List<Map<String, Object>> flipkartProducts = Arrays.asList(
                createFlipkartProduct("1", "Product 1"),
                createFlipkartProduct("2", "Product 2"),
                null, // Should be filtered out
                new HashMap<>(), // Empty map, should be filtered out
                createFlipkartProduct("3", "Product 3")
        );

        List<Product> products = normalizer.normalizeAll(flipkartProducts);

        assertEquals(3, products.size());
        assertEquals("1", products.get(0).getId());
        assertEquals("2", products.get(1).getId());
        assertEquals("3", products.get(2).getId());
    }

    @Test
    @DisplayName("Returns empty list for null input")
    void testNormalizeAllNullInput() {
        List<Product> products = normalizer.normalizeAll(null);
        assertNotNull(products);
        assertTrue(products.isEmpty());
    }

    @Test
    @DisplayName("Handles malformed data gracefully")
    void testHandlesMalformedData() {
        Map<String, Object> malformedProduct = new HashMap<>();
        malformedProduct.put("productId", "123");
        malformedProduct.put("title", "Test Product");
        malformedProduct.put("productUrl", "http://test.com");
        malformedProduct.put("sellingPrice", "invalid_price");
        malformedProduct.put("rating", "not_a_number");

        Product product = normalizer.normalize(malformedProduct);

        assertNotNull(product);
        assertEquals(0, product.getPrice()); // Default when parsing fails
        assertEquals(0.0, product.getRating()); // Default when parsing fails
    }

    // Helper method
    private Map<String, Object> createFlipkartProduct(String id, String title) {
        Map<String, Object> product = new HashMap<>();
        product.put("productId", id);
        product.put("title", title);
        product.put("productUrl", "http://test.com/" + id);
        product.put("sellingPrice", 10000);
        return product;
    }
}
