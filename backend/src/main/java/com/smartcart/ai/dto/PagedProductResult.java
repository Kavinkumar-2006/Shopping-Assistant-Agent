package com.smartcart.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.smartcart.ai.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Paginated wrapper for product discovery responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedProductResult {

    @Builder.Default
    private List<Product> products = new ArrayList<>();

    private int page;
    private int pageSize;
    private int totalMatches;
    private int totalPages;
    private boolean hasNextPage;
    private boolean hasPreviousPage;

    /**
     * Factory method to create a PagedProductResult from a list of items and pagination params.
     */
    public static PagedProductResult create(List<Product> allRankedProducts, int page, int pageSize) {
        if (allRankedProducts == null || allRankedProducts.isEmpty()) {
            return PagedProductResult.builder()
                    .products(new ArrayList<>())
                    .page(page)
                    .pageSize(pageSize)
                    .totalMatches(0)
                    .totalPages(0)
                    .hasNextPage(false)
                    .hasPreviousPage(false)
                    .build();
        }

        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        int totalMatches = allRankedProducts.size();
        int totalPages = (int) Math.ceil((double) totalMatches / safePageSize);

        int fromIndex = (safePage - 1) * safePageSize;
        List<Product> pageItems;
        if (fromIndex >= totalMatches) {
            pageItems = new ArrayList<>();
        } else {
            int toIndex = Math.min(fromIndex + safePageSize, totalMatches);
            pageItems = new ArrayList<>(allRankedProducts.subList(fromIndex, toIndex));
        }

        return PagedProductResult.builder()
                .products(pageItems)
                .page(safePage)
                .pageSize(safePageSize)
                .totalMatches(totalMatches)
                .totalPages(totalPages)
                .hasNextPage(safePage < totalPages)
                .hasPreviousPage(safePage > 1)
                .build();
    }
}
