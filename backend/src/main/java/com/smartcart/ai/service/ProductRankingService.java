package com.smartcart.ai.service;

import com.smartcart.ai.dto.ParsedQuery;
import com.smartcart.ai.dto.ProductScore;
import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.SortPreference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dedicated service to score and rank products using multiple relevance signals or sorting parameters.
 * Enhanced with intelligent multi-factor scoring, value-for-money, and result diversity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductRankingService {

    private final ProductScoringService scoringService;

    /**
     * Ranks the list of filtered products.
     *
     * @param products list of filtered products
     * @param parsed   parsed query constraints containing sort preference
     * @return sorted list of products
     */
    public List<Product> rank(List<Product> products, ParsedQuery parsed) {
        if (products == null || products.isEmpty()) {
            return new ArrayList<>();
        }

        SortPreference sort = parsed.getSortPreference();
        if (sort != null) {
            log.info("Sorting products by preference: {}", sort);
            switch (sort) {
                case PRICE_LOW_TO_HIGH:
                    return products.stream()
                            .sorted(Comparator.comparingLong(Product::getPrice))
                            .collect(Collectors.toList());
                case PRICE_HIGH_TO_LOW:
                    return products.stream()
                            .sorted(Comparator.comparingLong(Product::getPrice).reversed())
                            .collect(Collectors.toList());
                case RATING:
                    return products.stream()
                            .sorted(Comparator.comparingDouble(Product::getRating).reversed())
                            .collect(Collectors.toList());
                case POPULARITY:
                    return products.stream()
                            .sorted(Comparator.comparingInt(Product::getReviewCount).reversed())
                            .collect(Collectors.toList());
                case VALUE:
                    return rankByValue(products, parsed);
                case RELEVANCE:
                default:
                    break;
            }
        }

        log.info("Ranking products by intelligent multi-factor relevance scoring");
        List<Product> ranked = rankByRelevance(products, parsed);
        
        // Apply diversity if no explicit brand preference
        if (parsed.getBrand() == null && products.size() > 5) {
            ranked = applyDiversity(ranked, parsed);
        }
        
        return ranked;
    }

    /**
     * Ranks products by intelligent relevance scoring.
     */
    private List<Product> rankByRelevance(List<Product> products, ParsedQuery parsed) {
        // Calculate scores for all products
        Map<String, ProductScore> scoreMap = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> scoringService.calculateScore(p, parsed)
                ));

        // Sort by overall score (descending)
        return products.stream()
                .sorted(Comparator.comparingDouble((Product p) -> 
                        scoreMap.get(p.getId()).getOverallScore()).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Ranks products by value for money.
     */
    private List<Product> rankByValue(List<Product> products, ParsedQuery parsed) {
        log.info("Ranking products by value for money");
        
        Map<String, ProductScore> scoreMap = products.stream()
                .collect(Collectors.toMap(
                        Product::getId,
                        p -> scoringService.calculateScore(p, parsed)
                ));

        return products.stream()
                .sorted(Comparator.comparingDouble((Product p) -> 
                        scoreMap.get(p.getId()).getValueScore()).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Applies controlled diversity to ranking results.
     * Prevents 3+ consecutive products from the same brand.
     * Respects explicit user preferences — brand filter means no diversity applied.
     */
    private List<Product> applyDiversity(List<Product> ranked, ParsedQuery parsed) {
        if (ranked.size() <= 3) {
            return ranked;
        }

        List<Product> diversified = new ArrayList<>();
        List<Product> deferred = new ArrayList<>();

        for (Product product : ranked) {
            int size = diversified.size();

            // Always take first 2 products without diversity check
            if (size < 2) {
                diversified.add(product);
                continue;
            }

            String brand = product.getBrand() != null ? product.getBrand() : "";
            String prevBrand1 = diversified.get(size - 1).getBrand();
            String prevBrand2 = diversified.get(size - 2).getBrand();

            // If last 2 are same brand and this is also same brand — defer it
            if (brand.equals(prevBrand1) && brand.equals(prevBrand2)) {
                deferred.add(product);
            } else {
                diversified.add(product);
            }
        }

        // Append all deferred products at the end
        diversified.addAll(deferred);

        log.info("Applied diversity: {} products deferred for brand distribution", deferred.size());
        return diversified;
    }

    /**
     * Calculates score for a single product (for testing/debugging).
     */
    public ProductScore calculateProductScore(Product product, ParsedQuery parsed) {
        return scoringService.calculateScore(product, parsed);
    }

    /**
     * Legacy method for backward compatibility.
     * @deprecated Use rankByRelevance or ProductScoringService directly
     */
    @Deprecated
    public double calculateRelevanceScore(Product p, ParsedQuery parsed) {
        return scoringService.calculateScore(p, parsed).getOverallScore();
    }
}

