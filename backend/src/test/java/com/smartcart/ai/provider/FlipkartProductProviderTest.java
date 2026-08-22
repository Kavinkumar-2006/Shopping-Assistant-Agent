package com.smartcart.ai.provider;

import com.smartcart.ai.config.FlipkartProviderConfig;
import com.smartcart.ai.dto.ProductDiscoveryQuery;
import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.ProductSource;
import com.smartcart.ai.service.FlipkartApiClient;
import com.smartcart.ai.service.FlipkartProductNormalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlipkartProductProviderTest {

    @Mock
    private FlipkartProviderConfig config;

    @Mock
    private FlipkartApiClient apiClient;

    @Mock
    private FlipkartProductNormalizer normalizer;

    private FlipkartProductProvider provider;

    @BeforeEach
    void setUp() {
        provider = new FlipkartProductProvider(config, apiClient, normalizer);
    }

    @Test
    @DisplayName("getSource returns FLIPKART")
    void testGetSource() {
        assertEquals(ProductSource.FLIPKART, provider.getSource());
    }

    @Test
    @DisplayName("Returns empty list when provider is not configured")
    void testDiscoverWhenNotConfigured() {
        when(config.isConfigured()).thenReturn(false);

        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .query("laptop")
                .build();

        List<Product> result = provider.discover(query);

        assertTrue(result.isEmpty());
        verify(apiClient, never()).get(any(), any(), any());
    }

    @Test
    @DisplayName("Successfully discovers products from Flipkart API")
    void testSuccessfulProductDiscovery() {
        when(config.isConfigured()).thenReturn(true);

        // Mock API response
        Map<String, Object> mockResponse = new HashMap<>();
        List<Map<String, Object>> mockProducts = Arrays.asList(
                createMockFlipkartProduct("FLP1", "Lenovo Laptop", 55000),
                createMockFlipkartProduct("FLP2", "Dell Laptop", 60000)
        );
        mockResponse.put("products", mockProducts);

        when(apiClient.get(anyString(), anyMap(), eq(Map.class))).thenReturn(mockResponse);

        // Mock normalized products
        List<Product> normalizedProducts = Arrays.asList(
                createMockProduct("FLP1", "Lenovo Laptop", 55000),
                createMockProduct("FLP2", "Dell Laptop", 60000)
        );
        when(normalizer.normalizeAll(mockProducts)).thenReturn(normalizedProducts);

        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .query("laptop")
                .build();

        List<Product> result = provider.discover(query);

        assertEquals(2, result.size());
        assertEquals(ProductSource.FLIPKART, result.get(0).getSource());
        verify(apiClient).get(anyString(), anyMap(), eq(Map.class));
        verify(normalizer).normalizeAll(mockProducts);
    }

    @Test
    @DisplayName("Returns empty list when API returns null response")
    void testHandlesNullApiResponse() {
        when(config.isConfigured()).thenReturn(true);
        when(apiClient.get(anyString(), anyMap(), eq(Map.class))).thenReturn(null);

        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder().query("laptop").build();
        List<Product> result = provider.discover(query);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Returns empty list when API returns empty response")
    void testHandlesEmptyApiResponse() {
        when(config.isConfigured()).thenReturn(true);
        when(apiClient.get(anyString(), anyMap(), eq(Map.class))).thenReturn(new HashMap<>());

        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder().query("laptop").build();
        List<Product> result = provider.discover(query);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Handles API client exception gracefully")
    void testHandlesApiClientException() {
        when(config.isConfigured()).thenReturn(true);
        when(apiClient.get(anyString(), anyMap(), eq(Map.class)))
                .thenThrow(new RuntimeException("Network timeout"));

        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder().query("laptop").build();
        List<Product> result = provider.discover(query);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Builds query parameters correctly")
    void testQueryParameterBuilding() {
        when(config.isConfigured()).thenReturn(true);

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("products", Collections.emptyList());
        when(apiClient.get(anyString(), anyMap(), eq(Map.class))).thenReturn(mockResponse);

        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder()
                .query("laptop")
                .category("electronics")
                .brand("Lenovo")
                .minPrice(50000L)
                .maxPrice(70000L)
                .minRating(4.0)
                .page(2)
                .pageSize(10)
                .build();

        provider.discover(query);

        verify(apiClient).get(anyString(), argThat(params ->
                params.get("query").equals("laptop") &&
                params.get("category").equals("electronics") &&
                params.get("brand").equals("Lenovo") &&
                params.get("minPrice").equals("50000") &&
                params.get("maxPrice").equals("70000") &&
                params.get("minRating").equals("4.0") &&
                params.get("page").equals("2") &&
                params.get("resultCount").equals("10")
        ), eq(Map.class));
    }

    @Test
    @DisplayName("Extracts products from various response structures")
    void testProductExtractionFromDifferentResponseStructures() {
        when(config.isConfigured()).thenReturn(true);

        // Test with "productBaseInfoList" field
        Map<String, Object> response1 = new HashMap<>();
        List<Map<String, Object>> products1 = Collections.singletonList(
                createMockFlipkartProduct("P1", "Product 1", 10000)
        );
        response1.put("productBaseInfoList", products1);

        when(apiClient.get(anyString(), anyMap(), eq(Map.class))).thenReturn(response1);
        when(normalizer.normalizeAll(products1)).thenReturn(Collections.singletonList(
                createMockProduct("P1", "Product 1", 10000)
        ));

        List<Product> result = provider.discover(ProductDiscoveryQuery.builder().build());
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Handles null query parameter gracefully")
    void testHandlesNullQuery() {
        when(config.isConfigured()).thenReturn(true);

        Map<String, Object> mockResponse = new HashMap<>();
        mockResponse.put("products", Collections.emptyList());
        when(apiClient.get(anyString(), anyMap(), eq(Map.class))).thenReturn(mockResponse);

        List<Product> result = provider.discover(null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // Helper methods

    private Map<String, Object> createMockFlipkartProduct(String id, String title, long price) {
        Map<String, Object> product = new HashMap<>();
        product.put("productId", id);
        product.put("title", title);
        product.put("sellingPrice", price);
        product.put("productUrl", "https://www.flipkart.com/product/" + id);
        product.put("brand", "Test Brand");
        product.put("category", "electronics");
        product.put("rating", 4.5);
        product.put("reviewCount", 100);
        return product;
    }

    private Product createMockProduct(String id, String name, long price) {
        return Product.builder()
                .id(id)
                .name(name)
                .price(price)
                .brand("Test Brand")
                .category("laptop")
                .rating(4.5)
                .reviewCount(100)
                .source(ProductSource.FLIPKART)
                .currency("INR")
                .availability(true)
                .storeName("Flipkart")
                .build();
    }
}
