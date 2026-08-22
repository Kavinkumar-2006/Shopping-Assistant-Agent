package com.smartcart.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartcart.ai.entity.ProductSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Metadata collected during a single product discovery run.
 * Carries transparency information about which providers were queried,
 * how many succeeded, and deduplication statistics.
 *
 * <p>These fields are appended to {@link RecommendationResponse} and are
 * additive — existing API clients that don't read these fields are unaffected.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscoveryMetadata {

    /** Total number of providers queried during this request. */
    private int providersQueried;

    /** Number of providers that returned results without error. */
    private int providersSucceeded;

    /** Number of providers that failed (timeout, error, rate-limited). */
    private int providersFailed;

    /** Human-readable source labels that contributed products (e.g. ["LOCAL"]). */
    private List<String> dataSources;

    /** Total raw product count collected before deduplication. */
    private int totalProductsBeforeDeduplication;

    /** Unique product count after deduplication. */
    private int totalProductsAfterDeduplication;

    /** True if at least one external provider was queried (even if it failed). */
    private boolean externalProviderQueried;

    /** True if the result was served from cache for at least one provider. */
    private boolean servedFromCache;
}
