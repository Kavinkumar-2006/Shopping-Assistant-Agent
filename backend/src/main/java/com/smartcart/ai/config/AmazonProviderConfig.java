package com.smartcart.ai.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration placeholder for Amazon Product Advertising API integration.
 *
 * <p>No Amazon provider implementation exists yet — this class reserves the
 * configuration structure so credentials can be added when ready without
 * touching application code.
 *
 * <p>The application starts and functions fully without any of these values set.
 * All fields default to blank/false via environment variable fallbacks.
 *
 * <p>To activate in the future:
 * <ol>
 *   <li>Register at https://affiliate-program.amazon.in/ (or .com for US)</li>
 *   <li>Obtain AWS Access Key, Secret Key and an Associates Partner Tag</li>
 *   <li>Set the corresponding environment variables</li>
 *   <li>Create an AmazonProductProvider implementing {@code ProductProvider}</li>
 * </ol>
 *
 * <p><strong>Security:</strong> credentials are read from environment variables only —
 * never hard-coded, never committed to source control, never printed in logs.
 */
@Slf4j
@Data
@Component
public class AmazonProviderConfig {

    /** Master switch — false by default until a real implementation exists. */
    @Value("${amazon.provider.enabled:false}")
    private boolean enabled;

    /** AWS IAM Access Key for Product Advertising API v5. */
    @Value("${amazon.access.key:}")
    private String accessKey;

    /** AWS IAM Secret Key — NEVER log this value. */
    @Value("${amazon.secret.key:}")
    private String secretKey;

    /** Associates Partner Tag (tracking ID) required by Amazon PA API v5. */
    @Value("${amazon.partner.tag:}")
    private String partnerTag;

    /** Target marketplace (e.g. www.amazon.in, www.amazon.com). */
    @Value("${amazon.marketplace:www.amazon.in}")
    private String marketplace;

    /** Request timeout in milliseconds. */
    @Value("${amazon.api.timeout-ms:5000}")
    private int timeoutMs;

    /**
     * Returns true only when the provider is enabled AND all three required
     * credentials are non-blank.
     *
     * <p>This check must be called before any API request is attempted.
     * The application never crashes on missing credentials — it silently
     * skips the Amazon provider and falls back to LocalCatalogProvider.
     */
    public boolean isConfigured() {
        boolean configured = enabled
                && accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank()
                && partnerTag != null && !partnerTag.isBlank();

        if (enabled && !configured) {
            log.warn("AmazonProviderConfig: enabled=true but credentials are missing or incomplete. "
                    + "Amazon provider will be skipped. Set AMAZON_ACCESS_KEY, AMAZON_SECRET_KEY, AMAZON_PARTNER_TAG.");
        }

        return configured;
    }
}
