package com.smartcart.ai.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcart.ai.entity.Product;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ProductSearchProvider implementation that sources products from
 * the local {@code products.json} classpath resource.
 *
 * <p>The catalog is loaded once at application startup ({@link PostConstruct})
 * and held in memory. All search operations are performed in-memory using
 * Java Streams — no database required.
 *
 * <p>Supported filters:
 * <ul>
 *   <li>Free-text search across name, brand, category, description, and tags (case-insensitive)</li>
 *   <li>Exact category match (case-insensitive)</li>
 *   <li>Maximum price ceiling (inclusive)</li>
 *   <li>Minimum rating threshold (inclusive)</li>
 * </ul>
 */
import com.smartcart.ai.dto.ProductDiscoveryQuery;
import com.smartcart.ai.entity.ProductSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ProductProvider and ProductSearchProvider implementation that sources products from
 * the local {@code products.json} classpath resource.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalCatalogProvider implements ProductSearchProvider, ProductProvider {

    private final ObjectMapper objectMapper;

    /** In-memory product catalog, loaded once at startup. */
    private List<Product> catalog = Collections.emptyList();

    // ─────────────────────────────────────────────────────────────────────────
    // Startup
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Loads products.json from the classpath into memory.
     * Called automatically by Spring after the bean is constructed.
     */
    @PostConstruct
    public void loadCatalog() {
        try {
            InputStream stream = new ClassPathResource("products.json").getInputStream();
            catalog = objectMapper.readValue(stream, new TypeReference<List<Product>>() {});
            log.info("LocalCatalogProvider: loaded {} products from products.json", catalog.size());
        } catch (IOException e) {
            log.error("LocalCatalogProvider: failed to load products.json — {}", e.getMessage());
            throw new RuntimeException("Could not load local product catalog", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ProductProvider implementation
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ProductSource getSource() {
        return ProductSource.LOCAL;
    }

    @Override
    public List<Product> discover(ProductDiscoveryQuery query) {
        if (query == null) {
            return catalog.stream().map(this::enrichLocalMetadata).collect(Collectors.toList());
        }

        return catalog.stream()
                .filter(p -> passesTextSearch(p, query.getQuery()))
                .filter(p -> passesCategory(p, query.getCategory()))
                .filter(p -> passesBrand(p, query.getBrand()))
                .filter(p -> passesMaxPrice(p, query.getMaxPrice()))
                .filter(p -> passesMinPrice(p, query.getMinPrice()))
                .filter(p -> passesMinRating(p, query.getMinRating()))
                .map(this::enrichLocalMetadata)
                .collect(Collectors.toList());
    }

    private Product enrichLocalMetadata(Product p) {
        if (p == null) return null;
        if (p.getSource() == null) p.setSource(ProductSource.LOCAL);
        if (p.getCurrency() == null) p.setCurrency("INR");
        if (p.getAvailability() == null) p.setAvailability(true);
        if (p.getStoreName() == null) p.setStoreName("ShopSmart Local Catalog");
        if (p.getSourceProductId() == null) p.setSourceProductId(p.getId());
        // Local catalog data is not a retailer listing. Do not manufacture a product URL.
        p.setProductUrl(null);
        p.setSourceProductUrl(null);
        p.setSourceUrl(null);
        if (p.getFetchedTime() == null) p.setFetchedTime(Instant.now().toString());
        return p;
    }

    private boolean passesBrand(Product p, String brand) {
        if (brand == null || brand.isBlank()) return true;
        return p.getBrand() != null && p.getBrand().equalsIgnoreCase(brand);
    }

    private boolean passesMinPrice(Product p, Long minPrice) {
        if (minPrice == null) return true;
        return p.getPrice() >= minPrice;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ProductSearchProvider implementation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>All filter parameters are optional (nullable). Filters are combined with AND semantics:
     * a product must pass all non-null filters to be included in the result.
     */
    @Override
    public List<Product> search(String query, String category, Long maxPrice, Double minRating) {
        return catalog.stream()
                .filter(p -> passesTextSearch(p, query))
                .filter(p -> passesCategory(p, category))
                .filter(p -> passesMaxPrice(p, maxPrice))
                .filter(p -> passesMinRating(p, minRating))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private filter predicates
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Case-insensitive text search across name, brand, category, description, and tags.
     * Returns true if {@code query} is null/blank (no text filter applied).
     */
    private boolean passesTextSearch(Product p, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.toLowerCase();
        if (matches(p.getName(), q))        return true;
        if (matches(p.getBrand(), q))       return true;
        if (matches(p.getCategory(), q))    return true;
        if (matches(p.getDescription(), q)) return true;
        if (p.getTags() != null) {
            return p.getTags().stream()
                    .anyMatch(tag -> tag != null && tag.toLowerCase().contains(q));
        }
        return false;
    }

    /** Returns true if {@code field} is non-null and contains the lowercase {@code q}. */
    private boolean matches(String field, String q) {
        return field != null && field.toLowerCase().contains(q);
    }

    /**
     * Exact category match (case-insensitive).
     * Returns true if {@code category} is null (no category filter applied).
     */
    private boolean passesCategory(Product p, String category) {
        if (category == null || category.isBlank()) {
            return true;
        }
        return p.getCategory() != null && p.getCategory().equalsIgnoreCase(category);
    }

    /**
     * Price must be ≤ maxPrice.
     * Returns true if {@code maxPrice} is null (no price filter applied).
     */
    private boolean passesMaxPrice(Product p, Long maxPrice) {
        if (maxPrice == null) {
            return true;
        }
        return p.getPrice() <= maxPrice;
    }

    /**
     * Rating must be ≥ minRating.
     * Returns true if {@code minRating} is null (no rating filter applied).
     */
    private boolean passesMinRating(Product p, Double minRating) {
        if (minRating == null) {
            return true;
        }
        return p.getRating() >= minRating;
    }
}
