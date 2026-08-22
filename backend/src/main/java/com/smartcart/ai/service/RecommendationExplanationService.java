package com.smartcart.ai.service;

import com.smartcart.ai.dto.ParsedQuery;
import com.smartcart.ai.dto.ProductScore;
import com.smartcart.ai.entity.Product;
import com.smartcart.ai.util.PriceFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Service to generate transparent, data-driven explanation sentences for product recommendations.
 * Enhanced with specification-driven reasoning and value analysis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationExplanationService {

    private final PriceFormatter priceFormatter;
    private final ProductScoringService scoringService;

    /**
     * Generates list of explanation sentences for a list of products.
     *
     * @param products recommended products
     * @param parsed   parsed query intent/constraints
     * @return list of explanations matching the input products order
     */
    public List<String> generateExplanations(List<Product> products, ParsedQuery parsed) {
        List<String> explanations = new ArrayList<>();
        if (products == null || products.isEmpty()) {
            return explanations;
        }

        // Calculate scores for value-based explanations
        Map<String, ProductScore> scoreMap = new java.util.HashMap<>();
        for (Product p : products) {
            scoreMap.put(p.getId(), scoringService.calculateScore(p, parsed));
        }

        for (Product p : products) {
            explanations.add(buildExplanation(p, parsed, scoreMap.get(p.getId())));
        }

        return explanations;
    }

    private String buildExplanation(Product p, ParsedQuery parsed, ProductScore score) {
        StringBuilder sb = new StringBuilder();
        sb.append("**").append(p.getName()).append("** (").append(p.getBrand()).append(")");

        List<String> reasons = new ArrayList<>();

        // 1. Price Context with Budget Intelligence
        reasons.add(buildPriceReason(p, parsed));

        // 2. Specification Details
        String specReason = buildSpecificationReason(p, parsed);
        if (specReason != null && !specReason.isEmpty()) {
            reasons.add(specReason);
        }

        // 3. Use Case Alignment
        String useCaseReason = buildUseCaseReason(p, parsed);
        if (useCaseReason != null && !useCaseReason.isEmpty()) {
            reasons.add(useCaseReason);
        }

        // 4. Rating & Credibility
        reasons.add(buildRatingReason(p));

        // 5. Value for Money Analysis
        if (score.getValueScore() > 15.0) {
            reasons.add("offers excellent value for money");
        }

        // 6. Brand Preference Match
        if (parsed.getBrand() != null && p.getBrand() != null && 
            p.getBrand().equalsIgnoreCase(parsed.getBrand())) {
            reasons.add("matches your preferred brand (" + parsed.getBrand() + ")");
        }

        if (parsed.getUseCase() != null) {
            sb.append(" is a strong choice for **").append(parsed.getUseCase()).append("** because ");
        } else {
            sb.append(" is recommended because ");
        }

        sb.append(String.join(", ", reasons));
        if (!sb.toString().endsWith(".")) {
            sb.append(".");
        }

        return sb.toString().trim();
    }

    /**
     * Generates a concise top recommendation explanation based on scoring and requirements.
     */
    public String generateTopExplanation(Product top, ParsedQuery parsed) {
        if (top == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("Based on your ");
        if (parsed.getBudget() != null) {
            sb.append(priceFormatter.format(parsed.getBudget())).append(" budget");
        } else {
            sb.append("search preferences");
        }
        if (parsed.getUseCase() != null) {
            sb.append(" and **").append(parsed.getUseCase()).append("** requirement");
        }
        sb.append(", **").append(top.getName()).append("** is my top recommendation because it offers the best balance of ");

        Map<String, String> specs = top.getSpecs();
        List<String> keyHighlights = new ArrayList<>();
        if (specs != null) {
            if (specs.containsKey("Processor")) keyHighlights.add(specs.get("Processor") + " performance");
            if (specs.containsKey("RAM")) keyHighlights.add(specs.get("RAM") + " RAM");
            if (specs.containsKey("Storage")) keyHighlights.add(specs.get("Storage"));
            if (specs.containsKey("Camera")) keyHighlights.add(specs.get("Camera") + " camera");
            if (specs.containsKey("Battery")) keyHighlights.add(specs.get("Battery") + " battery");
        }
        if (keyHighlights.isEmpty()) {
            keyHighlights.add("rating (★ " + top.getRating() + ")");
            keyHighlights.add("price (" + priceFormatter.format(top.getPrice()) + ")");
        } else {
            keyHighlights.add("price");
        }

        sb.append(String.join(", ", keyHighlights)).append(".");
        return sb.toString();
    }

    /**
     * Generates a concise side-by-side textual comparison explanation between two or more products.
     */
    public String generateComparisonExplanation(List<Product> products, ParsedQuery parsed) {
        if (products == null || products.size() < 2) return "";
        Product p1 = products.get(0);
        Product p2 = products.get(1);

        StringBuilder sb = new StringBuilder();
        sb.append("Comparing **").append(p1.getName()).append("** (").append(priceFormatter.format(p1.getPrice())).append(") ")
          .append("and **").append(p2.getName()).append("** (").append(priceFormatter.format(p2.getPrice())).append("): ");

        List<String> highlights = new ArrayList<>();
        Map<String, String> s1 = p1.getSpecs();
        Map<String, String> s2 = p2.getSpecs();

        if (s1 != null && s2 != null) {
            if (s1.containsKey("RAM") && s2.containsKey("RAM") && !s1.get("RAM").equals(s2.get("RAM"))) {
                highlights.add("**" + p1.getBrand() + "** has " + s1.get("RAM") + " RAM vs " + s2.get("RAM") + " on **" + p2.getBrand() + "**");
            }
            if (s1.containsKey("Processor") && s2.containsKey("Processor") && !s1.get("Processor").equals(s2.get("Processor"))) {
                highlights.add("**" + p1.getBrand() + "** is powered by " + s1.get("Processor") + " while **" + p2.getBrand() + "** features " + s2.get("Processor"));
            }
            if (s1.containsKey("Camera") && s2.containsKey("Camera") && !s1.get("Camera").equals(s2.get("Camera"))) {
                highlights.add("**" + p1.getBrand() + "** offers a " + s1.get("Camera") + " camera system vs " + s2.get("Camera") + " on **" + p2.getBrand() + "**");
            }
        }

        if (p1.getPrice() < p2.getPrice()) {
            highlights.add("**" + p1.getName() + "** is the more budget-friendly option, saving " + priceFormatter.format(p2.getPrice() - p1.getPrice()));
        } else if (p2.getPrice() < p1.getPrice()) {
            highlights.add("**" + p2.getName() + "** is the more budget-friendly option, saving " + priceFormatter.format(p1.getPrice() - p2.getPrice()));
        }

        if (highlights.isEmpty()) {
            sb.append("**").append(p1.getName()).append("** offers a ★ ").append(p1.getRating()).append(" rating, while **")
              .append(p2.getName()).append("** offers a ★ ").append(p2.getRating()).append(" rating.");
        } else {
            sb.append(String.join(". ", highlights)).append(".");
        }

        return sb.toString();
    }

    private String buildPriceReason(Product p, ParsedQuery parsed) {
        StringBuilder reason = new StringBuilder();
        reason.append("it is priced at ").append(priceFormatter.format(p.getPrice()));

        if (parsed.getBudget() != null && parsed.getBudget() > 0) {
            long diff = parsed.getBudget() - p.getPrice();
            if (diff > 0) {
                long savingsPercent = (diff * 100) / parsed.getBudget();
                if (savingsPercent >= 30) {
                    reason.append(" (significantly under budget, saving you ")
                          .append(priceFormatter.format(diff)).append(")");
                } else if (savingsPercent >= 15) {
                    reason.append(" (comfortably within budget, saving ")
                          .append(priceFormatter.format(diff)).append(")");
                } else {
                    reason.append(" (within your ").append(priceFormatter.format(parsed.getBudget()))
                          .append(" budget)");
                }
            } else if (diff == 0) {
                reason.append(" (at your budget limit)");
            }
        }

        return reason.toString();
    }

    private String buildRatingReason(Product p) {
        StringBuilder reason = new StringBuilder();
        reason.append("it holds a ");
        
        if (p.getRating() >= 4.5) {
            reason.append("strong ★ ");
        } else if (p.getRating() >= 4.0) {
            reason.append("solid ★ ");
        } else {
            reason.append("★ ");
        }
        
        reason.append(p.getRating()).append(" rating");

        if (p.getReviewCount() > 10000) {
            reason.append(" across ").append(String.format("%,d", p.getReviewCount()))
                  .append("+ verified reviews (highly trusted)");
        } else if (p.getReviewCount() > 1000) {
            reason.append(" across ").append(String.format("%,d", p.getReviewCount()))
                  .append(" reviews");
        } else {
            reason.append(" (").append(p.getReviewCount()).append(" reviews)");
        }

        return reason.toString();
    }

    private String buildSpecificationReason(Product p, ParsedQuery parsed) {
        Map<String, String> specs = p.getSpecs();
        if (specs == null || specs.isEmpty()) {
            return null;
        }

        List<String> specExcerpts = new ArrayList<>();

        // Laptop/Computing specs
        if ("laptop".equals(parsed.getCategory()) || "tablet".equals(parsed.getCategory())) {
            if (specs.containsKey("RAM")) {
                String ram = specs.get("RAM");
                if (ram.contains("16") || ram.contains("32") || ram.contains("64")) {
                    specExcerpts.add("equipped with " + ram + " for smooth multitasking");
                } else {
                    specExcerpts.add("has " + ram);
                }
            }
            if (specs.containsKey("Processor")) {
                specExcerpts.add("powered by " + specs.get("Processor"));
            }
            if (specs.containsKey("Storage")) {
                String storage = specs.get("Storage");
                if (storage.toUpperCase().contains("SSD")) {
                    specExcerpts.add("features fast " + storage + " storage");
                } else {
                    specExcerpts.add("offers " + storage + " storage");
                }
            }
            if (specs.containsKey("Display")) {
                specExcerpts.add(specs.get("Display") + " display");
            }
        }

        // Phone specs
        if ("phone".equals(parsed.getCategory())) {
            if (specs.containsKey("Camera")) {
                String camera = specs.get("Camera");
                if (camera.contains("108MP") || camera.contains("64MP") || camera.contains("50MP")) {
                    specExcerpts.add("features impressive " + camera + " camera system");
                } else {
                    specExcerpts.add("has " + camera + " camera");
                }
            }
            if (specs.containsKey("Battery")) {
                String battery = specs.get("Battery");
                if (battery.contains("5000") || battery.contains("6000")) {
                    specExcerpts.add("powered by long-lasting " + battery + " battery");
                } else {
                    specExcerpts.add("offers " + battery + " battery");
                }
            }
            if (specs.containsKey("RAM")) {
                specExcerpts.add(specs.get("RAM") + " RAM");
            }
        }

        // Headphones specs
        if ("headphones".equals(parsed.getCategory())) {
            if (specs.containsKey("ANC") && specs.get("ANC").toLowerCase().contains("active")) {
                specExcerpts.add("features active noise cancellation");
            }
            if (specs.containsKey("Battery")) {
                specExcerpts.add("offers " + specs.get("Battery") + " playtime");
            }
        }

        // Shoes specs
        if ("shoes".equals(parsed.getCategory())) {
            if (specs.containsKey("Cushioning")) {
                specExcerpts.add("provides " + specs.get("Cushioning") + " cushioning");
            }
            if (specs.containsKey("Weight")) {
                specExcerpts.add("weighs " + specs.get("Weight"));
            }
        }

        // Battery for all categories
        if (specs.containsKey("Battery") && !"phone".equals(parsed.getCategory()) && 
            !"headphones".equals(parsed.getCategory())) {
            specExcerpts.add("offers " + specs.get("Battery") + " battery life");
        }

        if (specExcerpts.isEmpty()) {
            return null;
        }

        return "It " + String.join(", ", specExcerpts);
    }

    private String buildUseCaseReason(Product p, ParsedQuery parsed) {
        if (parsed.getUseCase() == null) {
            return null;
        }

        Map<String, String> specs = p.getSpecs();
        
        // Check if product has the use case tag
        boolean hasUseCaseTag = p.getTags() != null && p.getTags().contains(parsed.getUseCase());
        
        if (!hasUseCaseTag) {
            return null;
        }

        // Build use-case specific reasoning with specification validation
        String useCase = parsed.getUseCase();
        StringBuilder reason = new StringBuilder();

        if ("coding".equals(useCase) || "programming".equals(useCase)) {
            reason.append("Its specifications make it excellent for **coding**: ");
            List<String> codingFeatures = new ArrayList<>();
            
            if (specs != null) {
                if (specs.containsKey("RAM")) {
                    String ram = specs.get("RAM");
                    if (ram.contains("16") || ram.contains("32") || ram.contains("64")) {
                        codingFeatures.add("ample " + ram + " for running IDEs and virtual machines");
                    }
                }
                if (specs.containsKey("Storage") && specs.get("Storage").toUpperCase().contains("SSD")) {
                    codingFeatures.add("fast SSD for quick compilation");
                }
                if (specs.containsKey("Processor")) {
                    codingFeatures.add("powerful processor for build tasks");
                }
            }
            
            if (!codingFeatures.isEmpty()) {
                reason.append(String.join(", ", codingFeatures));
            } else {
                reason.append("suitable specifications for development work");
            }
        } else if ("gaming".equals(useCase)) {
            reason.append("Built for **gaming** with ");
            if (specs != null && specs.containsKey("RAM")) {
                reason.append(specs.get("RAM")).append(" and high-performance components");
            } else {
                reason.append("gaming-optimized hardware");
            }
        } else if ("camera".equals(useCase) || "photography".equals(useCase)) {
            reason.append("Perfect for **photography** with ");
            if (specs != null && specs.containsKey("Camera")) {
                reason.append(specs.get("Camera")).append(" camera system");
            } else {
                reason.append("advanced camera capabilities");
            }
        } else if ("battery".equals(useCase)) {
            reason.append("Optimized for **long battery life**");
            if (specs != null && specs.containsKey("Battery")) {
                reason.append(" with ").append(specs.get("Battery")).append(" capacity");
            }
        } else {
            // Generic use case match
            reason.append("Well-suited for **").append(useCase).append("** use");
        }

        return reason.toString();
    }
}

