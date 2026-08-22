package com.smartcart.ai.service;

import com.smartcart.ai.dto.ParsedQuery;
import com.smartcart.ai.dto.ProductScore;
import com.smartcart.ai.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service for calculating detailed product scores with transparent breakdown.
 * Provides individual scoring components for debugging, testing, and explanation.
 */
@Slf4j
@Service
public class ProductScoringService {

    // Scoring weights - tuned for balanced recommendations
    private static final double WEIGHT_RELEVANCE = 1.0;      // Base weight for text matching
    private static final double WEIGHT_BUDGET = 1.0;         // Budget fit importance
    private static final double WEIGHT_RATING = 1.0;         // Rating quality importance
    private static final double WEIGHT_POPULARITY = 0.4;     // Review count importance (reduced to avoid bias)
    private static final double WEIGHT_USE_CASE = 1.5;       // Use case match importance (increased)
    private static final double WEIGHT_VALUE = 1.2;          // Value for money importance (increased)
    private static final double WEIGHT_BRAND = 1.5;          // Brand preference importance (increased)
    private static final double WEIGHT_SPEC = 0.8;           // Specification quality importance

    // Maximum score caps for each component
    private static final double MAX_RELEVANCE = 50.0;
    private static final double MAX_BUDGET = 20.0;
    private static final double MAX_RATING = 75.0;
    private static final double MAX_POPULARITY = 10.0;
    private static final double MAX_USE_CASE = 30.0;
    private static final double MAX_VALUE = 25.0;
    private static final double MAX_BRAND = 30.0;
    private static final double MAX_SPEC = 20.0;

    /**
     * Calculates comprehensive score breakdown for a product.
     *
     * @param product the product to score
     * @param parsed  parsed query with user requirements
     * @return detailed score breakdown
     */
    public ProductScore calculateScore(Product product, ParsedQuery parsed) {
        double relevance = calculateRelevanceScore(product, parsed);
        double budget = calculateBudgetScore(product, parsed);
        double rating = calculateRatingScore(product);
        double popularity = calculatePopularityScore(product);
        double useCase = calculateUseCaseScore(product, parsed);
        double value = calculateValueScore(product, parsed);
        double brand = calculateBrandScore(product, parsed);
        double spec = calculateSpecScore(product, parsed);

        // Apply weights and sum
        double overall = 
            (relevance * WEIGHT_RELEVANCE) +
            (budget * WEIGHT_BUDGET) +
            (rating * WEIGHT_RATING) +
            (popularity * WEIGHT_POPULARITY) +
            (useCase * WEIGHT_USE_CASE) +
            (value * WEIGHT_VALUE) +
            (brand * WEIGHT_BRAND) +
            (spec * WEIGHT_SPEC);

        // Calculate match percentage (0-100%)
        double maxPossible = 
            (MAX_RELEVANCE * WEIGHT_RELEVANCE) +
            (MAX_BUDGET * WEIGHT_BUDGET) +
            (MAX_RATING * WEIGHT_RATING) +
            (MAX_POPULARITY * WEIGHT_POPULARITY) +
            (MAX_USE_CASE * WEIGHT_USE_CASE) +
            (MAX_VALUE * WEIGHT_VALUE) +
            (MAX_BRAND * WEIGHT_BRAND) +
            (MAX_SPEC * WEIGHT_SPEC);

        double matchPercentage = (overall / maxPossible) * 100.0;

        return ProductScore.builder()
                .productId(product.getId())
                .overallScore(overall)
                .relevanceScore(relevance)
                .budgetScore(budget)
                .ratingScore(rating)
                .popularityScore(popularity)
                .useCaseScore(useCase)
                .valueScore(value)
                .brandScore(brand)
                .specScore(spec)
                .matchPercentage(Math.min(100.0, matchPercentage))
                .build();
    }

    /**
     * Text relevance score based on keyword matching (0-50 pts).
     */
    private double calculateRelevanceScore(Product product, ParsedQuery parsed) {
        double score = 0.0;

        // Match against product name using keywords
        if (parsed.getKeywords() != null) {
            for (String keyword : parsed.getKeywords()) {
                if (product.getName() != null && product.getName().toLowerCase().contains(keyword.toLowerCase())) {
                    score += 6.0;
                }
            }
        }

        // Use case keyword match against name or description
        if (parsed.getUseCase() != null) {
            if (product.getName() != null && product.getName().toLowerCase().contains(parsed.getUseCase().toLowerCase())) {
                score += 5.0;
            }
            if (product.getDescription() != null && product.getDescription().toLowerCase().contains(parsed.getUseCase().toLowerCase())) {
                score += 8.0;
            }
        }

        // Category match against product category
        if (parsed.getCategory() != null && product.getCategory() != null
                && product.getCategory().equalsIgnoreCase(parsed.getCategory())) {
            score += 5.0;
        }

        // Match against features
        if (parsed.getFeatures() != null && !parsed.getFeatures().isEmpty()) {
            long featMatches = 0;
            for (String feat : parsed.getFeatures()) {
                String featLower = feat.toLowerCase();
                boolean match = false;

                if (product.getSpecs() != null) {
                    match = product.getSpecs().values().stream()
                            .anyMatch(val -> val != null && val.toLowerCase().contains(featLower));
                }

                if (!match && product.getTags() != null) {
                    match = product.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(featLower));
                }

                if (!match && product.getName() != null && product.getName().toLowerCase().contains(featLower)) {
                    match = true;
                }

                if (match) {
                    featMatches++;
                }
            }
            score += featMatches * 5.0;
        }

        return Math.min(score, MAX_RELEVANCE);
    }

    /**
     * Budget fit score with intelligent positioning (0-20 pts).
     * Prefers products near an "ideal" budget (70-85% of max budget).
     * Penalizes products that are too cheap (may lack quality) or at max budget (no savings).
     */
    private double calculateBudgetScore(Product product, ParsedQuery parsed) {
        if (parsed.getBudget() == null || parsed.getBudget() <= 0) {
            return MAX_BUDGET * 0.5; // Neutral score when no budget specified
        }

        long price = product.getPrice();
        long budget = parsed.getBudget();

        // Product above budget - penalize heavily
        if (price > budget) {
            long excess = price - budget;
            double penalty = (double) excess / budget;
            return Math.max(0, MAX_BUDGET * (1.0 - penalty * 2.0));
        }

        // Calculate position within budget (0.0 to 1.0)
        double position = (double) price / budget;

        // Ideal range: 70-85% of budget (best value sweet spot)
        if (position >= 0.70 && position <= 0.85) {
            return MAX_BUDGET; // Perfect budget fit
        }

        // Good range: 50-95% of budget
        if (position >= 0.50 && position <= 0.95) {
            return MAX_BUDGET * 0.85;
        }

        // Too cheap (below 30% of budget) - may lack features
        if (position < 0.30) {
            return MAX_BUDGET * 0.6;
        }

        // At max budget (95-100%) - no savings
        if (position > 0.95) {
            return MAX_BUDGET * 0.7;
        }

        // Other positions get proportional score
        return MAX_BUDGET * 0.75;
    }

    /**
     * Rating quality score (0-75 pts).
     * Heavily weighted as ratings are strong quality indicators.
     */
    private double calculateRatingScore(Product product) {
        return product.getRating() * (MAX_RATING / 5.0);
    }

    /**
     * Popularity score based on review count (0-10 pts).
     * Uses logarithmic scale to prevent mega-popular products from dominating.
     */
    private double calculatePopularityScore(Product product) {
        if (product.getReviewCount() <= 0) {
            return 0.0;
        }
        // Log scale: 1000 reviews ≈ 3 pts, 10000 reviews ≈ 4 pts, 50000 reviews ≈ 4.7 pts
        return Math.min(MAX_POPULARITY, Math.log10(product.getReviewCount()) * 2.5);
    }

    /**
     * Use case match score with specification intelligence (0-30 pts).
     * Checks tags AND validates specifications match use case requirements.
     */
    private double calculateUseCaseScore(Product product, ParsedQuery parsed) {
        double score = 0.0;

        if (parsed.getUseCase() == null) {
            return MAX_USE_CASE * 0.5; // Neutral when no use case specified
        }

        // Tag match
        if (product.getTags() != null && product.getTags().contains(parsed.getUseCase())) {
            score += 15.0;
        }

        // Specification-based use case validation
        Map<String, String> specs = product.getSpecs();
        if (specs != null) {
            score += scoreUseCaseSpecs(parsed.getUseCase(), specs, parsed.getCategory());
        }

        return Math.min(score, MAX_USE_CASE);
    }

    /**
     * Scores specifications based on use case requirements.
     */
    private double scoreUseCaseSpecs(String useCase, Map<String, String> specs, String category) {
        double score = 0.0;

        if ("coding".equals(useCase) || "programming".equals(useCase)) {
            // Coding needs: good RAM, SSD, decent processor
            String ram = specs.get("RAM");
            if (ram != null) {
                if (ram.contains("16") || ram.contains("32") || ram.contains("64")) score += 5.0;
                else if (ram.contains("8")) score += 3.0;
            }

            String storage = specs.get("Storage");
            if (storage != null && storage.toUpperCase().contains("SSD")) score += 4.0;

            String processor = specs.get("Processor");
            if (processor != null && (processor.contains("i7") || processor.contains("i9") || processor.contains("Ryzen 7") || processor.contains("Ryzen 9") || processor.contains("M1") || processor.contains("M2"))) {
                score += 3.0;
            }
        }

        if ("gaming".equals(useCase)) {
            // Gaming needs: high RAM, dedicated graphics (implied by tags), good processor
            String ram = specs.get("RAM");
            if (ram != null && (ram.contains("16") || ram.contains("32") || ram.contains("64"))) {
                score += 4.0;
            }

            String processor = specs.get("Processor");
            if (processor != null && (processor.contains("i7") || processor.contains("i9") || processor.contains("Ryzen 7") || processor.contains("Ryzen 9"))) {
                score += 4.0;
            }
        }

        if ("camera".equals(useCase) || "photography".equals(useCase)) {
            // Photography needs: good camera specs
            String camera = specs.get("Camera");
            if (camera != null) {
                if (camera.contains("108MP") || camera.contains("64MP") || camera.contains("50MP")) score += 6.0;
                else if (camera.contains("48MP")) score += 4.0;
            }
        }

        if ("battery".equals(useCase)) {
            // Battery use case: high capacity
            String battery = specs.get("Battery");
            if (battery != null) {
                if (battery.contains("6000") || battery.contains("5000")) score += 6.0;
                else if (battery.contains("4000")) score += 3.0;
            }
        }

        return score;
    }

    /**
     * Value for money score (0-25 pts).
     * Calculates quality-to-price ratio, with diminishing returns for very cheap products
     * (which may lack quality despite a good rating/price ratio).
     */
    private double calculateValueScore(Product product, ParsedQuery parsed) {
        if (product.getPrice() <= 0) return 0;

        // Normalize price to 10k units
        double priceIn10k = product.getPrice() / 10000.0;

        // Apply soft price floor: products below ₹15k get capped value
        // to avoid ultra-cheap products dominating with tiny denominator
        double effectivePriceIn10k = Math.max(priceIn10k, 1.5);

        // Rating-to-effective-cost ratio
        double ratingValueRatio = (product.getRating() * product.getRating()) / effectivePriceIn10k;

        // Review confidence bonus: more reviews = more reliable value signal
        double reviewConfidence;
        if (product.getReviewCount() > 10000) reviewConfidence = 3.5;
        else if (product.getReviewCount() > 3000) reviewConfidence = 2.5;
        else if (product.getReviewCount() > 1000) reviewConfidence = 1.5;
        else if (product.getReviewCount() > 100) reviewConfidence = 0.8;
        else reviewConfidence = 0.3;

        // Specification completeness bonus (more specs = more transparent value)
        double specBonus = 0.0;
        if (product.getSpecs() != null && !product.getSpecs().isEmpty()) {
            specBonus = Math.min(3.0, product.getSpecs().size() * 0.5);
        }

        double totalValue = (ratingValueRatio * 2.5) + reviewConfidence + specBonus;

        return Math.min(totalValue, MAX_VALUE);
    }

    /**
     * Brand preference score (0-30 pts).
     * Heavily rewards exact brand match when user specifies brand.
     */
    private double calculateBrandScore(Product product, ParsedQuery parsed) {
        if (parsed.getBrand() == null) {
            return MAX_BRAND * 0.5; // Neutral when no brand preference
        }

        if (product.getBrand() != null && product.getBrand().equalsIgnoreCase(parsed.getBrand())) {
            return MAX_BRAND; // Full score for brand match
        }

        return 0.0; // No score for non-matching brand
    }

    /**
     * Specification quality score (0-20 pts).
     * Rewards products with comprehensive specifications.
     */
    private double calculateSpecScore(Product product, ParsedQuery parsed) {
        if (product.getSpecs() == null || product.getSpecs().isEmpty()) {
            return 0.0;
        }

        // Score based on number and quality of specs
        int specCount = product.getSpecs().size();
        double score = Math.min(15.0, specCount * 2.0);

        // Bonus for having key specs populated
        if (product.getSpecs().containsKey("Processor")) score += 1.0;
        if (product.getSpecs().containsKey("RAM")) score += 1.0;
        if (product.getSpecs().containsKey("Storage")) score += 1.0;
        if (product.getSpecs().containsKey("Display")) score += 1.0;
        if (product.getSpecs().containsKey("Battery")) score += 1.0;

        return Math.min(score, MAX_SPEC);
    }
}
