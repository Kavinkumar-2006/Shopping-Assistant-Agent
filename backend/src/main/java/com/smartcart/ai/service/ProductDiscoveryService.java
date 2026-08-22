package com.smartcart.ai.service;

import com.smartcart.ai.dto.DiscoveryMetadata;
import com.smartcart.ai.config.ExternalProductProviderConfig;
import com.smartcart.ai.config.FlipkartProviderConfig;
import com.smartcart.ai.dto.PagedProductResult;
import com.smartcart.ai.dto.ParsedQuery;
import com.smartcart.ai.dto.ProductDiscoveryQuery;
import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.ProductSource;
import com.smartcart.ai.provider.ProductProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Central orchestrator for large-scale product discovery.
 *
 * <p>Pipeline:
 * <ol>
 *   <li>Iterate all registered {@link ProductProvider}s — rate-limit + cache aware</li>
 *   <li>Aggregate raw candidates from available providers</li>
 *   <li>Deduplicate across providers</li>
 *   <li>Filter by parsed constraints</li>
 *   <li>Rank with {@link ProductRankingService}</li>
 *   <li>Paginate with {@link PagedProductResult}</li>
 * </ol>
 *
 * <p>Failure isolation: if any provider throws, its results are skipped and
 * discovery continues with the remaining providers. LOCAL catalog is always
 * the final safety net.
 */
@Slf4j
@Service
public class ProductDiscoveryService {

    private final List<ProductProvider> providers;
    private final ProductDeduplicationService deduplicationService;
    private final ProductFilterService filterService;
    private final ProductRankingService rankingService;
    private final ProviderHealthService healthService;
    private final ProductCacheService cacheService;
    private final ProviderRateLimitService rateLimitService;

    @Autowired
    public ProductDiscoveryService(
            List<ProductProvider> providers,
            ProductDeduplicationService deduplicationService,
            ProductFilterService filterService,
            ProductRankingService rankingService,
            ProviderHealthService healthService,
            ProductCacheService cacheService,
            ProviderRateLimitService rateLimitService) {
        this.providers = providers;
        this.deduplicationService = deduplicationService;
        this.filterService = filterService;
        this.rankingService = rankingService;
        this.healthService = healthService;
        this.cacheService = cacheService;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Compatibility constructor for focused unit tests. Production uses the
     * dependency-injected constructor above with configured health/cache/limit services.
     */
    public ProductDiscoveryService(
            List<? extends ProductProvider> providers,
            ProductDeduplicationService deduplicationService,
            ProductFilterService filterService,
            ProductRankingService rankingService) {
        this(
                new ArrayList<>(providers),
                deduplicationService,
                filterService,
                rankingService,
                new ProviderHealthService(new FlipkartProviderConfig(), new ExternalProductProviderConfig()),
                new ProductCacheService(300, 200),
                new ProviderRateLimitService(100, 60, 30)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Executes product discovery and returns paginated results.
     */
    public PagedProductResult discover(ProductDiscoveryQuery query) {
        log.info("ProductDiscoveryService.discover: querying {} provider(s) for " +
                        "category='{}', query='{}', page={}, pageSize={}",
                providers.size(),
                query.getCategory(), query.getQuery(),
                query.getPage(), query.getPageSize());

        DiscoveryResult dr = runDiscoveryPipeline(query);
        return PagedProductResult.create(dr.rankedProducts(), query.getPage(), query.getPageSize());
    }

    /**
     * Returns all ranked products without pagination (used internally and in tests).
     */
    public List<Product> discoverAll(ProductDiscoveryQuery query) {
        return runDiscoveryPipeline(query).rankedProducts();
    }

    /**
     * Runs the full discovery pipeline and returns both the product list and
     * the {@link DiscoveryMetadata} collected during the run.
     */
    public DiscoveryResult discoverWithMetadata(ProductDiscoveryQuery query) {
        return runDiscoveryPipeline(query);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core pipeline
    // ─────────────────────────────────────────────────────────────────────────

    private DiscoveryResult runDiscoveryPipeline(ProductDiscoveryQuery query) {
        List<Product> rawCandidates = new ArrayList<>();

        // Metadata collectors
        int queried = 0;
        int succeeded = 0;
        int failed = 0;
        List<String> dataSources = new ArrayList<>();
        boolean externalQueried = false;
        boolean servedFromCache = false;

        // ── Step 1: Aggregate from all providers ──────────────────────────
        for (ProductProvider provider : providers) {
            ProductSource source = provider.getSource();
            boolean isExternal = source != ProductSource.LOCAL;

            // Rate-limit check
            if (!rateLimitService.isAllowed(source)) {
                log.warn("ProductDiscoveryService: skipping {} — rate-limited", source);
                healthService.recordFailure(source, "Rate-limited — skipped this request");
                queried++;
                failed++;
                continue;
            }

            queried++;
            if (isExternal) externalQueried = true;

            // Cache check for external providers (skip cache for LOCAL — always fast)
            if (isExternal) {
                String cacheKey = cacheService.buildKey(
                        source,
                        query.getQuery(), query.getCategory(),
                        query.getBrand(), query.getMaxPrice()
                );
                Optional<List<Product>> cached = cacheService.get(cacheKey);
                if (cached.isPresent()) {
                    List<Product> fromCache = cached.get();
                    rawCandidates.addAll(fromCache);
                    dataSources.add(source.name() + " (cached)");
                    succeeded++;
                    servedFromCache = true;
                    log.debug("ProductDiscoveryService: cache hit for {} — {} products", source, fromCache.size());
                    continue;
                }
            }

            // Live provider call
            try {
                rateLimitService.recordRequest(source);
                List<Product> providerProducts = provider.discover(query);

                if (providerProducts != null && !providerProducts.isEmpty()) {
                    rawCandidates.addAll(providerProducts);
                    dataSources.add(source.name());
                    healthService.recordSuccess(source);
                    succeeded++;

                    // Cache external results for future requests
                    if (isExternal) {
                        String cacheKey = cacheService.buildKey(
                                source,
                                query.getQuery(), query.getCategory(),
                                query.getBrand(), query.getMaxPrice()
                        );
                        cacheService.put(cacheKey, source, providerProducts);
                    }

                    log.debug("ProductDiscoveryService: provider {} returned {} products",
                            source, providerProducts.size());
                } else {
                    // Provider returned empty but didn't throw — still counts as success
                    healthService.recordSuccess(source);
                    succeeded++;
                    log.debug("ProductDiscoveryService: provider {} returned 0 products", source);
                }

            } catch (Exception e) {
                healthService.recordFailure(source, e.getMessage());
                failed++;
                log.error("ProductDiscoveryService: provider {} failed — {}. Continuing with other providers.",
                        source, e.getMessage());
            }
        }

        log.info("ProductDiscoveryService: aggregated {} total raw products from {} providers " +
                        "(queried={}, succeeded={}, failed={})",
                rawCandidates.size(), providers.size(), queried, succeeded, failed);

        // ── Step 2: Deduplicate ───────────────────────────────────────────
        int beforeDedup = rawCandidates.size();
        List<Product> uniqueProducts = deduplicationService.deduplicate(rawCandidates);
        int afterDedup = uniqueProducts.size();

        // ── Step 3: Convert query → ParsedQuery for filter/ranking ────────
        ParsedQuery parsedConstraint = toParsedQuery(query);

        // ── Step 4: Filter ────────────────────────────────────────────────
        List<Product> filteredProducts = filterService.filter(uniqueProducts, parsedConstraint);
        log.info("ProductDiscoveryService: {} products remain after filtering", filteredProducts.size());

        // ── Step 5: Rank ──────────────────────────────────────────────────
        List<Product> ranked = rankingService.rank(filteredProducts, parsedConstraint);

        // ── Build metadata ────────────────────────────────────────────────
        DiscoveryMetadata metadata = DiscoveryMetadata.builder()
                .providersQueried(queried)
                .providersSucceeded(succeeded)
                .providersFailed(failed)
                .dataSources(dataSources.isEmpty() ? List.of("LOCAL") : dataSources)
                .totalProductsBeforeDeduplication(beforeDedup)
                .totalProductsAfterDeduplication(afterDedup)
                .externalProviderQueried(externalQueried)
                .servedFromCache(servedFromCache)
                .build();

        return new DiscoveryResult(ranked, metadata);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private ParsedQuery toParsedQuery(ProductDiscoveryQuery query) {
        if (query == null) return ParsedQuery.builder().build();
        return ParsedQuery.builder()
                .originalQuery(query.getQuery())
                .category(query.getCategory())
                .brand(query.getBrand())
                .minPrice(query.getMinPrice())
                .budget(query.getMaxPrice())
                .maxPrice(query.getMaxPrice())
                .minRating(query.getMinRating())
                .features(query.getFeatures())
                .excludedFeatures(query.getExcludedFeatures())
                .sortPreference(query.getSortPreference())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Result record — bundles products + metadata for callers that want both
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Immutable result of a discovery run: ranked products plus collection metadata.
     */
    public record DiscoveryResult(List<Product> rankedProducts, DiscoveryMetadata metadata) {}
}
