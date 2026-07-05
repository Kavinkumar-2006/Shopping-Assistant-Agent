package com.smartcart.ai.service;

import com.smartcart.ai.dto.ParsedQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a natural-language shopping query into a structured {@link ParsedQuery}.
 *
 * <p>Design principles:
 * <ul>
 *   <li>Fully deterministic — no randomness, no external API calls</li>
 *   <li>Regex for budget extraction (anchored to trigger words to avoid false positives)</li>
 *   <li>Ordered keyword maps for category detection (longest match wins)</li>
 *   <li>Priority-ranked use-case detection (first match wins for useCase)</li>
 *   <li>All detected keywords are collected for scoring (multi-tag support)</li>
 * </ul>
 *
 * <p>Supported query examples:
 * <ul>
 *   <li>"Suggest a laptop under 60000 for coding"</li>
 *   <li>"Best phone below ₹25k with good camera"</li>
 *   <li>"I need running shoes under 3000"</li>
 *   <li>"Wireless headphones with ANC under 10000"</li>
 *   <li>"Gaming laptop budget 80k"</li>
 * </ul>
 */
@Slf4j
@Service
public class QueryParserService {

    // ─────────────────────────────────────────────────────────────────────────
    // BUDGET REGEX
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Matches budget expressions that are ANCHORED to a trigger word.
     * This avoids capturing random numbers in sentences like "top 5 phones".
     *
     * Supports:
     *   "under 60000"     "below ₹25,000"    "less than 3000"
     *   "within 10k"      "upto 15K"         "max 8000"
     *   "budget of 50000" "around 20000"     "approximately 12000"
     *   "₹60000"          "Rs 25000"
     *
     * Capture group 1: the raw number string (may contain commas)
     * The 'k'/'K' suffix is detected from the full match group(0).
     */
    private static final Pattern BUDGET_PATTERN = Pattern.compile(
        "(?:under|below|within|less\\s+than|upto|up\\s+to|max|maximum|budget\\s+of|" +
        "around|approximately|approx|not\\s+more\\s+than|₹|rs\\.?\\s*)\\s*" +
        "(\\d[\\d,]*)\\s*(k|K)?",
        Pattern.CASE_INSENSITIVE
    );

    // ─────────────────────────────────────────────────────────────────────────
    // CATEGORY KEYWORD MAP
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ordered list of (keyword, normalised-category) pairs.
     * Multi-word entries MUST come before their single-word substrings
     * so "running shoes" is matched before "shoes".
     */
    private static final List<String[]> CATEGORY_ENTRIES = List.of(
        // ── Laptops ──
        new String[]{"gaming laptop",   "laptop"},
        new String[]{"coding laptop",   "laptop"},
        new String[]{"laptop",          "laptop"},
        new String[]{"laptops",         "laptop"},
        new String[]{"notebook",        "laptop"},
        new String[]{"macbook",         "laptop"},
        new String[]{"chromebook",      "laptop"},
        // ── Phones ──
        new String[]{"smartphone",      "phone"},
        new String[]{"mobile phone",    "phone"},
        new String[]{"iphone",          "phone"},
        new String[]{"phone",           "phone"},
        new String[]{"phones",          "phone"},
        new String[]{"mobile",          "phone"},
        // ── Headphones ──
        new String[]{"noise cancelling headphone", "headphones"},
        new String[]{"noise canceling headphone",  "headphones"},
        new String[]{"wireless headphone",         "headphones"},
        new String[]{"headphone",       "headphones"},
        new String[]{"headphones",      "headphones"},
        new String[]{"earphone",        "headphones"},
        new String[]{"earphones",       "headphones"},
        new String[]{"earbuds",         "headphones"},
        new String[]{"headset",         "headphones"},
        // ── Shoes ──
        new String[]{"running shoes",   "shoes"},
        new String[]{"sports shoes",    "shoes"},
        new String[]{"training shoes",  "shoes"},
        new String[]{"sneakers",        "shoes"},
        new String[]{"sneaker",         "shoes"},
        new String[]{"shoes",           "shoes"},
        new String[]{"shoe",            "shoes"},
        // ── Tablets ──
        new String[]{"ipad",            "tablet"},
        new String[]{"tablets",         "tablet"},
        new String[]{"tablet",          "tablet"},
        // ── Smartwatches ──
        new String[]{"smart watch",     "smartwatch"},
        new String[]{"smartwatch",      "smartwatch"}
    );

    // ─────────────────────────────────────────────────────────────────────────
    // USE-CASE PRIORITY LIST
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Priority-ordered list of (trigger-phrase, canonical-use-case) pairs.
     * The FIRST match becomes the primary useCase.
     * All matches are collected into the keywords list.
     */
    private static final List<String[]> USE_CASE_ENTRIES = List.of(
        // Coding / Development
        new String[]{"coding",          "coding"},
        new String[]{"programming",     "coding"},
        new String[]{"developer",       "coding"},
        new String[]{"software",        "coding"},
        new String[]{"development",     "coding"},
        new String[]{"web development", "coding"},
        // Gaming
        new String[]{"gaming",          "gaming"},
        new String[]{"game",            "gaming"},
        new String[]{"games",           "gaming"},
        // Camera / Photography
        new String[]{"photography",     "camera"},
        new String[]{"camera",          "camera"},
        new String[]{"photo",           "camera"},
        new String[]{"selfie",          "camera"},
        new String[]{"zoom",            "camera"},
        // Battery
        new String[]{"long battery",    "battery"},
        new String[]{"battery life",    "battery"},
        new String[]{"battery",         "battery"},
        // Running / Gym / Fitness
        new String[]{"marathon",        "running"},
        new String[]{"running",         "running"},
        new String[]{"jogging",         "running"},
        new String[]{"gym",             "running"},
        new String[]{"workout",         "running"},
        new String[]{"fitness",         "running"},
        new String[]{"sports",          "running"},
        // Music / Audio
        new String[]{"audiophile",      "music"},
        new String[]{"music",           "music"},
        new String[]{"bass",            "music"},
        new String[]{"audio",           "music"},
        // Office / Work
        new String[]{"work from home",  "office"},
        new String[]{"work-from-home",  "office"},
        new String[]{"office",          "office"},
        new String[]{"work",            "office"},
        new String[]{"calls",           "office"},
        new String[]{"meetings",        "office"},
        // ANC / Noise Cancelling
        new String[]{"noise cancelling", "anc"},
        new String[]{"noise canceling",  "anc"},
        new String[]{"anc",              "anc"},
        // Travel
        new String[]{"travel",           "travel"},
        new String[]{"travelling",       "travel"},
        new String[]{"commute",          "travel"},
        // Student
        new String[]{"student",          "student"},
        new String[]{"college",          "student"},
        new String[]{"school",           "student"},
        // Budget-conscious
        new String[]{"budget",           "budget"},
        new String[]{"cheap",            "budget"},
        new String[]{"affordable",       "budget"},
        new String[]{"value for money",  "budget"},
        // Premium
        new String[]{"premium",          "premium"},
        new String[]{"flagship",         "premium"},
        new String[]{"best",             "premium"}
    );

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

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

        log.info("Parsed '{}' → category={}, budget={}, useCase={}, keywords={}",
                rawQuery, category, budget, useCase, keywords);

        return ParsedQuery.builder()
                .category(category)
                .budget(budget)
                .useCase(useCase)
                .keywords(keywords)
                .originalQuery(rawQuery)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE — Detection helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Lower-cases and collapses extra whitespace for consistent matching. */
    private String normalise(String input) {
        return input.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    /**
     * Iterates the ordered CATEGORY_ENTRIES list and returns the first match.
     * Longer / more specific phrases come first in the list, so "running shoes"
     * wins over bare "shoes".
     */
    private String detectCategory(String query) {
        for (String[] entry : CATEGORY_ENTRIES) {
            if (containsWord(query, entry[0])) {
                return entry[1];
            }
        }
        return null; // not detected
    }

    /**
     * Extracts the maximum budget value from the query.
     *
     * <p>Rules:
     * <ul>
     *   <li>Must be preceded by a recognised trigger word (avoids "top 5 phones" → 5)</li>
     *   <li>Handles 'k'/'K' suffix (e.g., "60k" → 60000)</li>
     *   <li>Handles comma-separated thousands (e.g., "1,20,000")</li>
     *   <li>Returns null if no valid budget found</li>
     * </ul>
     */
    private Long detectBudget(String query) {
        Matcher matcher = BUDGET_PATTERN.matcher(query);
        Long maxBudget = null;

        while (matcher.find()) {
            String numStr = matcher.group(1).replace(",", "");
            try {
                long value = Long.parseLong(numStr);

                // Apply 'k' multiplier
                String suffix = matcher.group(2);
                if (suffix != null && suffix.equalsIgnoreCase("k")) {
                    value *= 1_000;
                }

                // Guard: realistic budget range ₹500 – ₹10,00,000
                if (value >= 500 && value <= 1_000_000) {
                    maxBudget = (maxBudget == null) ? value : Math.max(maxBudget, value);
                }
            } catch (NumberFormatException ignored) {
                // Malformed number — skip silently
            }
        }
        return maxBudget; // null = "not detected"
    }

    /**
     * Resolves the PRIMARY use-case from the query.
     * Returns the canonical value for the FIRST matching trigger in the
     * priority-ordered USE_CASE_ENTRIES list.
     */
    private String resolveUseCase(String query) {
        for (String[] entry : USE_CASE_ENTRIES) {
            if (containsWord(query, entry[0])) {
                return entry[1];
            }
        }
        return null; // not detected
    }

    /**
     * Collects ALL matched use-case keywords (deduplicated by canonical value).
     * Used for multi-dimensional scoring in RecommendationService.
     */
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

    /**
     * Checks if the query contains the given phrase as a whole token sequence.
     * Uses word-boundary matching to avoid substring false positives:
     * e.g., "game" won't match inside "gamer" or "gameplay".
     */
    private boolean containsWord(String query, String phrase) {
        // For multi-word phrases, just use contains (already normalised)
        if (phrase.contains(" ")) {
            return query.contains(phrase);
        }
        // For single words, enforce word boundaries
        Pattern wordBoundary = Pattern.compile("\\b" + Pattern.quote(phrase) + "\\b");
        return wordBoundary.matcher(query).find();
    }
}
