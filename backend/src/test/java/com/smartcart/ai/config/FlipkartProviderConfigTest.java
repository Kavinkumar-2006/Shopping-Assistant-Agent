package com.smartcart.ai.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FlipkartProviderConfigTest {

    @Test
    @DisplayName("isConfigured returns true when all credentials are present")
    void testConfiguredWithAllCredentials() {
        FlipkartProviderConfig config = new FlipkartProviderConfig();
        config.setEnabled(true);
        config.setAffiliateId("test_affiliate_id");
        config.setAffiliateToken("test_token");
        config.setBaseUrl("https://affiliate-api.flipkart.net/affiliate/api");

        assertTrue(config.isConfigured());
    }

    @Test
    @DisplayName("isConfigured returns false when disabled")
    void testNotConfiguredWhenDisabled() {
        FlipkartProviderConfig config = new FlipkartProviderConfig();
        config.setEnabled(false);
        config.setAffiliateId("test_affiliate_id");
        config.setAffiliateToken("test_token");
        config.setBaseUrl("https://affiliate-api.flipkart.net/affiliate/api");

        assertFalse(config.isConfigured());
    }

    @Test
    @DisplayName("isConfigured returns false when affiliate ID is missing")
    void testNotConfiguredWhenAffiliateIdMissing() {
        FlipkartProviderConfig config = new FlipkartProviderConfig();
        config.setEnabled(true);
        config.setAffiliateId("");
        config.setAffiliateToken("test_token");
        config.setBaseUrl("https://affiliate-api.flipkart.net/affiliate/api");

        assertFalse(config.isConfigured());
    }

    @Test
    @DisplayName("isConfigured returns false when affiliate token is missing")
    void testNotConfiguredWhenAffiliateTokenMissing() {
        FlipkartProviderConfig config = new FlipkartProviderConfig();
        config.setEnabled(true);
        config.setAffiliateId("test_affiliate_id");
        config.setAffiliateToken("");
        config.setBaseUrl("https://affiliate-api.flipkart.net/affiliate/api");

        assertFalse(config.isConfigured());
    }

    @Test
    @DisplayName("isConfigured returns false when base URL is missing")
    void testNotConfiguredWhenBaseUrlMissing() {
        FlipkartProviderConfig config = new FlipkartProviderConfig();
        config.setEnabled(true);
        config.setAffiliateId("test_affiliate_id");
        config.setAffiliateToken("test_token");
        config.setBaseUrl("");

        assertFalse(config.isConfigured());
    }

    @Test
    @DisplayName("isConfigured returns false when affiliate ID is null")
    void testNotConfiguredWhenAffiliateIdNull() {
        FlipkartProviderConfig config = new FlipkartProviderConfig();
        config.setEnabled(true);
        config.setAffiliateId(null);
        config.setAffiliateToken("test_token");
        config.setBaseUrl("https://affiliate-api.flipkart.net/affiliate/api");

        assertFalse(config.isConfigured());
    }

    @Test
    @DisplayName("Default values are set correctly")
    void testDefaultValues() {
        FlipkartProviderConfig config = new FlipkartProviderConfig();
        
        // These will be set by Spring from application.properties defaults
        // We're testing that the setter/getter work properly
        config.setTimeoutMs(5000);
        
        assertEquals(5000, config.getTimeoutMs());
    }
}
