package com.smartcart.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartcart.ai.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Outbound DTO returned by {@code GET /api/products/search}.
 *
 * <p>Contains the original query string, the total number of results,
 * pagination details, and the list of matching products.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductSearchResponse {

    /** The free-text search term supplied by the caller (may be null or blank). */
    private String query;

    /** Total number of products returned by the search. */
    private int totalResults;

    /** The matching products. Never null; may be an empty list. */
    private List<Product> products;

    // ── Phase 4 Pagination Extensions ────────────────────────────────────────

    private Integer page;
    private Integer pageSize;
    private Integer totalPages;
    private Boolean hasNextPage;
    private Boolean hasPreviousPage;
}

