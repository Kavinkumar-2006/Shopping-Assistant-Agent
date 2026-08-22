package com.smartcart.ai.service;

import com.smartcart.ai.dto.ParsedQuery;
import com.smartcart.ai.entity.ShoppingIntent;
import com.smartcart.ai.entity.SortPreference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a natural-language shopping query into a structured {@link ParsedQuery}.
 * Extended to support brands, price ranges, features, exclusions, ratings, and sorting.
 */
@Slf4j
@Service
public class QueryParserService {

    // ── Existing Budget Regex ────────────────────────────────────────────────
    private static final Pattern BUDGET_PATTERN = Pattern.compile(
        "(?:under|below|within|less\\s+than|upto|up\\s+to|max|maximum|budget\\s+of|budget\\s+is|budget|for|" +
        "around|approximately|approx|not\\s+more\\s+than|make\\s+it|change\\s+(?:budget\\s+)?to|set\\s+(?:budget\\s+)?to|make\\s+budget|instead|₹|rs\\.?\\s*|inr\\s*)\\s*" +
        "(\\d[\\d,]*(?:\\.\\d+)?)\\s*(k|K|lakh|lac|L)?|(?:\\b(\\d[\\d,]*(?:\\.\\d+)?)\\s*(k|K|lakh|lac|L)\\b)",
        Pattern.CASE_INSENSITIVE
    );

    // ── V2 Price Floor Regex ─────────────────────────────────────────────────
    private static final Pattern MIN_PRICE_PATTERN = Pattern.compile(
        "(?:above|over|greater\\s+than|more\\s+than|min|minimum|starting\\s+from|from|at\\s+least)\\s*" +
        "(\\d[\\d,]*(?:\\.\\d+)?)\\s*(k|K|lakh|lac|L)?",
        Pattern.CASE_INSENSITIVE
    );

    // ── Price Range Regex (e.g., "40k to 60k", "between 40000 and 60000", "5k to 15k") ───
    private static final Pattern PRICE_RANGE_PATTERN = Pattern.compile(
        "(?:(?:between|from)\\s*)?(\\d[\\d,]*(?:\\.\\d+)?)\\s*(k|K|lakh|lac|L)?\\s*(?:to|and|-|–)\\s*(\\d[\\d,]*(?:\\.\\d+)?)\\s*(k|K|lakh|lac|L)?",
        Pattern.CASE_INSENSITIVE
    );

    // ── V2 Rating Regex ──────────────────────────────────────────────────────
    private static final Pattern RATING_PATTERN = Pattern.compile(
        "(?:rating\\s*(?:above|over|of|at\\s+least)?\\s*(\\d(?:\\.\\d)?))|" +
        "(?:(\\d(?:\\.\\d)?)\\s*stars?)",
        Pattern.CASE_INSENSITIVE
    );

    // ── V2 Exclusion Regex ───────────────────────────────────────────────────
    private static final Pattern EXCLUSION_PATTERN = Pattern.compile(
        "(?:no|without|excluding|except)\\s+(\\w+)",
        Pattern.CASE_INSENSITIVE
    );

    // ── Known Brands List ────────────────────────────────────────────────────
    private static final List<String> KNOWN_BRANDS = List.of(
        // Laptops / Phones
        "dell", "hp", "lenovo", "asus", "acer", "apple", "samsung", "oneplus",
        "realme", "oppo", "vivo", "xiaomi", "mi", "motorola", "google", "microsoft", "msi", "gigabyte",
        // Audio
        "sony", "bose", "jbl", "boat", "noise", "marshall", "saregama", "sennheiser",
        // Footwear
        "nike", "adidas", "puma",
        // Peripherals / AV
        "logitech", "razer", "corsair", "keychron", "zebronics",
        "canon", "nikon", "fujifilm", "gopro",
        "lg", "vu", "tcl", "hisense",
        "benq", "viewsonic"
    );

    // ── Known Features List ──────────────────────────────────────────────────
    private static final List<String> KNOWN_FEATURES = List.of(
        "anc", "5g", "ssd", "ram", "camera", "cushioning", "wireless", "waterproof", "gps", "bluetooth",
        "4k", "uhd", "mechanical", "rgb", "ergonomic", "mirrorless"
    );

    // ── Existing + New Category Keywords Mapping ─────────────────────────────
    private static final List<String[]> CATEGORY_ENTRIES = List.of(
        new String[]{"gaming laptop",   "laptop"},
        new String[]{"coding laptop",   "laptop"},
        new String[]{"laptop",          "laptop"},
        new String[]{"laptops",         "laptop"},
        new String[]{"notebook",        "laptop"},
        new String[]{"macbook",         "laptop"},
        new String[]{"chromebook",      "laptop"},
        new String[]{"smartphone",      "phone"},
        new String[]{"mobile phone",    "phone"},
        new String[]{"iphone",          "phone"},
        new String[]{"phone",           "phone"},
        new String[]{"phones",          "phone"},
        new String[]{"mobile",          "phone"},
        new String[]{"noise cancelling headphone", "headphones"},
        new String[]{"noise canceling headphone",  "headphones"},
        new String[]{"wireless headphone",         "headphones"},
        new String[]{"headphone",       "headphones"},
        new String[]{"headphones",      "headphones"},
        new String[]{"earphone",        "headphones"},
        new String[]{"earphones",       "headphones"},
        new String[]{"earbuds",         "headphones"},
        new String[]{"headset",         "headphones"},
        new String[]{"running shoes",   "shoes"},
        new String[]{"sports shoes",    "shoes"},
        new String[]{"training shoes",  "shoes"},
        new String[]{"sneakers",        "shoes"},
        new String[]{"sneaker",         "shoes"},
        new String[]{"shoes",           "shoes"},
        new String[]{"shoe",            "shoes"},
        new String[]{"ipad",            "tablet"},
        new String[]{"tablets",         "tablet"},
        new String[]{"tablet",          "tablet"},
        new String[]{"smart watch",     "smartwatch"},
        new String[]{"smartwatch",      "smartwatch"},
        // ── Phase 4 Step 8: New Categories ───────────────────────────────────
        new String[]{"television",      "tv"},
        new String[]{"televisions",     "tv"},
        new String[]{"smart tv",        "tv"},
        new String[]{"oled tv",         "tv"},
        new String[]{"qled tv",         "tv"},
        new String[]{"led tv",          "tv"},
        new String[]{"tv",              "tv"},
        new String[]{"dslr camera",     "camera"},
        new String[]{"mirrorless camera","camera"},
        new String[]{"action camera",   "camera"},
        new String[]{"point and shoot", "camera"},
        new String[]{"digital camera",  "camera"},
        new String[]{"cameras",         "camera"},
        new String[]{"bluetooth speaker","speaker"},
        new String[]{"portable speaker","speaker"},
        new String[]{"soundbar",        "speaker"},
        new String[]{"speakers",        "speaker"},
        new String[]{"mechanical keyboard","keyboard"},
        new String[]{"wireless keyboard","keyboard"},
        new String[]{"gaming keyboard", "keyboard"},
        new String[]{"keyboards",       "keyboard"},
        new String[]{"keyboard",        "keyboard"},
        new String[]{"gaming mouse",    "mouse"},
        new String[]{"wireless mouse",  "mouse"},
        new String[]{"ergonomic mouse", "mouse"},
        new String[]{"mouse",           "mouse"},
        new String[]{"mice",            "mouse"},
        new String[]{"gaming monitor",  "monitor"},
        new String[]{"4k monitor",      "monitor"},
        new String[]{"ultrawide monitor","monitor"},
        new String[]{"monitors",        "monitor"},
        new String[]{"monitor",         "monitor"},
        new String[]{"display",         "monitor"},
        new String[]{"speaker",         "speaker"},
        new String[]{"camera",          "camera"}
    );

    // ── Existing Use-Case Priority Mapping ───────────────────────────────────
    private static final List<String[]> USE_CASE_ENTRIES = List.of(
        new String[]{"coding",          "coding"},
        new String[]{"programming",     "coding"},
        new String[]{"developer",       "coding"},
        new String[]{"software",        "coding"},
        new String[]{"development",     "coding"},
        new String[]{"web development", "coding"},
        new String[]{"gaming",          "gaming"},
        new String[]{"game",            "gaming"},
        new String[]{"games",           "gaming"},
        new String[]{"photography",     "camera"},
        new String[]{"camera",          "camera"},
        new String[]{"photo",           "camera"},
        new String[]{"selfie",          "camera"},
        new String[]{"zoom",            "camera"},
        new String[]{"long battery",    "battery"},
        new String[]{"battery life",    "battery"},
        new String[]{"battery",         "battery"},
        new String[]{"marathon",        "running"},
        new String[]{"running",         "running"},
        new String[]{"jogging",         "running"},
        new String[]{"gym",             "running"},
        new String[]{"workout",         "running"},
        new String[]{"fitness",         "running"},
        new String[]{"sports",          "running"},
        new String[]{"audiophile",      "music"},
        new String[]{"music",           "music"},
        new String[]{"bass",            "music"},
        new String[]{"audio",           "music"},
        new String[]{"work from home",  "office"},
        new String[]{"work-from-home",  "office"},
        new String[]{"office",          "office"},
        new String[]{"work",            "office"},
        new String[]{"calls",           "office"},
        new String[]{"meetings",        "office"},
        new String[]{"noise cancelling", "anc"},
        new String[]{"noise canceling",  "anc"},
        new String[]{"anc",              "anc"},
        new String[]{"travel",           "travel"},
        new String[]{"travelling",       "travel"},
        new String[]{"commute",          "travel"},
        new String[]{"student",          "student"},
        new String[]{"college",          "student"},
        new String[]{"school",           "student"},
        new String[]{"budget",           "budget"},
        new String[]{"cheap",            "budget"},
        new String[]{"affordable",       "budget"},
        new String[]{"value for money",  "budget"},
        new String[]{"premium",          "premium"},
        new String[]{"flagship",         "premium"},
        new String[]{"best",             "premium"}
    );

    /**
     * Parses a raw natural-language query into a structured {@link ParsedQuery}.
     *
     * @param rawQuery the user's input string
     * @return structured query with category, budget, useCase, and keywords
     */
    public ParsedQuery parse(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return ParsedQuery.builder()
                    .originalQuery(rawQuery)
                    .keywords(List.of())
                    .build();
        }

        String normalised = normalise(rawQuery);

        String       category = detectCategory(normalised);
        Long         budget   = detectBudget(normalised);
        List<String> keywords = detectKeywords(normalised);
        String       useCase  = resolveUseCase(normalised);

        // V2 Extended Parsing
        String       brand       = detectBrand(normalised);
        Long         minPrice    = detectMinPrice(normalised);
        Long         maxPrice    = budget; // maxPrice maps directly to parsed budget
        List<String> features    = detectFeatures(normalised);
        List<String> exclusions  = detectExclusions(normalised);
        Double       minRating   = detectMinRating(normalised);
        SortPreference sorting   = detectSortPreference(normalised);
        List<String> comparison  = detectComparisonProducts(normalised);

        log.info("Parsed '{}' → category={}, budget={}, useCase={}, brand={}, minPrice={}, maxPrice={}, features={}, exclusions={}, minRating={}, sorting={}, comparison={}",
                rawQuery, category, budget, useCase, brand, minPrice, maxPrice, features, exclusions, minRating, sorting, comparison);

        return ParsedQuery.builder()
                .category(category)
                .budget(budget)
                .useCase(useCase)
                .keywords(keywords)
                .originalQuery(rawQuery)
                .brand(brand)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .features(features)
                .excludedFeatures(exclusions)
                .minRating(minRating)
                .sortPreference(sorting)
                .comparisonProducts(comparison)
                .build();
    }

    private String normalise(String input) {
        return input.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private String detectCategory(String query) {
        boolean isCameraUseCase = query.matches(".*\\b(for|with)\\s+camera\\b.*") &&
                !query.matches(".*\\b(dslr|mirrorless|action|digital|security|cctv)\\s+camera\\b.*");

        for (String[] entry : CATEGORY_ENTRIES) {
            if (isCameraUseCase && entry[1].equals("camera")) {
                continue;
            }
            if (containsWord(query, entry[0])) {
                return entry[1];
            }
        }
        return null;
    }

    private Long detectBudget(String query) {
        // First check for price ranges
        Matcher rangeMatcher = PRICE_RANGE_PATTERN.matcher(query);
        if (rangeMatcher.find()) {
            // Extract max value from range
            String maxStr = rangeMatcher.group(3).replace(",", "");
            String maxSuffix = rangeMatcher.group(4);
            try {
                long value = parseNumberWithSuffix(maxStr, maxSuffix);
                if (value >= 500 && value <= 10_000_000) {
                    return value;
                }
            } catch (NumberFormatException ignored) {}
        }

        Matcher matcher = BUDGET_PATTERN.matcher(query);
        Long maxBudget = null;

        while (matcher.find()) {
            String numStr = matcher.group(1) != null ? matcher.group(1) : matcher.group(3);
            String suffix = matcher.group(1) != null ? matcher.group(2) : matcher.group(4);
            if (numStr != null) {
                numStr = numStr.replace(",", "");
                try {
                    long value = parseNumberWithSuffix(numStr, suffix);
                    if (value >= 500 && value <= 10_000_000) {
                        maxBudget = (maxBudget == null) ? value : Math.max(maxBudget, value);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return maxBudget;
    }

    // ── V2 Advanced Query Parsing Extraction Helpers ─────────────────────────

    /**
     * Parses a number with optional suffix (k, lakh, lac, L).
     * Examples: "60k" -> 60000, "1 lakh" -> 100000, "0.6 lakh" -> 60000
     */
    private long parseNumberWithSuffix(String numStr, String suffix) throws NumberFormatException {
        double value = Double.parseDouble(numStr);
        
        if (suffix != null) {
            String suffixLower = suffix.toLowerCase();
            if (suffixLower.equals("k")) {
                value *= 1_000;
            } else if (suffixLower.equals("lakh") || suffixLower.equals("lac") || suffixLower.equals("l")) {
                value *= 100_000;
            }
        }
        
        return (long) value;
    }

    private String detectBrand(String query) {
        for (String brand : KNOWN_BRANDS) {
            if (containsWord(query, brand)) {
                return brand.substring(0, 1).toUpperCase() + brand.substring(1);
            }
        }
        return null;
    }

    private Long detectMinPrice(String query) {
        // Check for price ranges first
        Matcher rangeMatcher = PRICE_RANGE_PATTERN.matcher(query);
        if (rangeMatcher.find()) {
            // Extract min value from range
            String minStr = rangeMatcher.group(1).replace(",", "");
            String minSuffix = rangeMatcher.group(2);
            try {
                long value = parseNumberWithSuffix(minStr, minSuffix);
                if (value >= 100 && value <= 10_000_000) {
                    return value;
                }
            } catch (NumberFormatException ignored) {}
        }

        Matcher matcher = MIN_PRICE_PATTERN.matcher(query);
        Long minPrice = null;

        while (matcher.find()) {
            String numStr = matcher.group(1).replace(",", "");
            String suffix = matcher.group(2);
            try {
                long value = parseNumberWithSuffix(numStr, suffix);
                if (value >= 100 && value <= 10_000_000) {
                    minPrice = (minPrice == null) ? value : Math.min(minPrice, value);
                }
            } catch (NumberFormatException ignored) {}
        }
        return minPrice;
    }

    private List<String> detectFeatures(String query) {
        List<String> features = new ArrayList<>();
        for (String feat : KNOWN_FEATURES) {
            if (containsWord(query, feat)) {
                // Verify this feature is not preceded by negation
                int index = query.indexOf(feat);
                boolean isNegated = false;
                if (index > 3) {
                    String sub = query.substring(Math.max(0, index - 10), index).trim();
                    if (sub.contains("no ") || sub.contains("without ") || sub.contains("except ")) {
                        isNegated = true;
                    }
                }
                if (!isNegated) {
                    features.add(feat);
                }
            }
        }
        return features;
    }

    private List<String> detectExclusions(String query) {
        List<String> exclusions = new ArrayList<>();
        Matcher matcher = EXCLUSION_PATTERN.matcher(query);
        while (matcher.find()) {
            exclusions.add(matcher.group(1));
        }
        return exclusions;
    }

    private Double detectMinRating(String query) {
        Matcher matcher = RATING_PATTERN.matcher(query);
        if (matcher.find()) {
            String valStr = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            try {
                double r = Double.parseDouble(valStr);
                if (r >= 0.0 && r <= 5.0) {
                    return r;
                }
            } catch (NumberFormatException ignored) {}
        }

        // Implicit rating requirement
        if (query.contains("best rated") || query.contains("top rated") || query.contains("highest rated")) {
            return 4.0;
        }

        return null;
    }

    private SortPreference detectSortPreference(String query) {
        if (query.contains("cheapest") || query.contains("lowest price") || query.contains("price low to high")) {
            return SortPreference.PRICE_LOW_TO_HIGH;
        }
        if (query.contains("most expensive") || query.contains("highest price") || query.contains("price high to low")) {
            return SortPreference.PRICE_HIGH_TO_LOW;
        }
        if (query.contains("best rated") || query.contains("top rated") || query.contains("highest rating") || query.contains("highest rated")) {
            return SortPreference.RATING;
        }
        if (query.contains("popular") || query.contains("most popular") || query.contains("most reviewed") || query.contains("best seller")) {
            return SortPreference.POPULARITY;
        }
        if (query.contains("best value") || query.contains("value for money") || query.contains("best deal")) {
            return SortPreference.VALUE;
        }
        return null;
    }

    private List<String> detectComparisonProducts(String query) {
        List<String> list = new ArrayList<>();
        for (String brand : KNOWN_BRANDS) {
            if (containsWord(query, brand)) {
                list.add(brand.substring(0, 1).toUpperCase() + brand.substring(1));
            }
        }
        return list;
    }

    // ── End V2 Detections ────────────────────────────────────────────────────

    private String resolveUseCase(String query) {
        for (String[] entry : USE_CASE_ENTRIES) {
            if (containsWord(query, entry[0])) {
                return entry[1];
            }
        }
        return null;
    }

    private List<String> detectKeywords(String query) {
        List<String> collected = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (String[] entry : USE_CASE_ENTRIES) {
            if (containsWord(query, entry[0]) && seen.add(entry[1])) {
                collected.add(entry[1]);
            }
        }
        return Collections.unmodifiableList(collected);
    }

    private boolean containsWord(String query, String phrase) {
        if (phrase.contains(" ")) {
            return query.contains(phrase);
        }
        Pattern wordBoundary = Pattern.compile("\\b" + Pattern.quote(phrase) + "\\b");
        return wordBoundary.matcher(query).find();
    }
}
