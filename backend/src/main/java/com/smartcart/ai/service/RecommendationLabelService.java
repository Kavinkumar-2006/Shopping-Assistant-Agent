package com.smartcart.ai.service;

import com.smartcart.ai.dto.ParsedQuery;
import com.smartcart.ai.dto.ProductScore;
import com.smartcart.ai.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to assign recommendation labels to products (Best Overall, Best Value, etc.).
 * Labels are calculated from actual product data and scores, not assigned randomly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationLabelService {

    private final ProductScoringService scoringService;

    /**
     * Assigns recommendation labels to a list of products.
     * Each product can have at most one primary label.
     * Labels are exclusive - only one product gets each label type.
     *
     * @param products list of products to label
     * @param parsed   parsed query context
     * @return map of product ID to recommendation label
     */
    public Map<String, String> assignLabels(List<Product> products, ParsedQuery parsed) {
        Map<String, String> labels = new HashMap<>();

        if (products == null || products.isEmpty()) {
            return labels;
        }

        // Calculate scores for all products
        Map<String, ProductScore> scoreMap = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> scoringService.calculateScore(p, parsed)
                ));

        Set<String> usedLabels = new HashSet<>();

        // 1. Best for [Use Case] — FIRST so use-case-optimized products get their specific label
        //    before being consumed by generic "Best Overall"
        if (parsed.getUseCase() != null) {
            Product bestForUseCase = products.stream()
                    .filter(p -> !usedLabels.contains(p.getId()))
                    .max(Comparator.comparingDouble(p -> scoreMap.get(p.getId()).getUseCaseScore()))
                    .orElse(null);
            if (bestForUseCase != null && scoreMap.get(bestForUseCase.getId()).getUseCaseScore() >= 10.0) {
                String label = "Best for " + capitalize(parsed.getUseCase());
                labels.put(bestForUseCase.getId(), label);
                usedLabels.add(bestForUseCase.getId());
            }
        }

        // 2. Best Overall (highest overall score among remaining products)
        Product bestOverall = products.stream()
                .filter(p -> !usedLabels.contains(p.getId()))
                .max(Comparator.comparingDouble(p -> scoreMap.get(p.getId()).getOverallScore()))
                .orElse(null);
        if (bestOverall != null) {
            labels.put(bestOverall.getId(), "Best Overall");
            usedLabels.add(bestOverall.getId());
        }

        // 3. Best Value (highest value score)
        Product bestValue = products.stream()
                .filter(p -> !usedLabels.contains(p.getId()))
                .max(Comparator.comparingDouble(p -> scoreMap.get(p.getId()).getValueScore()))
                .orElse(null);
        if (bestValue != null) {
            labels.put(bestValue.getId(), "Best Value");
            usedLabels.add(bestValue.getId());
        }

        // 4. Best Budget (lowest price with good rating >= 4.0)
        Product bestBudget = products.stream()
                .filter(p -> !usedLabels.contains(p.getId()))
                .filter(p -> p.getRating() >= 4.0)
                .min(Comparator.comparingLong(Product::getPrice))
                .orElse(null);
        if (bestBudget != null) {
            labels.put(bestBudget.getId(), "Best Budget");
            usedLabels.add(bestBudget.getId());
        }

        // 5. Best Rated (highest rating, min 100 reviews for credibility)
        Product bestRated = products.stream()
                .filter(p -> !usedLabels.contains(p.getId()))
                .filter(p -> p.getReviewCount() >= 100)
                .max(Comparator.comparingDouble(Product::getRating))
                .orElse(null);
        if (bestRated != null) {
            labels.put(bestRated.getId(), "Best Rated");
            usedLabels.add(bestRated.getId());
        }

        // 6. Best Premium (highest price with excellent rating >= 4.5, above 70% of budget)
        if (parsed.getBudget() != null && parsed.getBudget() > 50000) {
            Product bestPremium = products.stream()
                    .filter(p -> !usedLabels.contains(p.getId()))
                    .filter(p -> p.getRating() >= 4.5)
                    .filter(p -> p.getPrice() >= parsed.getBudget() * 0.7)
                    .max(Comparator.comparingLong(Product::getPrice))
                    .orElse(null);
            if (bestPremium != null) {
                labels.put(bestPremium.getId(), "Best Premium");
                usedLabels.add(bestPremium.getId());
            }
        }

        // 7. Most Popular (highest review count, only if notably popular)
        Product mostPopular = products.stream()
                .filter(p -> !usedLabels.contains(p.getId()))
                .max(Comparator.comparingInt(Product::getReviewCount))
                .orElse(null);
        if (mostPopular != null && mostPopular.getReviewCount() > 10000) {
            labels.put(mostPopular.getId(), "Most Popular");
            usedLabels.add(mostPopular.getId());
        }

        log.info("Assigned {} recommendation labels to products", labels.size());
        return labels;
    }

    /**
     * Gets the recommendation label for a specific product.
     */
    public String getLabel(String productId, Map<String, String> labelMap) {
        return labelMap.getOrDefault(productId, null);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
