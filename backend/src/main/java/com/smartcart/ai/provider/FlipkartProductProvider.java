package com.smartcart.ai.provider;

import com.smartcart.ai.config.FlipkartProviderConfig;
import com.smartcart.ai.dto.ProductDiscoveryQuery;
import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.ProductSource;
import com.smartcart.ai.service.FlipkartApiClient;
import com.smartcart.ai.service.FlipkartProductNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * ProductProvider implementation for Flipkart Affiliate API integration.
 * Provides real product data from India's largest e-commerce platform.
 *
 * <p>Gracefully handles missing credentials, API failures, and timeouts by
 * returning empty results, allowing the application to continue with LocalCatalogProvider.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlipkartProductProvider implements ProductProvider {

    private final FlipkartProviderConfig config;
    private final FlipkartApiClient apiClient;
    private final FlipkartProductNormalizer normalizer;

    @Override
    public ProductSource getSource() {
        return ProductSource.FLIPKART;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Product> discover(ProductDiscoveryQuery query) {
        if (!config.isConfigured()) {
            log.debug("FlipkartProductProvider: provider not configured or disabled — returning empty result");
            return Collections.emptyList();
        }

        try {
            log.info("FlipkartProductProvider: discovering products for query='{}', category='{}'",
                    query != null ? query.getQuery() : null,
                    query != null ? query.getCategory() : null);

            Map<String, String> queryParams = buildQueryParams(query);
            String endpoint = determineEndpoint(query);

            Map<String, Object> response = apiClient.get(endpoint, queryParams, Map.class);

            if (response == null || response.isEmpty()) {
                log.warn("FlipkartProductProvider: empty or null response from Flipkart API");
                return Collections.emptyList();
            }

            List<Map<String, Object>> products = extractProductsList(response);
            if (products.isEmpty()) {
                log.info("FlipkartProductProvider: no products found in Flipkart API response");
                return Collections.emptyList();
            }

            List<Product> normalized = normalizer.normalizeAll(products);
            log.info("FlipkartProductProvider: successfully normalized {} products from Flipkart API", normalized.size());

            return normalized;

        } catch (Exception e) {
            log.error("FlipkartProductProvider: error during product discovery — {}. Returning empty result.", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Builds Flipkart API query parameters from ProductDiscoveryQuery.
     */
    private Map<String, String> buildQueryParams(ProductDiscoveryQuery query) {
        Map<String, String> params = new HashMap<>();

        if (query == null) {
            return params;
        }

        // Flipkart API query parameter
        if (query.getQuery() != null && !query.getQuery().isBlank()) {
            params.put("query", query.getQuery());
        }

        // Category filtering
        if (query.getCategory() != null && !query.getCategory().isBlank()) {
            params.put("category", query.getCategory());
        }

        // Brand filtering
        if (query.getBrand() != null && !query.getBrand().isBlank()) {
            params.put("brand", query.getBrand());
        }

        // Price range filtering
        if (query.getMinPrice() != null) {
            params.put("minPrice", String.valueOf(query.getMinPrice()));
        }
        if (query.getMaxPrice() != null) {
            params.put("maxPrice", String.valueOf(query.getMaxPrice()));
        }

        // Rating filtering
        if (query.getMinRating() != null) {
            params.put("minRating", String.valueOf(query.getMinRating()));
        }

        // Pagination
        int page = query.getPage() > 0 ? query.getPage() : 1;
        int pageSize = query.getPageSize() > 0 ? query.getPageSize() : 20;
        params.put("page", String.valueOf(page));
        params.put("resultCount", String.valueOf(pageSize));

        return params;
    }

    /**
     * Determines the appropriate Flipkart API endpoint based on query type.
     * Flipkart offers different endpoints for search, category, and offers.
     */
    private String determineEndpoint(ProductDiscoveryQuery query) {
        // Default to product search endpoint
        // In real implementation, this would map to actual Flipkart API endpoints like:
        // /products/search, /products/category/{id}, /offers, etc.
        
        if (query != null && query.getCategory() != null && !query.getCategory().isBlank()) {
            // Category-specific search
            return "/products/category";
        }

        // General product search
        return "/products/search";
    }

    /**
     * Extracts products list from Flipkart API response.
     * Handles various possible response structures from Flipkart API.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractProductsList(Map<String, Object> response) {
        // Try common response field names
        if (response.containsKey("products") && response.get("products") instanceof List) {
            return (List<Map<String, Object>>) response.get("products");
        }

        if (response.containsKey("productBaseInfoList") && response.get("productBaseInfoList") instanceof List) {
            return (List<Map<String, Object>>) response.get("productBaseInfoList");
        }

        if (response.containsKey("items") && response.get("items") instanceof List) {
            return (List<Map<String, Object>>) response.get("items");
        }

        if (response.containsKey("productInfoList") && response.get("productInfoList") instanceof List) {
            return (List<Map<String, Object>>) response.get("productInfoList");
        }

        // If response itself is a list
        if (response.containsKey("data") && response.get("data") instanceof List) {
            return (List<Map<String, Object>>) response.get("data");
        }

        log.warn("FlipkartProductProvider: could not extract products list from response structure");
        return Collections.emptyList();
    }
}
