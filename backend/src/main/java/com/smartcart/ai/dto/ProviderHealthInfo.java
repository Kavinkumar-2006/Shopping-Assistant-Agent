package com.smartcart.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartcart.ai.entity.ProviderStatus;
import com.smartcart.ai.entity.ProductSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight health snapshot for a single product provider.
 * Does NOT expose credentials or secrets.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderHealthInfo {

    /** The provider identity (LOCAL, FLIPKART, AMAZON, PRODUCT_API, …). */
    private ProductSource source;

    /** Current availability status. */
    private ProviderStatus status;

    /** Human-readable reason (never contains secrets). */
    private String reason;

    /** Whether credentials are present (true/false only — no values logged). */
    private boolean credentialsPresent;

    /** ISO-8601 timestamp of the last status check. */
    private String lastChecked;

    /** Number of successful calls since startup. */
    private long successfulCalls;

    /** Number of failed calls since startup. */
    private long failedCalls;
}
