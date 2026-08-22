package com.smartcart.ai.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration bean for external product discovery providers.
 * Reads environment variables / application properties with safe defaults.
 */
@Data
@Component
public class ExternalProductProviderConfig {

    @Value("${product.provider.external.enabled:false}")
    private boolean enabled;

    @Value("${product.provider.external.base-url:}")
    private String baseUrl;

    @Value("${product.provider.external.api-key:}")
    private String apiKey;

    @Value("${product.provider.external.timeout-ms:5000}")
    private int timeoutMs = 5000;

    @Value("${product.provider.external.page-size:20}")
    private int pageSize = 20;

    /**
     * Checks whether the external provider is fully configured with a valid base URL.
     */
    public boolean isConfigured() {
        return enabled && baseUrl != null && !baseUrl.isBlank();
    }
}
