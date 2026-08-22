package com.smartcart.ai.service;

import com.smartcart.ai.config.ExternalProductProviderConfig;
import com.smartcart.ai.config.FlipkartProviderConfig;
import com.smartcart.ai.dto.ProviderHealthInfo;
import com.smartcart.ai.entity.ProductSource;
import com.smartcart.ai.entity.ProviderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks and reports the health status of every registered product provider.
 *
 * <p>Status is derived from configuration state and updated dynamically when
 * providers succeed or fail during discovery calls. No credentials or secrets
 * are ever logged or included in health responses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderHealthService {

    private final FlipkartProviderConfig flipkartConfig;
    private final ExternalProductProviderConfig externalConfig;

    /** Per-provider success counters (thread-safe). */
    private final Map<ProductSource, AtomicLong> successCounts = new ConcurrentHashMap<>();

    /** Per-provider failure counters (thread-safe). */
    private final Map<ProductSource, AtomicLong> failureCounts = new ConcurrentHashMap<>();

    /** Per-provider last recorded error message (no credentials). */
    private final Map<ProductSource, String> lastErrors = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // Status resolution
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the current status of a provider based on its configuration.
     *
     * @param source the provider to check
     * @return current {@link ProviderStatus}
     */
    public ProviderStatus getStatus(ProductSource source) {
        return switch (source) {
            case LOCAL -> ProviderStatus.AVAILABLE;
            case FLIPKART -> resolveExternalStatus(
                    flipkartConfig.isEnabled(),
                    flipkartConfig.isConfigured()
            );
            case PRODUCT_API -> resolveExternalStatus(
                    externalConfig.isEnabled(),
                    externalConfig.isConfigured()
            );
            case AMAZON -> ProviderStatus.NOT_CONFIGURED; // Placeholder — no impl yet
            case RETAILER_A, RETAILER_B -> ProviderStatus.DISABLED;
        };
    }

    /**
     * Returns true if the provider is currently usable (not disabled/rate-limited/error).
     */
    public boolean isAvailable(ProductSource source) {
        ProviderStatus status = getStatus(source);
        return status == ProviderStatus.AVAILABLE;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Call tracking — called by ProductDiscoveryService after each provider call
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Records a successful provider call.
     */
    public void recordSuccess(ProductSource source) {
        successCounts.computeIfAbsent(source, k -> new AtomicLong(0)).incrementAndGet();
        lastErrors.remove(source);
    }

    /**
     * Records a failed provider call.
     *
     * @param source  the provider
     * @param message error message (must NOT contain credentials)
     */
    public void recordFailure(ProductSource source, String message) {
        failureCounts.computeIfAbsent(source, k -> new AtomicLong(0)).incrementAndGet();
        // Store only a sanitised version — strip anything that looks like a token/key
        lastErrors.put(source, sanitise(message));
        log.warn("ProviderHealthService: recorded failure for {} — {}", source, sanitise(message));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Health snapshot
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a health snapshot for every known provider.
     * No credentials, keys, or tokens are included.
     */
    public List<ProviderHealthInfo> getAllProviderHealth() {
        List<ProviderHealthInfo> result = new ArrayList<>();
        for (ProductSource source : ProductSource.values()) {
            result.add(buildHealthInfo(source));
        }
        return result;
    }

    /**
     * Returns a health snapshot for a single provider.
     */
    public ProviderHealthInfo getProviderHealth(ProductSource source) {
        return buildHealthInfo(source);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ProviderHealthInfo buildHealthInfo(ProductSource source) {
        ProviderStatus status = getStatus(source);

        String reason = buildReason(source, status);
        boolean credentialsPresent = hasCredentials(source);

        return ProviderHealthInfo.builder()
                .source(source)
                .status(status)
                .reason(reason)
                .credentialsPresent(credentialsPresent)
                .lastChecked(Instant.now().toString())
                .successfulCalls(successCounts.getOrDefault(source, new AtomicLong(0)).get())
                .failedCalls(failureCounts.getOrDefault(source, new AtomicLong(0)).get())
                .build();
    }

    private ProviderStatus resolveExternalStatus(boolean enabled, boolean configured) {
        if (!enabled) return ProviderStatus.DISABLED;
        if (!configured) return ProviderStatus.NOT_CONFIGURED;
        return ProviderStatus.AVAILABLE;
    }

    private boolean hasCredentials(ProductSource source) {
        return switch (source) {
            case LOCAL -> true;
            case FLIPKART -> {
                String id = flipkartConfig.getAffiliateId();
                String token = flipkartConfig.getAffiliateToken();
                yield id != null && !id.isBlank() && token != null && !token.isBlank();
            }
            case PRODUCT_API -> {
                String key = externalConfig.getApiKey();
                yield key != null && !key.isBlank();
            }
            case AMAZON, RETAILER_A, RETAILER_B -> false;
        };
    }

    private String buildReason(ProductSource source, ProviderStatus status) {
        return switch (status) {
            case AVAILABLE -> source == ProductSource.LOCAL
                    ? "Local catalog loaded and ready"
                    : "Provider enabled and credentials configured";
            case DISABLED -> "Provider is disabled in configuration";
            case NOT_CONFIGURED -> "Provider is enabled but credentials are missing or incomplete";
            case ERROR -> lastErrors.getOrDefault(source, "Unknown error");
            case RATE_LIMITED -> "Provider is temporarily suspended due to rate-limit protection";
        };
    }

    /** Removes credential-like patterns from a message before storing/logging. */
    private String sanitise(String message) {
        if (message == null) return "Unknown error";
        return message.replaceAll("(?i)(key|token|secret|password|id|auth)=[^\\s&,]+", "$1=***REDACTED***");
    }
}
