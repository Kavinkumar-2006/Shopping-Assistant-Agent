package com.smartcart.ai.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration bean for Flipkart Affiliate API integration.
 * Reads environment variables with safe defaults. Application continues
 * functioning with LocalCatalogProvider when credentials are absent.
 */
@Slf4j
@Data
@Component
public class FlipkartProviderConfig {

    @Value("${flipkart.provider.enabled:false}")
    private boolean enabled;

    @Value("${flipkart.affiliate.id:}")
    private String affiliateId;

    @Value("${flipkart.affiliate.token:}")
    private String affiliateToken;

    @Value("${flipkart.api.base-url:https://affiliate-api.flipkart.net/affiliate/api}")
    private String baseUrl;

    @Value("${flipkart.api.timeout-ms:5000}")
    private int timeoutMs = 5000;

    /**
     * Checks whether the Flipkart provider is fully configured with valid credentials.
     * Application can start without credentials (graceful fallback to LocalCatalogProvider).
     */
    public boolean isConfigured() {
        boolean configured = enabled
                && affiliateId != null && !affiliateId.isBlank()
                && affiliateToken != null && !affiliateToken.isBlank()
                && baseUrl != null && !baseUrl.isBlank();

        if (enabled && !configured) {
            log.warn("FlipkartProviderConfig: enabled=true but credentials are missing or incomplete. " +
                    "Flipkart provider will be disabled. Set FLIPKART_AFFILIATE_ID and FLIPKART_AFFILIATE_TOKEN.");
        }

        return configured;
    }
}
