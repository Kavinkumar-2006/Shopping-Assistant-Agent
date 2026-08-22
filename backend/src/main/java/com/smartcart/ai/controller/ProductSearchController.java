package com.smartcart.ai.controller;

import com.smartcart.ai.dto.PagedProductResult;
import com.smartcart.ai.dto.ProductDiscoveryQuery;
import com.smartcart.ai.dto.ProductSearchResponse;
import com.smartcart.ai.service.ProductDiscoveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing product search and discovery endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductDiscoveryService productDiscoveryService;

    /**
     * Full-featured product discovery with optional filters and pagination.
     *
     * <p>Query parameters:
     * <ul>
     *   <li>{@code q}         — free-text search (name, brand, category, description, tags)</li>
     *   <li>{@code category}  — exact category filter (case-insensitive)</li>
     *   <li>{@code brand}     — exact brand filter (case-insensitive)</li>
     *   <li>{@code maxPrice}  — maximum price in ₹ (inclusive)</li>
     *   <li>{@code minRating} — minimum rating threshold (inclusive, 0.0–5.0)</li>
     *   <li>{@code page}      — 1-based page number (default 1)</li>
     *   <li>{@code pageSize}  — results per page (default 20)</li>
     * </ul>
     *
     * @return {@link ProductSearchResponse} with query echo, total count, pagination metadata, and product list
     */
    @GetMapping("/search")
    public ResponseEntity<ProductSearchResponse> search(
            @RequestParam(name = "q",         required = false) String query,
            @RequestParam(name = "category",  required = false) String category,
            @RequestParam(name = "brand",     required = false) String brand,
            @RequestParam(name = "maxPrice",  required = false) Long maxPrice,
            @RequestParam(name = "minRating", required = false) Double minRating,
            @RequestParam(name = "page",      required = false, defaultValue = "1") int page,
            @RequestParam(name = "pageSize",  required = false, defaultValue = "20") int pageSize) {

        log.info("GET /api/products/search — q='{}', category='{}', brand='{}', maxPrice={}, minRating={}, page={}, pageSize={}",
                query, category, brand, maxPrice, minRating, page, pageSize);

        ProductDiscoveryQuery discoveryQuery = ProductDiscoveryQuery.builder()
                .query(query)
                .category(category)
                .brand(brand)
                .maxPrice(maxPrice)
                .minRating(minRating)
                .page(page)
                .pageSize(pageSize)
                .build();

        PagedProductResult pagedResult = productDiscoveryService.discover(discoveryQuery);

        ProductSearchResponse response = ProductSearchResponse.builder()
                .query(query)
                .totalResults(pagedResult.getTotalMatches())
                .products(pagedResult.getProducts())
                .page(pagedResult.getPage())
                .pageSize(pagedResult.getPageSize())
                .totalPages(pagedResult.getTotalPages())
                .hasNextPage(pagedResult.isHasNextPage())
                .hasPreviousPage(pagedResult.isHasPreviousPage())
                .build();

        log.info("GET /api/products/search — returning {} product(s) (total matches: {})",
                pagedResult.getProducts().size(), pagedResult.getTotalMatches());
        return ResponseEntity.ok(response);
    }
}

