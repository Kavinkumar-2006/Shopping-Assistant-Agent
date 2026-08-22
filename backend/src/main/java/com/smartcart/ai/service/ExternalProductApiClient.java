package com.smartcart.ai.service;

import com.smartcart.ai.config.ExternalProductProviderConfig;
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
 * Generic HTTP client for querying external product APIs safely with timeout and error handling.
 */
@Slf4j
@Service
public class ExternalProductApiClient {

    private final RestTemplate restTemplate;

    public ExternalProductApiClient(ExternalProductProviderConfig config, RestTemplateBuilder builder) {
        int timeoutMs = config.getTimeoutMs() > 0 ? config.getTimeoutMs() : 5000;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    /**
     * Executes a GET request against an external product endpoint safely.
     *
     * @param baseUrl     endpoint base URL
     * @param queryParams query parameters
     * @param headersMap  HTTP request headers (e.g. API keys)
     * @param responseType target response class
     * @return ResponseEntity or null if an error/timeout occurred
     */
    public <T> T get(String baseUrl, Map<String, String> queryParams, Map<String, String> headersMap, Class<T> responseType) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }

        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUrl);
            if (queryParams != null) {
                queryParams.forEach((k, v) -> {
                    if (v != null && !v.isBlank()) {
                        uriBuilder.queryParam(k, v);
                    }
                });
            }

            HttpHeaders headers = new HttpHeaders();
            if (headersMap != null) {
                headersMap.forEach(headers::add);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            String targetUri = uriBuilder.build().toUriString();

            log.info("ExternalProductApiClient: sending GET request to {}", sanitizeUrl(targetUri));

            ResponseEntity<T> response = restTemplate.exchange(
                    targetUri,
                    HttpMethod.GET,
                    entity,
                    responseType
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                log.warn("ExternalProductApiClient: received non-2xx status code {} from {}",
                        response.getStatusCode(), sanitizeUrl(targetUri));
                return null;
            }
        } catch (Exception e) {
            log.error("ExternalProductApiClient: request failed for {} — {}", sanitizeUrl(baseUrl), e.getMessage());
            return null;
        }
    }

    /**
     * Sanitizes URLs for logging so sensitive query parameters or key patterns are never logged.
     */
    private String sanitizeUrl(String url) {
        if (url == null) return "";
        return url.replaceAll("(?i)(key|token|auth|secret|password)=[^&]+", "$1=***REDACTED***");
    }
}
