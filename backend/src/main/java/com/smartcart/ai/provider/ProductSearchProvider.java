package com.smartcart.ai.provider;

import com.smartcart.ai.entity.Product;

import java.util.List;

/**
 * Strategy interface for product search providers.
 *
 * <p>Implementations can source products from different backends:
 * local catalog, Amazon-compatible feeds, Flipkart-compatible feeds, etc.
 *
 * <p>All parameters are optional (nullable). When null, the corresponding
 * filter is not applied.
 */
public interface ProductSearchProvider {

    /**
     * Search for products matching the given criteria.
     *
     * @param query     free-text search term (matched against name, brand, category, description, tags);
     *                  null or blank means no text filter
     * @param category  exact category filter (case-insensitive); null means no category filter
     * @param maxPrice  maximum price ceiling in ₹ (inclusive); null means no price filter
     * @param minRating minimum rating threshold (inclusive); null means no rating filter
     * @return a non-null list of matching products (may be empty)
     */
    List<Product> search(String query, String category, Long maxPrice, Double minRating);
}
