package com.smartcart.ai.dto;

import com.smartcart.ai.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Session-scoped, serializable shopping preferences. It deliberately contains only
 * recommendation inputs and product references, never user identity or credentials. */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingSessionContext {
    private String category;
    private Long minPrice;
    private Long maxPrice;
    private String useCase;
    private String brand;
    @Builder.Default private List<String> excludedBrands = new ArrayList<>();
    @Builder.Default private List<String> requiredFeatures = new ArrayList<>();
    private Double minRating;
    private String preferredOperatingSystem;
    private com.smartcart.ai.entity.SortPreference sortPreference;
    @Builder.Default private List<String> keywords = new ArrayList<>();
    @Builder.Default private List<String> rejectedProductIds = new ArrayList<>();
    @Builder.Default private List<Product> previousRecommendations = new ArrayList<>();
    @Builder.Default private List<Product> comparedProducts = new ArrayList<>();
    private String awaitingField;
}
