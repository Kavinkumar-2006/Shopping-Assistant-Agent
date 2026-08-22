package com.smartcart.ai.provider;

import com.smartcart.ai.config.ExternalProductProviderConfig;
import com.smartcart.ai.dto.ProductDiscoveryQuery;
import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.ProductSource;
import com.smartcart.ai.service.ExternalProductApiClient;
import com.smartcart.ai.service.ExternalProductNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Concrete ProductProvider implementation for integrating generic external product APIs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalProductApiProvider implements ProductProvider {

    private final ExternalProductProviderConfig config;
    private final ExternalProductApiClient apiClient;
    private final ExternalProductNormalizer normalizer;

    @Override
    public ProductSource getSource() {
        return ProductSource.PRODUCT_API;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Product> discover(ProductDiscoveryQuery query) {
        if (!config.isConfigured()) {
            log.debug("ExternalProductApiProvider: disabled or unconfigured — returning empty candidate list");
            return Collections.emptyList();
        }

        try {
            Map<String, String> queryParams = buildQueryParams(query);
            Map<String, String> headers = new HashMap<>();
            if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
                headers.put("Authorization", "Bearer " + config.getApiKey());
                headers.put("X-API-Key", config.getApiKey());
            }

            Map<String, Object> response = apiClient.get(
                    config.getBaseUrl(),
                    queryParams,
                    headers,
                    Map.class
            );

            if (response == null || response.isEmpty()) {
                log.warn("ExternalProductApiProvider: empty or null response received from external API");
                return Collections.emptyList();
            }

            List<Map<String, Object>> items = extractItemsList(response);
            if (items.isEmpty()) {
                return Collections.emptyList();
            }

            List<Product> normalizedProducts = new ArrayList<>();
            for (Map<String, Object> item : items) {
                Product p = normalizer.normalizeMap(item, getSource(), "External API Merchant");
                if (p != null) {
                    normalizedProducts.add(p);
                }
            }

            log.info("ExternalProductApiProvider: successfully discovered {} normalized products from external API",
                    normalizedProducts.size());
            return normalizedProducts;

        } catch (Exception e) {
            log.error("ExternalProductApiProvider: error during product discovery — {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Translates ProductDiscoveryQuery into external API query parameters cleanly.
     */
    private Map<String, String> buildQueryParams(ProductDiscoveryQuery query) {
        Map<String, String> params = new HashMap<>();
        if (query == null) return params;

        if (query.getQuery() != null && !query.getQuery().isBlank()) {
            params.put("q", query.getQuery());
        }
        if (query.getCategory() != null && !query.getCategory().isBlank()) {
            params.put("category", query.getCategory());
        }
        if (query.getBrand() != null && !query.getBrand().isBlank()) {
            params.put("brand", query.getBrand());
        }
        if (query.getMaxPrice() != null) {
            params.put("maxPrice", String.valueOf(query.getMaxPrice()));
        }
        if (query.getMinPrice() != null) {
            params.put("minPrice", String.valueOf(query.getMinPrice()));
        }
        if (query.getMinRating() != null) {
            params.put("minRating", String.valueOf(query.getMinRating()));
        }

        params.put("page", String.valueOf(query.getPage() > 0 ? query.getPage() : 1));
        params.put("limit", String.valueOf(config.getPageSize() > 0 ? config.getPageSize() : 20));

        return params;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractItemsList(Map<String, Object> response) {
        if (response.containsKey("products") && response.get("products") instanceof List) {
            return (List<Map<String, Object>>) response.get("products");
        }
        if (response.containsKey("items") && response.get("items") instanceof List) {
            return (List<Map<String, Object>>) response.get("items");
        }
        if (response.containsKey("data") && response.get("data") instanceof List) {
            return (List<Map<String, Object>>) response.get("data");
        }
        if (response.containsKey("results") && response.get("results") instanceof List) {
            return (List<Map<String, Object>>) response.get("results");
        }
        return Collections.emptyList();
    }
}
