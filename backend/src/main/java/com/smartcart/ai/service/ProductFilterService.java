package com.smartcart.ai.service;

import com.smartcart.ai.dto.ParsedQuery;
import com.smartcart.ai.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dedicated service to filter candidate products against parsed constraints.
 */
@Slf4j
@Service
public class ProductFilterService {

    /**
     * Filters list of products according to parsed constraints.
     *
     * @param products list of candidate products
     * @param parsed   parsed query constraints
     * @return filtered list of products
     */
    public List<Product> filter(List<Product> products, ParsedQuery parsed) {
        if (products == null || products.isEmpty()) {
            return new ArrayList<>();
        }

        return products.stream()
                .filter(p -> passesCategory(p, parsed))
                .filter(p -> passesBrand(p, parsed))
                .filter(p -> passesPriceFloor(p, parsed))
                .filter(p -> passesPriceCeiling(p, parsed))
                .filter(p -> passesRating(p, parsed))
                .filter(p -> passesExclusions(p, parsed))
                .filter(p -> passesFeatures(p, parsed))
                .collect(Collectors.toList());
    }

    /**
     * Builds list of user-friendly text descriptions of the applied filters.
     *
     * @param parsed parsed query details
     * @return List of filter strings
     */
    public List<String> getAppliedFiltersDescription(ParsedQuery parsed) {
        List<String> list = new ArrayList<>();
        if (parsed.getCategory() != null) {
            list.add("Category: " + parsed.getCategory());
        }
        if (parsed.getBrand() != null) {
            list.add("Brand: " + parsed.getBrand());
        }
        if (parsed.getMinPrice() != null) {
            list.add("Min Price: ₹" + String.format("%,d", parsed.getMinPrice()));
        }
        if (parsed.getBudget() != null) {
            list.add("Max Price: ₹" + String.format("%,d", parsed.getBudget()));
        }
        if (parsed.getMinRating() != null) {
            list.add("Min Rating: ★" + parsed.getMinRating());
        }
        if (parsed.getFeatures() != null && !parsed.getFeatures().isEmpty()) {
            list.add("Features required: " + String.join(", ", parsed.getFeatures()));
        }
        if (parsed.getExcludedFeatures() != null && !parsed.getExcludedFeatures().isEmpty()) {
            list.add("Excluding: " + String.join(", ", parsed.getExcludedFeatures()));
        }
        return list;
    }

    private boolean passesCategory(Product p, ParsedQuery parsed) {
        if (parsed.getCategory() == null) return true;
        return p.getCategory() != null && p.getCategory().equalsIgnoreCase(parsed.getCategory());
    }

    private boolean passesBrand(Product p, ParsedQuery parsed) {
        if (parsed.getBrand() == null) return true;
        return p.getBrand() != null && p.getBrand().equalsIgnoreCase(parsed.getBrand());
    }

    private boolean passesPriceFloor(Product p, ParsedQuery parsed) {
        if (parsed.getMinPrice() == null) return true;
        return p.getPrice() >= parsed.getMinPrice();
    }

    private boolean passesPriceCeiling(Product p, ParsedQuery parsed) {
        if (parsed.getBudget() == null) return true;
        return p.getPrice() <= parsed.getBudget();
    }

    private boolean passesRating(Product p, ParsedQuery parsed) {
        if (parsed.getMinRating() == null) return true;
        return p.getRating() >= parsed.getMinRating();
    }

    private boolean passesExclusions(Product p, ParsedQuery parsed) {
        if (parsed.getExcludedFeatures() == null || parsed.getExcludedFeatures().isEmpty()) {
            return true;
        }
        for (String excl : parsed.getExcludedFeatures()) {
            String exclLower = excl.toLowerCase();
            // Check brand
            if (p.getBrand() != null && p.getBrand().toLowerCase().contains(exclLower)) {
                return false;
            }
            // Check name
            if (p.getName() != null && p.getName().toLowerCase().contains(exclLower)) {
                return false;
            }
            // Check description
            if (p.getDescription() != null && p.getDescription().toLowerCase().contains(exclLower)) {
                return false;
            }
            // Check tags
            if (p.getTags() != null && p.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(exclLower))) {
                return false;
            }
        }
        return true;
    }

    private boolean passesFeatures(Product p, ParsedQuery parsed) {
        if (parsed.getFeatures() == null || parsed.getFeatures().isEmpty()) {
            return true;
        }
        for (String feat : parsed.getFeatures()) {
            String featLower = feat.toLowerCase();
            boolean match = checkSingleFeatureMatch(p, featLower);

            if (!match && featLower.contains(" ")) {
                // Tokenized check for phrases like "16gb ram"
                String[] tokens = featLower.split("\\s+");
                boolean allTokensMatch = true;
                for (String token : tokens) {
                    if (!checkSingleFeatureMatch(p, token)) {
                        allTokensMatch = false;
                        break;
                    }
                }
                if (allTokensMatch) {
                    match = true;
                }
            }

            // If any single required feature is missing, product fails
            if (!match) {
                return false;
            }
        }
        return true;
    }

    private boolean checkSingleFeatureMatch(Product p, String term) {
        if (p.getSpecs() != null) {
            for (var entry : p.getSpecs().entrySet()) {
                if ((entry.getKey() != null && entry.getKey().toLowerCase().contains(term)) ||
                        (entry.getValue() != null && entry.getValue().toLowerCase().contains(term))) {
                    return true;
                }
            }
        }
        if (p.getTags() != null && p.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(term))) {
            return true;
        }
        if (p.getName() != null && p.getName().toLowerCase().contains(term)) {
            return true;
        }
        if (p.getDescription() != null && p.getDescription().toLowerCase().contains(term)) {
            return true;
        }
        return false;
    }
}
