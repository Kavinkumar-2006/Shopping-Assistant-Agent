package com.smartcart.ai.service;

import com.smartcart.ai.dto.ParsedQuery;
import com.smartcart.ai.dto.ShoppingSessionContext;
import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.SortPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Applies a turn's parsed constraints to a bounded shopping session. */
@Slf4j
@Service
public class ConversationContextService {
    private static final Pattern POSITION = Pattern.compile("\\b(first|second|third)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern RESET_PATTERN = Pattern.compile(
        ".*\\b(reset\\s+conversation|clear\\s+conversation|start\\s+over|reset\\s+session|reset\\s+thread|clear\\s+context|new\\s+search|start\\s+new\\s+search|start\\s+a\\s+new\\s+search|clear\\s+everything|clear\\s+all|reset\\s+search|reset)\\b.*",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Detects whether a message represents a reset command.
     */
    public boolean isResetCommand(String message) {
        if (message == null || message.isBlank()) return false;
        String lower = message.toLowerCase(Locale.ROOT).trim();
        return RESET_PATTERN.matcher(lower).matches() || 
               lower.equalsIgnoreCase("reset") || 
               lower.equalsIgnoreCase("start over") || 
               lower.equalsIgnoreCase("new search") || 
               lower.equalsIgnoreCase("clear everything") ||
               lower.equalsIgnoreCase("clear all");
    }

    public ShoppingSessionContext update(ShoppingSessionContext current, ParsedQuery parsed, String message) {
        if (isResetCommand(message)) {
            log.info("Reset command detected in message '{}'. Resetting session context.", message);
            return ShoppingSessionContext.builder().build();
        }

        ShoppingSessionContext context = current == null ? ShoppingSessionContext.builder().build() : current.toBuilder().build();
        if (context.getRejectedProductIds() == null) {
            context.setRejectedProductIds(new ArrayList<>());
        } else {
            context.setRejectedProductIds(new ArrayList<>(context.getRejectedProductIds()));
        }
        if (context.getPreviousRecommendations() == null) {
            context.setPreviousRecommendations(new ArrayList<>());
        } else {
            context.setPreviousRecommendations(new ArrayList<>(context.getPreviousRecommendations()));
        }

        String lower = message != null ? message.toLowerCase(Locale.ROOT) : "";
        boolean removalRequest = lower.matches(".*\\b(forget|remove|don't care about|do not want|don't want)\\b.*");

        // 1. Explicit Removals
        if (lower.matches(".*\\b(forget|remove|don't care about)\\s+(the )?budget.*")) { context.setMaxPrice(null); context.setMinPrice(null); }
        if (lower.matches(".*\\b(forget|remove|don't care about)\\s+(the )?(laptop|phone|headphones|shoes|tablet|smartwatch).*")) context.setCategory(null);
        if (lower.matches(".*\\b(forget|remove|don't care about)\\s+(the )?(gaming|coding|camera|ram).*")) {
            if (lower.contains("gaming") || lower.contains("coding") || lower.contains("camera")) context.setUseCase(null);
            if (lower.contains("ram")) context.setRequiredFeatures(new ArrayList<>());
        }
        if (lower.startsWith("remove ") && parsed.getBrand() != null) context.setBrand(null);

        // 2. Category Switch / Override
        if (!removalRequest && parsed.getCategory() != null) {
            String newCat = parsed.getCategory();
            if (context.getCategory() != null && !context.getCategory().equalsIgnoreCase(newCat)) {
                log.info("Category changed from '{}' to '{}'. Overriding category and resetting old category-specific constraints.", context.getCategory(), newCat);
                context.setBrand(parsed.getBrand());
                context.setUseCase(parsed.getUseCase());
                context.setRequiredFeatures(parsed.getFeatures() != null ? new ArrayList<>(parsed.getFeatures()) : new ArrayList<>());
                context.setPreferredOperatingSystem(null);
                context.setRejectedProductIds(new ArrayList<>());
                context.setPreviousRecommendations(new ArrayList<>());
            }
            context.setCategory(newCat);
        }

        // 3. Merging/Overriding New Turn Attributes
        if (!removalRequest) {
            if (parsed.getMinPrice() != null) context.setMinPrice(parsed.getMinPrice());
            if (parsed.getBudget() != null) context.setMaxPrice(parsed.getBudget());
            if (parsed.getBrand() != null && !lower.contains("no ") && !lower.contains("without ")) context.setBrand(parsed.getBrand());
            if (parsed.getUseCase() != null) context.setUseCase(parsed.getUseCase());

            if (parsed.getFeatures() != null && !parsed.getFeatures().isEmpty()) {
                List<String> features = context.getRequiredFeatures() != null ? new ArrayList<>(context.getRequiredFeatures()) : new ArrayList<>();
                for (String f : parsed.getFeatures()) {
                    if (!features.contains(f)) features.add(f);
                }
                context.setRequiredFeatures(features);
            }
            if (parsed.getExcludedFeatures() != null && !parsed.getExcludedFeatures().isEmpty()) {
                List<String> excluded = context.getExcludedBrands() != null ? new ArrayList<>(context.getExcludedBrands()) : new ArrayList<>();
                for (String value : parsed.getExcludedFeatures()) {
                    if (!excluded.contains(value)) excluded.add(value);
                }
                context.setExcludedBrands(excluded);
            }
            if (parsed.getMinRating() != null) context.setMinRating(parsed.getMinRating());
            if (parsed.getSortPreference() != null) context.setSortPreference(parsed.getSortPreference());

            if (lower.contains("windows")) context.setPreferredOperatingSystem("Windows");
            if (lower.contains("macos") || lower.contains("mac os")) context.setPreferredOperatingSystem("macOS");
            if (lower.contains("android")) context.setPreferredOperatingSystem("Android");
            if (lower.contains("ios")) context.setPreferredOperatingSystem("iOS");
        }

        rejectReferencedProduct(context, lower);
        return context;
    }

    public ParsedQuery merge(ParsedQuery turn, ShoppingSessionContext context) {
        if (context == null) return turn;

        String category = turn.getCategory() != null ? turn.getCategory() : context.getCategory();
        Long budget = turn.getBudget() != null ? turn.getBudget() : (turn.getMaxPrice() != null ? turn.getMaxPrice() : context.getMaxPrice());
        Long minPrice = turn.getMinPrice() != null ? turn.getMinPrice() : context.getMinPrice();
        String brand = turn.getBrand() != null ? turn.getBrand() : context.getBrand();
        String useCase = turn.getUseCase() != null ? turn.getUseCase() : context.getUseCase();
        Double minRating = turn.getMinRating() != null ? turn.getMinRating() : context.getMinRating();
        SortPreference sorting = turn.getSortPreference() != null ? turn.getSortPreference() : context.getSortPreference();

        List<String> features = new ArrayList<>();
        if (context.getRequiredFeatures() != null) features.addAll(context.getRequiredFeatures());
        if (turn.getFeatures() != null) {
            for (String f : turn.getFeatures()) {
                if (!features.contains(f)) features.add(f);
            }
        }

        List<String> keywords = new ArrayList<>();
        if (context.getKeywords() != null) keywords.addAll(context.getKeywords());
        if (turn.getKeywords() != null) {
            for (String k : turn.getKeywords()) {
                if (!keywords.contains(k)) keywords.add(k);
            }
        }

        return turn.toBuilder()
                .category(category)
                .budget(budget)
                .maxPrice(budget)
                .minPrice(minPrice)
                .brand(brand)
                .useCase(useCase)
                .features(features)
                .keywords(keywords)
                .minRating(minRating)
                .sortPreference(sorting)
                .build();
    }

    private void rejectReferencedProduct(ShoppingSessionContext context, String message) {
        if (!message.contains("don't want") && !message.contains("do not want") && 
            !message.contains("don't like") && !message.contains("do not like") && 
            !message.contains("reject") && !message.contains("exclude") && 
            !message.contains("remove") && !message.contains("not the") &&
            !message.contains("other than")) return;
        List<Product> previous = context.getPreviousRecommendations();
        if (previous == null || previous.isEmpty()) return;

        if (context.getRejectedProductIds() == null) {
            context.setRejectedProductIds(new ArrayList<>());
        }

        Matcher matcher = POSITION.matcher(message);
        if (matcher.find()) {
            int index = switch (matcher.group(1).toLowerCase(Locale.ROOT)) { case "first" -> 0; case "second" -> 1; default -> 2; };
            if (index < previous.size()) {
                String id = previous.get(index).getId();
                if (!context.getRejectedProductIds().contains(id)) {
                    context.getRejectedProductIds().add(id);
                }
            }
        } else {
            previous.stream()
                .filter(product -> message.contains(product.getBrand().toLowerCase(Locale.ROOT)) || message.contains(product.getName().toLowerCase(Locale.ROOT)))
                .forEach(product -> {
                    if (!context.getRejectedProductIds().contains(product.getId())) {
                        context.getRejectedProductIds().add(product.getId());
                    }
                });
        }
    }
}

