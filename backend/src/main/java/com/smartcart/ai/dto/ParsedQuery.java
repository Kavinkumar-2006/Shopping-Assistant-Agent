package com.smartcart.ai.dto;

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
 *
 * Example for "suggest a laptop under 60000 for coding":
 *   category  = "laptop"
 *   budget    = 60000L
 *   useCase   = "coding"
 *   keywords  = ["coding", "student"]
 */
@Data
@Builder
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
     * Using Long (object) instead of primitive so null = "not present".
     */
    private Long budget;

    /**
     * Primary use-case — the single most dominant intent in the query.
     * Possible values: "coding" | "gaming" | "camera" | "battery" |
     *                  "running" | "music" | "office" | "travel" |
     *                  "anc"    | "student" | "budget" | null
     * Null if no use-case keyword was detected.
     */
    private String useCase;

    /**
     * All matched use-case / feature keywords for multi-dimensional scoring.
     * A superset of useCase — e.g., ["coding", "lightweight", "student"]
     */
    private List<String> keywords;

    /** Original raw query string — used for logging and summary generation. */
    private String originalQuery;
}
