package com.smartcart.ai.service;

import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.ProductSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Normalizes Flipkart Affiliate API responses into ShopSmart's internal Product model.
 * Handles missing fields gracefully with safe defaults.
 */
@Slf4j
@Service
public class FlipkartProductNormalizer {

    /**
     * Normalizes a single Flipkart product map into a Product entity.
     *
     * @param flipkartProduct raw product map from Flipkart API
     * @return normalized Product or null if critical fields are missing
     */
    public Product normalize(Map<String, Object> flipkartProduct) {
        if (flipkartProduct == null || flipkartProduct.isEmpty()) {
            return null;
        }

        try {
            // Extract critical fields
            String productId = extractString(flipkartProduct, "productId");
            String title = extractString(flipkartProduct, "title");
            String productUrl = extractString(flipkartProduct, "productUrl");

            // Skip products without essential identifiers
            if ((productId == null || productId.isBlank()) && (productUrl == null || productUrl.isBlank())) {
                log.debug("FlipkartProductNormalizer: skipping product without valid ID or URL");
                return null;
            }

            // Build Product with safe defaults
            Product product = Product.builder()
                    .id(productId != null ? productId : generateIdFromUrl(productUrl))
                    .name(title != null ? title : "Untitled Product")
                    .brand(extractString(flipkartProduct, "brand"))
                    .category(extractCategory(flipkartProduct))
                    .price(extractPrice(flipkartProduct))
                    .rating(extractRating(flipkartProduct))
                    .reviewCount(extractReviewCount(flipkartProduct))
                    .description(extractDescription(flipkartProduct))
                    .imageUrl(extractImageUrl(flipkartProduct))
                    .productUrl(productUrl)
                    .sourceProductId(productId)
                    .sourceProductUrl(productUrl)
                    .specs(extractSpecs(flipkartProduct))
                    .tags(extractTags(flipkartProduct))
                    .highlights(extractHighlights(flipkartProduct))
                    .source(ProductSource.FLIPKART)
                    .sourceUrl("https://www.flipkart.com")
                    .currency("INR")
                    .availability(extractAvailability(flipkartProduct))
                    .storeName("Flipkart")
                    .fetchedTime(Instant.now().toString())
                    .build();

            return product;

        } catch (Exception e) {
            log.error("FlipkartProductNormalizer: error normalizing product — {}", e.getMessage());
            return null;
        }
    }

    /**
     * Normalizes a list of Flipkart products, filtering out null results.
     */
    public List<Product> normalizeAll(List<Map<String, Object>> flipkartProducts) {
        if (flipkartProducts == null) {
            return Collections.emptyList();
        }

        return flipkartProducts.stream()
                .map(this::normalize)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Field extraction helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String extractString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private String extractCategory(Map<String, Object> map) {
        String category = extractString(map, "category");
        if (category != null && !category.isBlank()) {
            // Normalize category to lowercase single word (e.g., "Laptop" -> "laptop")
            return category.toLowerCase().split("[\\s/]+")[0];
        }

        // Attempt to infer from categoryPath or productBaseInfoV1.category
        String categoryPath = extractString(map, "categoryPath");
        if (categoryPath != null) {
            String[] parts = categoryPath.split(">");
            if (parts.length > 0) {
                return parts[parts.length - 1].trim().toLowerCase();
            }
        }

        return null;
    }

    private long extractPrice(Map<String, Object> map) {
        // Try multiple price field names Flipkart might use
        Object priceObj = map.get("sellingPrice");
        if (priceObj == null) priceObj = map.get("price");
        if (priceObj == null) priceObj = map.get("finalPrice");
        if (priceObj == null) priceObj = map.get("discountedPrice");

        if (priceObj instanceof Number) {
            return ((Number) priceObj).longValue();
        }

        if (priceObj instanceof String) {
            try {
                // Remove currency symbols and commas
                String priceStr = priceObj.toString().replaceAll("[^0-9.]", "");
                return (long) Double.parseDouble(priceStr);
            } catch (NumberFormatException e) {
                log.debug("FlipkartProductNormalizer: could not parse price '{}'", priceObj);
            }
        }

        return 0;
    }

    private double extractRating(Map<String, Object> map) {
        Object ratingObj = map.get("rating");
        if (ratingObj == null) ratingObj = map.get("productRating");
        if (ratingObj == null) ratingObj = map.get("starRating");

        if (ratingObj instanceof Number) {
            return ((Number) ratingObj).doubleValue();
        }

        if (ratingObj instanceof String) {
            try {
                return Double.parseDouble(ratingObj.toString());
            } catch (NumberFormatException e) {
                log.debug("FlipkartProductNormalizer: could not parse rating '{}'", ratingObj);
            }
        }

        return 0.0;
    }

    private int extractReviewCount(Map<String, Object> map) {
        Object countObj = map.get("reviewCount");
        if (countObj == null) countObj = map.get("numReviews");
        if (countObj == null) countObj = map.get("reviews");

        if (countObj instanceof Number) {
            return ((Number) countObj).intValue();
        }

        if (countObj instanceof String) {
            try {
                String countStr = countObj.toString().replaceAll("[^0-9]", "");
                return Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                log.debug("FlipkartProductNormalizer: could not parse review count '{}'", countObj);
            }
        }

        return 0;
    }

    private String extractDescription(Map<String, Object> map) {
        String desc = extractString(map, "description");
        if (desc == null) desc = extractString(map, "productDescription");
        if (desc == null) desc = extractString(map, "shortDescription");
        return desc;
    }

    private String extractImageUrl(Map<String, Object> map) {
        String imageUrl = extractString(map, "imageUrl");
        if (imageUrl == null) imageUrl = extractString(map, "imageUrls");
        if (imageUrl == null) imageUrl = extractString(map, "productImage");
        if (imageUrl == null) imageUrl = extractString(map, "thumbnailImage");

        // Handle image URL arrays
        Object imagesObj = map.get("imageUrls");
        if (imagesObj instanceof List && !((List<?>) imagesObj).isEmpty()) {
            Object firstImage = ((List<?>) imagesObj).get(0);
            return firstImage != null ? firstImage.toString() : null;
        }

        return imageUrl;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractSpecs(Map<String, Object> map) {
        Object specsObj = map.get("specifications");
        if (specsObj == null) specsObj = map.get("specs");
        if (specsObj == null) specsObj = map.get("attributes");

        if (specsObj instanceof Map) {
            Map<String, String> specs = new HashMap<>();
            ((Map<?, ?>) specsObj).forEach((k, v) -> {
                if (k != null && v != null) {
                    specs.put(k.toString(), v.toString());
                }
            });
            return specs.isEmpty() ? null : specs;
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractTags(Map<String, Object> map) {
        Object tagsObj = map.get("tags");
        if (tagsObj == null) tagsObj = map.get("features");
        if (tagsObj == null) tagsObj = map.get("keywords");

        if (tagsObj instanceof List) {
            List<String> tags = ((List<?>) tagsObj).stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());
            return tags.isEmpty() ? null : tags;
        }

        if (tagsObj instanceof String) {
            String tagsStr = tagsObj.toString();
            if (tagsStr.contains(",")) {
                return Arrays.asList(tagsStr.split(","));
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractHighlights(Map<String, Object> map) {
        Object highlightsObj = map.get("highlights");
        if (highlightsObj == null) highlightsObj = map.get("keyFeatures");
        if (highlightsObj == null) highlightsObj = map.get("bulletPoints");

        if (highlightsObj instanceof List) {
            List<String> highlights = ((List<?>) highlightsObj).stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.toList());
            return highlights.isEmpty() ? null : highlights;
        }

        return null;
    }

    private Boolean extractAvailability(Map<String, Object> map) {
        Object availObj = map.get("inStock");
        if (availObj == null) availObj = map.get("available");
        if (availObj == null) availObj = map.get("availability");

        if (availObj instanceof Boolean) {
            return (Boolean) availObj;
        }

        if (availObj instanceof String) {
            String availStr = availObj.toString().toLowerCase();
            return availStr.contains("in stock") || availStr.contains("available");
        }

        // Default to true if availability not specified
        return true;
    }

    private String generateIdFromUrl(String url) {
        if (url == null) return UUID.randomUUID().toString();
        // Extract product ID from URL if possible (e.g., last segment)
        String[] parts = url.split("/");
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1];
            if (lastPart.length() > 0 && lastPart.length() < 100) {
                return "flipkart_" + lastPart;
            }
        }
        return "flipkart_" + UUID.randomUUID().toString();
    }
}
