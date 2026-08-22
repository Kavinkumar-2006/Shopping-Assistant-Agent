package com.smartcart.ai.service;

import com.smartcart.ai.entity.ProductSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple per-provider rate-limit protection service.
 *
 * <p>Strategy: fixed window counter. Each provider has a request counter that
 * resets every {@code windowSeconds}. If the counter reaches {@code maxRequests}
 * within the window, the provider is flagged as rate-limited and no further
 * calls are allowed until the window expires.
 *
 * <p>Additionally, when a provider returns an HTTP 429 (Too Many Requests) or
 * similar error, {@link #recordRateLimitHit(ProductSource)} can be called to
 * immediately impose a {@code cooldownSeconds} penalty.
 *
 * <p>This is intentionally lightweight — no external infrastructure required.
 * Thread-safe via atomic operations on ConcurrentHashMap entries.
 */
@Slf4j
@Service
public class ProviderRateLimitService {

    /** Maximum requests allowed per provider per window. */
    @Value("${product.ratelimit.max-requests-per-window:100}")
    private int maxRequestsPerWindow;

    /** Window duration in seconds (default: 60 s = 1 minute). */
    @Value("${product.ratelimit.window-seconds:60}")
    private long windowSeconds;

    /** Cooldown period (in seconds) imposed after a rate-limit signal from the provider. */
    @Value("${product.ratelimit.cooldown-seconds:30}")
    private long cooldownSeconds;

    // ── Per-provider counters ────────────────────────────────────────────────

    /** Request count within the current window. */
    private final Map<ProductSource, AtomicInteger> windowCounts = new ConcurrentHashMap<>();

    /** Epoch-second timestamp when the current window started. */
    private final Map<ProductSource, AtomicLong> windowStart = new ConcurrentHashMap<>();

    /** Epoch-second timestamp until which the provider is in cooldown (0 = not in cooldown). */
    private final Map<ProductSource, AtomicLong> cooldownUntil = new ConcurrentHashMap<>();

    /** Creates the Spring-managed limiter; property values are injected after construction. */
    public ProviderRateLimitService() {
    }

    /** Test-friendly constructor with explicit rate-limit settings. */
    public ProviderRateLimitService(int maxRequestsPerWindow, long windowSeconds, long cooldownSeconds) {
        this.maxRequestsPerWindow = maxRequestsPerWindow;
        this.windowSeconds = windowSeconds;
        this.cooldownSeconds = cooldownSeconds;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} if a call to this provider is currently allowed
     * (i.e. not rate-limited or in cooldown).
     *
     * <p>LOCAL provider is always allowed.
     *
     * @param source the provider to check
     * @return true if the call should proceed
     */
    public boolean isAllowed(ProductSource source) {
        if (source == ProductSource.LOCAL) return true;

        // Check cooldown first (explicit rate-limit signal from provider)
        long cooldown = cooldownUntil.getOrDefault(source, new AtomicLong(0)).get();
        if (Instant.now().getEpochSecond() < cooldown) {
            log.debug("ProviderRateLimitService: {} is in cooldown until epoch {}", source, cooldown);
            return false;
        }

        // Check window counter
        ensureWindowInitialised(source);
        long windowBegin = windowStart.get(source).get();
        long now = Instant.now().getEpochSecond();

        if (now - windowBegin >= windowSeconds) {
            // Window expired — reset
            windowStart.get(source).set(now);
            windowCounts.get(source).set(0);
        }

        int count = windowCounts.get(source).get();
        if (count >= maxRequestsPerWindow) {
            log.warn("ProviderRateLimitService: {} has exceeded {} requests in {}s window",
                    source, maxRequestsPerWindow, windowSeconds);
            return false;
        }

        return true;
    }

    /**
     * Records a call attempt. Should be called immediately before making a provider request.
     * Does nothing for LOCAL provider.
     *
     * @param source the provider being called
     */
    public void recordRequest(ProductSource source) {
        if (source == ProductSource.LOCAL) return;
        ensureWindowInitialised(source);

        long windowBegin = windowStart.get(source).get();
        long now = Instant.now().getEpochSecond();

        if (now - windowBegin >= windowSeconds) {
            windowStart.get(source).set(now);
            windowCounts.get(source).set(1);
        } else {
            windowCounts.get(source).incrementAndGet();
        }
    }

    /**
     * Records that the provider returned a rate-limit signal (e.g. HTTP 429).
     * Imposes the configured cooldown period on the provider.
     *
     * @param source the provider that was rate-limited
     */
    public void recordRateLimitHit(ProductSource source) {
        if (source == ProductSource.LOCAL) return;
        long until = Instant.now().getEpochSecond() + cooldownSeconds;
        cooldownUntil.computeIfAbsent(source, k -> new AtomicLong(0)).set(until);
        log.warn("ProviderRateLimitService: {} rate-limited by provider — cooldown imposed for {}s",
                source, cooldownSeconds);
    }

    /**
     * Clears cooldown and window state for a provider (used in tests).
     */
    public void reset(ProductSource source) {
        windowCounts.remove(source);
        windowStart.remove(source);
        cooldownUntil.remove(source);
    }

    /**
     * Returns how many requests have been made in the current window for a provider.
     */
    public int getWindowCount(ProductSource source) {
        ensureWindowInitialised(source);
        return windowCounts.getOrDefault(source, new AtomicInteger(0)).get();
    }

    /**
     * Returns true if the provider is currently in an explicit cooldown period.
     */
    public boolean isInCooldown(ProductSource source) {
        long cooldown = cooldownUntil.getOrDefault(source, new AtomicLong(0)).get();
        return Instant.now().getEpochSecond() < cooldown;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void ensureWindowInitialised(ProductSource source) {
        windowCounts.computeIfAbsent(source, k -> new AtomicInteger(0));
        windowStart.computeIfAbsent(source, k -> new AtomicLong(Instant.now().getEpochSecond()));
    }
}
