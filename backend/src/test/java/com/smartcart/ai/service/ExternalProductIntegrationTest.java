package com.smartcart.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcart.ai.config.ExternalProductProviderConfig;
import com.smartcart.ai.dto.PagedProductResult;
import com.smartcart.ai.dto.ProductDiscoveryQuery;
import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.ProductSource;
import com.smartcart.ai.provider.ExternalProductApiProvider;
import com.smartcart.ai.provider.LocalCatalogProvider;
import com.smartcart.ai.provider.ProductProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ExternalProductIntegrationTest {

    private ExternalProductProviderConfig config;
    private ExternalProductApiClient apiClientMock;
    private ExternalProductNormalizer normalizer;
    private ExternalProductApiProvider externalProvider;
    private LocalCatalogProvider localCatalogProvider;
    private ProductDiscoveryService discoveryService;

    @BeforeEach
    void setUp() {
        config = new ExternalProductProviderConfig();
        config.setEnabled(true);
        config.setBaseUrl("https://api.mock-retailer.com/products");
        config.setApiKey("test-key-123");
        config.setTimeoutMs(2000);

        apiClientMock = Mockito.mock(ExternalProductApiClient.class);
        normalizer = new ExternalProductNormalizer();
        externalProvider = new ExternalProductApiProvider(config, apiClientMock, normalizer);

        localCatalogProvider = new LocalCatalogProvider(new ObjectMapper());
        localCatalogProvider.loadCatalog();

        ProductDeduplicationService deduplicationService = new ProductDeduplicationService();
        ProductFilterService filterService = new ProductFilterService();
        ProductScoringService scoringService = new ProductScoringService();
        ProductRankingService rankingService = new ProductRankingService(scoringService);

        discoveryService = new ProductDiscoveryService(
                List.of(localCatalogProvider, externalProvider),
                deduplicationService,
                filterService,
                rankingService
        );
    }

    @Test
    @DisplayName("External provider success: returns normalized products from API response")
    void testExternalProviderSuccess() {
        Map<String, Object> item = Map.of(
                "id", "ext-101",
                "title", "Asus ZenBook 14",
                "brand", "Asus",
                "category", "laptop",
                "price", 65000,
                "rating", 4.7
        );
        Map<String, Object> apiResponse = Map.of("products", List.of(item));

        when(apiClientMock.get(eq("https://api.mock-retailer.com/products"), any(), any(), eq(Map.class)))
                .thenReturn(apiResponse);

        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder().category("laptop").build();
        List<Product> products = externalProvider.discover(query);

        assertEquals(1, products.size());
        Product p = products.get(0);
        assertEquals("ext-101", p.getId());
        assertEquals("Asus ZenBook 14", p.getName());
        assertEquals("Asus", p.getBrand());
        assertEquals(ProductSource.PRODUCT_API, p.getSource());
        assertEquals("ext-101", p.getSourceProductId());
    }

    @Test
    @DisplayName("External provider timeout/error: handles exception cleanly returning empty list")
    void testExternalProviderTimeoutAndError() {
        when(apiClientMock.get(any(), any(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("Connection timed out after 2000ms"));

        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder().category("laptop").build();
        List<Product> products = externalProvider.discover(query);

        assertNotNull(products);
        assertTrue(products.isEmpty(), "Should return empty list on timeout without throwing uncaught exception");
    }

    @Test
    @DisplayName("Invalid external response: handles missing or malformed JSON payload safely")
    void testInvalidExternalResponse() {
        when(apiClientMock.get(any(), any(), any(), eq(Map.class)))
                .thenReturn(Map.of("unexpected_key", "no products here"));

        List<Product> products = externalProvider.discover(ProductDiscoveryQuery.builder().build());
        assertTrue(products.isEmpty());
    }

    @Test
    @DisplayName("Missing product fields: normalizer assigns defaults or skips item if title missing")
    void testMissingProductFields() {
        Map<String, Object> noTitleMap = Map.of("price", 5000);
        assertNull(normalizer.normalizeMap(noTitleMap, ProductSource.PRODUCT_API, "Test Store"));

        Map<String, Object> partialMap = Map.of("title", "Generic Headphones");
        Product p = normalizer.normalizeMap(partialMap, ProductSource.PRODUCT_API, "Test Store");
        assertNotNull(p);
        assertEquals("Generic Headphones", p.getName());
        assertEquals("Generic", p.getBrand());
        assertEquals("INR", p.getCurrency());
        assertTrue(p.getAvailability());
    }

    @Test
    @DisplayName("Provider failure with local fallback: failure in external API does not crash discovery")
    void testProviderFailureWithLocalFallback() {
        // External provider fails
        when(apiClientMock.get(any(), any(), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("500 Internal Server Error from Retailer API"));

        ProductDiscoveryQuery query = ProductDiscoveryQuery.builder().category("laptop").build();
        PagedProductResult result = discoveryService.discover(query);

        assertNotNull(result);
        assertFalse(result.getProducts().isEmpty(), "LocalCatalogProvider fallback must supply products when external API fails");
        assertTrue(result.getProducts().stream().allMatch(p -> p.getSource() == ProductSource.LOCAL));
    }

    @Test
    @DisplayName("Deduplication across providers collects multi-merchant offers")
    void testDeduplicationAcrossProvidersWithOffers() {
        Product p1 = Product.builder()
                .id("loc-1")
                .name("Dell XPS 13")
                .brand("Dell")
                .category("laptop")
                .price(95000)
                .rating(4.6)
                .storeName("Local Store")
                .source(ProductSource.LOCAL)
                .build();

        Product p2 = Product.builder()
                .id("ext-2")
                .name("dell xps 13")
                .brand("dell")
                .category("laptop")
                .price(93000)
                .rating(4.8)
                .storeName("Partner Retailer")
                .source(ProductSource.PRODUCT_API)
                .build();

        ProductDeduplicationService dedup = new ProductDeduplicationService();
        List<Product> result = dedup.deduplicate(List.of(p1, p2));

        assertEquals(1, result.size());
        Product merged = result.get(0);
        assertEquals(4.8, merged.getRating());
        assertNotNull(merged.getOffers());
        assertEquals(2, merged.getOffers().size(), "Offers list should contain details from both sellers");
    }

    @Test
    @DisplayName("Configuration: provider respects enabled/disabled flag and missing API key")
    void testConfigurationFlag() {
        ExternalProductProviderConfig disabledConfig = new ExternalProductProviderConfig();
        disabledConfig.setEnabled(false);
        assertFalse(disabledConfig.isConfigured());

        ExternalProductApiProvider disabledProvider = new ExternalProductApiProvider(disabledConfig, apiClientMock, normalizer);
        assertTrue(disabledProvider.discover(ProductDiscoveryQuery.builder().build()).isEmpty());

        ExternalProductProviderConfig noKeyConfig = new ExternalProductProviderConfig();
        noKeyConfig.setEnabled(true);
        noKeyConfig.setBaseUrl("https://api.open-products.org");
        assertTrue(noKeyConfig.isConfigured(), "Provider can run without API key if public");
    }
}
