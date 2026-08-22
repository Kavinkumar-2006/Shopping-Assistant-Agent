package com.smartcart.ai.service;

import com.smartcart.ai.config.FlipkartProviderConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.Map;

/**
 * HTTP client for Flipkart Affiliate API with authentication, timeout, and error handling.
 * Handles Flipkart-specific authentication headers (Fk-Affiliate-Id, Fk-Affiliate-Token).
 */
@Slf4j
@Service
public class FlipkartApiClient {

    private final RestTemplate restTemplate;
    private final FlipkartProviderConfig config;

    public FlipkartApiClient(FlipkartProviderConfig config, RestTemplateBuilder builder) {
        this.config = config;
        int timeoutMs = config.getTimeoutMs() > 0 ? config.getTimeoutMs() : 5000;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    /**
     * Executes a GET request to Flipkart Affiliate API with proper authentication.
     *
     * @param endpoint     API endpoint path (e.g., "/products/search")
     * @param queryParams  query parameters
     * @param responseType target response class
     * @return response body or null if request failed
     */
    public <T> T get(String endpoint, Map<String, String> queryParams, Class<T> responseType) {
        if (!config.isConfigured()) {
            log.debug("FlipkartApiClient: provider not configured, skipping request");
            return null;
        }

        try {
            String fullUrl = config.getBaseUrl() + endpoint;
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(fullUrl);

            if (queryParams != null) {
                queryParams.forEach((k, v) -> {
                    if (v != null && !v.isBlank()) {
                        uriBuilder.queryParam(k, v);
                    }
                });
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add("Fk-Affiliate-Id", config.getAffiliateId());
            headers.add("Fk-Affiliate-Token", config.getAffiliateToken());
            headers.add("Accept", "application/json");

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            String targetUri = uriBuilder.build().toUriString();

            log.info("FlipkartApiClient: sending GET request to {}", sanitizeUrl(targetUri));

            ResponseEntity<T> response = restTemplate.exchange(
                    targetUri,
                    HttpMethod.GET,
                    entity,
                    responseType
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("FlipkartApiClient: received successful response from Flipkart API");
                return response.getBody();
            } else {
                log.warn("FlipkartApiClient: received non-2xx status code {} from Flipkart API",
                        response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("FlipkartApiClient: request failed — {}", e.getMessage());
            return null;
        }
    }

    /**
     * Sanitizes URLs for logging so sensitive credentials are never logged.
     * Redacts affiliate ID and token parameters.
     */
    private String sanitizeUrl(String url) {
        if (url == null) return "";
        return url.replaceAll("(?i)(id|token|auth|key|secret|password)=[^&]+", "$1=***");
    }
}
