package com.smartcart.ai.service;

import com.smartcart.ai.dto.DiscoveryMetadata;
import com.smartcart.ai.dto.ProductDiscoveryQuery;
import com.smartcart.ai.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application-layer service for product search.
 *
 * <p>Delegates discovery execution to {@link ProductDiscoveryService},
 * leveraging the scalable multi-provider architecture.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductDiscoveryService productDiscoveryService;

    /**
     * Searches for products matching the given criteria using {@link ProductDiscoveryService}.
     * Returns only the product list — metadata is discarded (backward-compatible).
     */
    public List<Product> search(String query, String category, Long maxPrice, Double minRating) {
        log.info("ProductSearchService.search — query='{}', category='{}', maxPrice={}, minRating={}",
                query, category, maxPrice, minRating);

        ProductDiscoveryQuery discoveryQuery = ProductDiscoveryQuery.builder()
                .query(query)
                .category(category)
                .maxPrice(maxPrice)
                .minRating(minRating)
                .build();

        List<Product> results = productDiscoveryService.discoverAll(discoveryQuery);

        log.info("ProductSearchService.search — {} result(s) returned", results.size());
        return results;
    }

    /**
     * Same as {@link #search} but also returns {@link DiscoveryMetadata} collected
     * during the provider aggregation. Used by the agent to include transparency
     * information in the recommendation response.
     */
    public SearchWithMetadataResult searchWithMetadata(String query, String category,
                                                       Long maxPrice, Double minRating) {
        ProductDiscoveryQuery discoveryQuery = ProductDiscoveryQuery.builder()
                .query(query)
                .category(category)
                .maxPrice(maxPrice)
                .minRating(minRating)
                .build();

        ProductDiscoveryService.DiscoveryResult dr =
                productDiscoveryService.discoverWithMetadata(discoveryQuery);

        log.info("ProductSearchService.searchWithMetadata — {} result(s) from {} providers",
                dr.rankedProducts().size(), dr.metadata().getProvidersQueried());
        return new SearchWithMetadataResult(dr.rankedProducts(), dr.metadata());
    }

    /** Bundles a product list with its discovery metadata. */
    public record SearchWithMetadataResult(List<Product> products, DiscoveryMetadata metadata) {}
}
