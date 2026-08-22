package com.smartcart.ai.dto;

import com.smartcart.ai.entity.SortPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Normalized query DTO sent to ProductDiscoveryService and ProductProviders.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductDiscoveryQuery {

    /** Free-text search term */
    private String query;

    /** Category filter (e.g. "laptop", "phone") */
    private String category;

    /** Brand filter (e.g. "Lenovo", "Dell", "Apple") */
    private String brand;

    /** Minimum price constraint in ₹ */
    private Long minPrice;

    /** Maximum budget ceiling in ₹ */
    private Long maxPrice;

    /** Minimum rating threshold (0.0 to 5.0) */
    private Double minRating;

    /** Required feature list */
    private List<String> features;

    /** Excluded features or brands */
    private List<String> excludedFeatures;

    /** Sorting parameter preference */
    private SortPreference sortPreference;

    /** Page index (1-based, default 1) */
    @Builder.Default
    private int page = 1;

    /** Page size (default 20) */
    @Builder.Default
    private int pageSize = 20;

    /**
     * Helper method to convert ParsedQuery into ProductDiscoveryQuery.
     */
    public static ProductDiscoveryQuery fromParsedQuery(ParsedQuery parsed, int page, int pageSize) {
        if (parsed == null) {
            return ProductDiscoveryQuery.builder().page(page).pageSize(pageSize).build();
        }
        return ProductDiscoveryQuery.builder()
                .query(parsed.getOriginalQuery())
                .category(parsed.getCategory())
                .brand(parsed.getBrand())
                .minPrice(parsed.getMinPrice())
                .maxPrice(parsed.getBudget() != null ? parsed.getBudget() : parsed.getMaxPrice())
                .minRating(parsed.getMinRating())
                .features(parsed.getFeatures())
                .excludedFeatures(parsed.getExcludedFeatures())
                .sortPreference(parsed.getSortPreference())
                .page(page > 0 ? page : 1)
                .pageSize(pageSize > 0 ? pageSize : 20)
                .build();
    }
}
