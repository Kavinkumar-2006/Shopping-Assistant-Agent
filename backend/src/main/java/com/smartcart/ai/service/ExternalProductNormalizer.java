package com.smartcart.ai.service;

import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.ProductSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/**
 * Service to safely normalize dynamic external API JSON responses into the canonical Product model.
 */
@Slf4j
@Service
public class ExternalProductNormalizer {

    /**
     * Normalizes a raw key-value map into a canonical Product entity.
     *
     * @param rawMap key-value pairs from external JSON response item
     * @param defaultSource source provider identification
     * @param defaultStoreName store / retailer name
     * @return normalized Product entity or null if essential fields are missing
     */
    @SuppressWarnings("unchecked")
    public Product normalizeMap(Map<String, Object> rawMap, ProductSource defaultSource, String defaultStoreName) {
        if (rawMap == null || rawMap.isEmpty()) {
            return null;
        }

        try {
            String sourceProductId = extractString(rawMap, "id", "productId", "sku", "item_id");
            String id = sourceProductId;
            if (id == null || id.isBlank()) {
                id = "ext-" + UUID.randomUUID().toString().substring(0, 8);
            }

            String name = extractString(rawMap, "name", "title", "product_name", "productName");
            if (name == null || name.isBlank()) {
                log.warn("ExternalProductNormalizer: skipped product with missing title/name");
                return null;
            }

            String brand = extractString(rawMap, "brand", "brandName", "manufacturer", "vendor");
            if (brand == null || brand.isBlank()) {
                brand = extractBrandFromName(name);
            }

            String category = extractString(rawMap, "category", "categoryName", "department", "type");
            String description = extractString(rawMap, "description", "details", "summary", "shortDescription");

            long price = extractLong(rawMap, 0L, "price", "currentPrice", "amount", "salePrice");
            double rating = extractDouble(rawMap, 4.0, "rating", "stars", "score", "avgRating");
            int reviewCount = extractInt(rawMap, 10, "reviewCount", "reviews", "totalReviews", "ratingCount");

            String imageUrl = extractString(rawMap, "imageUrl", "image", "thumbnail", "picture");
            String productUrl = extractString(rawMap, "productUrl", "url", "link", "itemUrl");
            String sourceUrl = extractString(rawMap, "sourceUrl", "merchantUrl", "storeUrl");
            String currency = extractString(rawMap, "currency", "priceCurrency");
            if (currency == null || currency.isBlank()) currency = "INR";

            Boolean availability = extractBoolean(rawMap, true, "inStock", "available", "availability");

            Map<String, String> specs = extractMap(rawMap, "specs", "specifications", "attributes");
            List<String> tags = extractList(rawMap, "tags", "keywords", "categories");
            List<String> highlights = extractList(rawMap, "highlights", "features", "bulletPoints");

            ProductSource source = defaultSource != null ? defaultSource : ProductSource.PRODUCT_API;
            String store = defaultStoreName != null ? defaultStoreName : "External Retail Partner";

            return Product.builder()
                    .id(id)
                    .name(name)
                    .brand(brand)
                    .category(category)
                    .description(description)
                    .price(price)
                    .rating(rating)
                    .reviewCount(reviewCount)
                    .imageUrl(imageUrl)
                    .productUrl(productUrl)
                    .sourceProductId(sourceProductId)
                    .sourceProductUrl(productUrl)
                    .sourceUrl(sourceUrl)
                    .source(source)
                    .currency(currency)
                    .availability(availability)
                    .fetchedTime(Instant.now().toString())
                    .storeName(store)
                    .specs(specs != null ? specs : new HashMap<>())
                    .tags(tags != null ? tags : new ArrayList<>())
                    .highlights(highlights != null ? highlights : new ArrayList<>())
                    .build();

        } catch (Exception e) {
            log.error("ExternalProductNormalizer: failed to normalize product map — {}", e.getMessage());
            return null;
        }
    }

    private String extractString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null) {
                return val.toString();
            }
        }
        return null;
    }

    private long extractLong(Map<String, Object> map, long defaultValue, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof Number) {
                return ((Number) val).longValue();
            } else if (val != null) {
                try {
                    return Math.round(Double.parseDouble(val.toString().replaceAll("[^0-9.]", "")));
                } catch (Exception ignored) {}
            }
        }
        return defaultValue;
    }

    private double extractDouble(Map<String, Object> map, double defaultValue, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof Number) {
                return ((Number) val).doubleValue();
            } else if (val != null) {
                try {
                    return Double.parseDouble(val.toString());
                } catch (Exception ignored) {}
            }
        }
        return defaultValue;
    }

    private int extractInt(Map<String, Object> map, int defaultValue, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            } else if (val != null) {
                try {
                    return Integer.parseInt(val.toString());
                } catch (Exception ignored) {}
            }
        }
        return defaultValue;
    }

    private Boolean extractBoolean(Map<String, Object> map, boolean defaultValue, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof Boolean) {
                return (Boolean) val;
            } else if (val != null) {
                return Boolean.parseBoolean(val.toString());
            }
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractMap(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof Map) {
                Map<String, String> res = new HashMap<>();
                ((Map<?, ?>) val).forEach((k, v) -> {
                    if (k != null && v != null) res.put(k.toString(), v.toString());
                });
                return res;
            }
        }
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractList(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val instanceof List) {
                List<String> res = new ArrayList<>();
                for (Object item : (List<?>) val) {
                    if (item != null) res.add(item.toString());
                }
                return res;
            }
        }
        return new ArrayList<>();
    }

    private String extractBrandFromName(String name) {
        if (name == null || name.isBlank()) return "Generic";
        String[] parts = name.trim().split("\\s+");
        return parts[0];
    }
}
