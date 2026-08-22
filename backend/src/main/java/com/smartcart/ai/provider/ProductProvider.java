package com.smartcart.ai.provider;

import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.ProductSource;
import com.smartcart.ai.dto.ProductDiscoveryQuery;

import java.util.List;

/**
 * Strategy interface for external or internal product providers.
 *
 * <p>Implementations can source products from local catalog, future retail APIs,
 * web feeds, or affiliate networks.
 */
public interface ProductProvider {

    /**
     * Unique source identification for this provider (e.g., LOCAL, RETAILER_A, PRODUCT_API).
     */
    ProductSource getSource();

    /**
     * Discovers products from this provider matching the given criteria.
     *
     * @param query discovery constraints including filters and pagination request
     * @return non-null list of products returned from this provider
     */
    List<Product> discover(ProductDiscoveryQuery query);
}
