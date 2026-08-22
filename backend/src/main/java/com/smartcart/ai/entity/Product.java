package com.smartcart.ai.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Core domain entity representing a product in the catalog.
 * Loaded from products.json at application startup.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Product {

    /** Unique product identifier */
    private String id;

    /** Display name of the product */
    private String name;

    /** Brand name (e.g., "Apple", "Samsung") */
    private String brand;

    /** Product category (e.g., "laptop", "phone", "headphones", "shoes") */
    private String category;

    /** Price in Indian Rupees (₹) */
    private long price;

    /** Rating out of 5.0 */
    private double rating;

    /** Number of customer reviews */
    private int reviewCount;

    /** Short product description */
    private String description;

    /** Emoji icon used as image fallback in the frontend */
    private String emoji;

    /** URL for product image */
    private String imageUrl;

    /**
     * Key specifications as a map.
     * e.g., { "RAM": "16GB", "Processor": "Intel i7", "Display": "15.6 inch FHD" }
     */
    private Map<String, String> specs;

    /**
     * Use-case and feature tags used for recommendation matching.
     * e.g., ["coding", "office", "lightweight", "long-battery"]
     */
    private List<String> tags;

    /** Highlight bullet points shown on the product card */
    private List<String> highlights;

    // ── Phase 4 Source Metadata & Multi-Provider Extensions ──────────────────

    /** Source / Provider identification */
    @Builder.Default
    private ProductSource source = ProductSource.LOCAL;

    /** Direct web page URL for this product on the retailer website */
    private String productUrl;

    /** Provider-native product identifier. Null for sources that do not expose one. */
    private String sourceProductId;

    /** Provider-native product page URL. Never synthesized for local catalog items. */
    private String sourceProductUrl;

    /** Base retailer / provider website URL */
    private String sourceUrl;

    /** Currency code (e.g. "INR", "USD") */
    @Builder.Default
    private String currency = "INR";

    /** Stock availability status */
    @Builder.Default
    private Boolean availability = true;

    /** Timestamp ISO string when the product was fetched from provider */
    private String fetchedTime;

    /** Merchant or store name (e.g., "ShopSmart Local Store", "Amazon Authorized") */
    private String storeName;

    /** Multi-merchant offers for this product */
    @Builder.Default
    private List<ProductOffer> offers = new ArrayList<>();

    // ── Phase 4 Step 5: Intelligent Ranking Metadata ─────────────────────────

    /** Recommendation label (Best Overall, Best Value, etc.) - computed during ranking */
    private String recommendationLabel;

    /** Overall relevance score - computed during ranking */
    private Double overallScore;

    /** Match percentage (0-100%) - computed during ranking */
    private Double matchPercentage;

    /** Value for money score - computed during ranking */
    private Double valueScore;
}


