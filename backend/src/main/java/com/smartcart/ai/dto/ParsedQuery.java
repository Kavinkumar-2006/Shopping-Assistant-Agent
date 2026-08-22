package com.smartcart.ai.dto;

import com.smartcart.ai.entity.ShoppingIntent;
import com.smartcart.ai.entity.SortPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Internal DTO produced by QueryParserService.
 *
 * Represents the fully structured interpretation of a raw user query.
 * All fields are nullable — null means "not detected / not specified".
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ParsedQuery {

    /**
     * Normalised product category.
     * Possible values: "laptop" | "phone" | "headphones" | "shoes" | "tablet" | "smartwatch"
     * Null if no category was detected.
     */
    private String category;

    /**
     * Maximum budget ceiling in Indian Rupees (₹).
     * Null if no budget was mentioned in the query.
     */
    private Long budget;

    /**
     * Primary use-case — the single most dominant intent in the query.
     */
    private String useCase;

    /**
     * All matched use-case / feature keywords for multi-dimensional scoring.
     */
    private List<String> keywords;

    /** Original raw query string — used for logging and summary generation. */
    private String originalQuery;

    // ── V2 AI Shopping Agent Attributes ──────────────────────────────────────

    /** Detected product brand (e.g. "Dell", "HP", "Samsung") */
    private String brand;

    /** Minimum price floor constraint in ₹ */
    private Long minPrice;

    /** Maximum price ceiling constraint in ₹ (aliases to budget) */
    private Long maxPrice;

    /** List of positive feature requirements (e.g. "anc", "5g", "ssd") */
    private List<String> features;

    /** List of negative feature / brand exclusions (e.g. "no anc", "no apple") */
    private List<String> excludedFeatures;

    /** Minimum rating required (e.g., 4.5) */
    private Double minRating;

    /** Sorting preference requested by user */
    private SortPreference sortPreference;

    /** Extracted brands or models to compare */
    private List<String> comparisonProducts;

    /** Interpreted core shopping intent */
    private ShoppingIntent intent;
}
