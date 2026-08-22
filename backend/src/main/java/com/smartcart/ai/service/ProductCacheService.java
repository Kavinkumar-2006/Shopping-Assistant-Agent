package com.smartcart.ai.service;

import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.ProductSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight in-memory TTL cache for external provider product results.
 *
 * <p>Goals:
 * <ul>
 *   <li>Reduce redundant external API calls for identical queries</li>
 *   <li>Respect TTL — expired entries are treated as cache misses</li>
 *   <li>Thread-safe (ConcurrentHashMap + immutable CacheEntry record)</li>
 *   <li>Replaceable — the interface is kept simple so Redis can be swapped in later</li>
 * </ul>
 *
 * <p>Only external provider results are cached. LOCAL catalog results are never
 * cached here because the local catalog is already in memory and instantaneous.
 */
@Slf4j
@Service
public class ProductCacheService {

    /** Default TTL: 5 minutes (configurable via property). */
    @Value("${product.cache.ttl-seconds:300}")
    private long ttlSeconds;

    /** Maximum total entries allowed before a sweep evicts the oldest. */
    @Value("${product.cache.max-entries:200}")
    private int maxEntries;

    private final Map<String, CacheEntry> store = new ConcurrentHashMap<>();

    /** Creates the Spring-managed cache; property values are injected after construction. */
    public ProductCacheService() {
    }

    /** Test-friendly constructor with explicit, non-zero cache settings. */
    public ProductCacheService(long ttlSeconds, int maxEntries) {
        this.ttlSeconds = ttlSeconds;
        this.maxEntries = maxEntries;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Stores a provider result in the cache.
     *
     * @param key      cache key (built via {@link #buildKey})
     * @param source   which provider produced the results
     * @param products product list to cache
     */
    public void put(String key, ProductSource source, List<Product> products) {
        if (key == null || products == null) return;

        evictIfNeeded();

        CacheEntry entry = new CacheEntry(
                key,
                source,
                Collections.unmodifiableList(new ArrayList<>(products)),
                Instant.now(),
                Instant.now().plusSeconds(ttlSeconds)
        );
        store.put(key, entry);
        log.debug("ProductCacheService: cached {} products for key='{}' (TTL={}s)", products.size(), key, ttlSeconds);
    }

    /**
     * Retrieves a non-expired entry from the cache, or returns empty if absent/expired.
     *
     * @param key cache key
     * @return Optional containing the cached product list, or empty on miss/expiry
     */
    public Optional<List<Product>> get(String key) {
        if (key == null) return Optional.empty();

        CacheEntry entry = store.get(key);
        if (entry == null) {
            return Optional.empty();
        }

        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(key);
            log.debug("ProductCacheService: cache miss (expired) for key='{}'", key);
            return Optional.empty();
        }

        log.debug("ProductCacheService: cache hit for key='{}'", key);
        return Optional.of(entry.products());
    }

    /**
     * Checks whether a valid (non-expired) entry exists for the key.
     */
    public boolean contains(String key) {
        return get(key).isPresent();
    }

    /**
     * Removes a specific entry from the cache.
     */
    public void invalidate(String key) {
        store.remove(key);
    }

    /**
     * Removes all entries for a specific provider source.
     */
    public void invalidateBySource(ProductSource source) {
        store.entrySet().removeIf(e -> e.getValue().source() == source);
        log.info("ProductCacheService: invalidated all entries for source={}", source);
    }

    /**
     * Removes all expired entries from the store.
     */
    public int evictExpired() {
        Instant now = Instant.now();
        int before = store.size();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
        int evicted = before - store.size();
        if (evicted > 0) {
            log.debug("ProductCacheService: evicted {} expired entries", evicted);
        }
        return evicted;
    }

    /**
     * Returns number of live (non-expired) entries currently in the cache.
     */
    public int size() {
        evictExpired();
        return store.size();
    }

    /**
     * Clears the entire cache. Used in tests and for full refresh scenarios.
     */
    public void clear() {
        store.clear();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Key builder
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a deterministic cache key from provider + query parameters.
     * The key is a normalized, lowercased concatenation — not a hash — so it is
     * readable in debug logs without exposing sensitive query details.
     *
     * @param source   the provider source
     * @param query    free-text query (may be null)
     * @param category product category (may be null)
     * @param brand    brand filter (may be null)
     * @param maxPrice price ceiling (may be null)
     * @return stable cache key string
     */
    public String buildKey(ProductSource source, String query, String category,
                           String brand, Long maxPrice) {
        StringBuilder sb = new StringBuilder();
        sb.append(source != null ? source.name().toLowerCase() : "unknown");
        sb.append(":q=").append(normalise(query));
        sb.append(":cat=").append(normalise(category));
        sb.append(":brand=").append(normalise(brand));
        sb.append(":max=").append(maxPrice != null ? maxPrice : "any");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String normalise(String s) {
        return s != null ? s.trim().toLowerCase() : "";
    }

    /**
     * If the store exceeds {@link #maxEntries}, evict expired entries first.
     * If still over limit, remove the oldest entries by insertion order.
     */
    private void evictIfNeeded() {
        if (store.size() < maxEntries) return;
        evictExpired();
        if (store.size() >= maxEntries) {
            // Remove oldest 10% — ConcurrentHashMap does not guarantee order,
            // so we sort by cachedAt timestamp and remove the earliest ones.
            int toRemove = Math.max(1, maxEntries / 10);
            store.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getValue().cachedAt()))
                    .limit(toRemove)
                    .map(Map.Entry::getKey)
                    .forEach(store::remove);
            log.debug("ProductCacheService: evicted {} oldest entries to stay under maxEntries={}", toRemove, maxEntries);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cache entry record (Java 16+ record — immutable, thread-safe)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Immutable cache entry. Uses Java record for conciseness and immutability.
     */
    public record CacheEntry(
            String key,
            ProductSource source,
            List<Product> products,
            Instant cachedAt,
            Instant expiresAt
    ) {}
}
