package com.smartcart.ai.service;

import com.smartcart.ai.dto.ParsedQuery;
import com.smartcart.ai.dto.RecommendationResponse;
import com.smartcart.ai.entity.Product;
import com.smartcart.ai.util.PriceFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core recommendation engine for ShopSmart AI.
 *
 * <p><b>Pipeline:</b>
 * <ol>
 *   <li><b>Parse</b>   — Delegates to {@link QueryParserService}</li>
 *   <li><b>Filter</b>  — Hard constraints: category (exact) + budget (price ≤ budget)</li>
 *   <li><b>Score</b>   — 5-factor weighted formula (see {@link #score})</li>
 *   <li><b>Rank</b>    — Descending by score, top 5 returned</li>
 *   <li><b>Summarise</b> — Template-driven natural-language summary</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    // ── Scoring weights (tunable constants) ──────────────────────────────────
    private static final double WEIGHT_RATING        = 15.0; // Rating × 15   (max ~75)
    private static final double WEIGHT_USE_CASE      = 20.0; // Primary useCase match bonus
    private static final double WEIGHT_KEYWORD       = 6.0;  // Per additional keyword match
    private static final double WEIGHT_PRICE_EFFICIENCY = 12.0; // Cheaper within budget bonus
    private static final double WEIGHT_POPULARITY    = 2.5;  // log10(reviews) × 2.5

    // ── Result limits ─────────────────────────────────────────────────────────
    private static final int MAX_RESULTS    = 5;
    private static final int MAX_COMPARISON = 3;

    private final List<Product>       productCatalog;
    private final QueryParserService  queryParserService;
    private final PriceFormatter      priceFormatter;

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Main entry point — accepts raw user message, returns full recommendation response.
     *
     * @param userMessage the user's natural-language shopping query
     * @return {@link RecommendationResponse} with summary, products, and comparison list
     */
    public RecommendationResponse recommend(String userMessage) {
        ParsedQuery parsed = queryParserService.parse(userMessage);

        // STEP 1 — Hard filter (category + budget are mandatory constraints if present)
        List<Product> filtered = applyHardFilters(parsed);
        log.info("Hard filter: {} products remain from catalog of {}", filtered.size(), productCatalog.size());

        if (filtered.isEmpty()) {
            return buildNoMatchResponse(parsed);
        }

        // STEP 2 — Score and rank
        List<Product> ranked = rankByScore(filtered, parsed);

        // STEP 3 — Slice results
        List<Product> topResults    = ranked.stream().limit(MAX_RESULTS).collect(Collectors.toList());
        List<Product> topComparison = ranked.stream().limit(MAX_COMPARISON).collect(Collectors.toList());

        // STEP 4 — Generate summary
        String summary = buildSummary(parsed, topResults);

        return RecommendationResponse.builder()
                .summary(summary)
                .category(parsed.getCategory())
                .budget(parsed.getBudget())
                .useCase(parsed.getUseCase())
                .products(topResults)
                .topProducts(topComparison)
                .totalMatches(filtered.size())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 1 — Hard Filters
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Applies mandatory constraints.
     *
     * <ul>
     *   <li><b>Category match is REQUIRED</b> if a category was detected.</li>
     *   <li><b>Price ≤ budget is REQUIRED</b> if a budget was detected.</li>
     * </ul>
     *
     * Both filters are null-safe: if not detected, no filtering on that dimension.
     */
    private List<Product> applyHardFilters(ParsedQuery parsed) {
        return productCatalog.stream()
                .filter(p -> passesCategory(p, parsed))
                .filter(p -> passesBudget(p, parsed))
                .collect(Collectors.toList());
    }

    /** Category match is EXACT (case-insensitive). Null category = no filtering. */
    private boolean passesCategory(Product p, ParsedQuery parsed) {
        if (parsed.getCategory() == null) return true;
        return p.getCategory().equalsIgnoreCase(parsed.getCategory());
    }

    /** Price must be ≤ detected budget. Null budget = no filtering. */
    private boolean passesBudget(Product p, ParsedQuery parsed) {
        if (parsed.getBudget() == null) return true;
        return p.getPrice() <= parsed.getBudget();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 2 — Scoring
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 5-factor weighted scoring formula.
     *
     * <pre>
     * ┌─────────────────────────────┬────────┬──────────────────────────────────┐
     * │ Factor                      │ Weight │ Rationale                        │
     * ├─────────────────────────────┼────────┼──────────────────────────────────┤
     * │ Rating quality              │ ×15    │ Base relevance from user reviews  │
     * │ Primary useCase match       │ +20    │ Exact match on dominant intent    │
     * │ Additional keyword matches  │ ×6     │ Boosts multi-attribute alignment  │
     * │ Price efficiency            │ ×12    │ Rewards lower price in budget     │
     * │ Popularity (log reviews)    │ ×2.5   │ Social proof signal               │
     * └─────────────────────────────┴────────┴──────────────────────────────────┘
     * </pre>
     *
     * Max theoretical score ≈ 75 + 20 + 30 + 12 + 10 = ~147 points.
     */
    private double score(Product p, ParsedQuery parsed) {
        double total = 0;

        // Factor 1 — Rating quality (0–75 pts)
        total += p.getRating() * WEIGHT_RATING;

        // Factor 2 — Primary useCase match (+20 pts flat bonus)
        if (parsed.getUseCase() != null
                && p.getTags() != null
                && p.getTags().contains(parsed.getUseCase())) {
            total += WEIGHT_USE_CASE;
        }

        // Factor 3 — Additional keyword matches (up to ~30 pts from 5 keywords)
        if (parsed.getKeywords() != null && !parsed.getKeywords().isEmpty()
                && p.getTags() != null) {
            long additionalMatches = parsed.getKeywords().stream()
                    // Skip the primary useCase — already rewarded above
                    .filter(kw -> !kw.equals(parsed.getUseCase()))
                    .filter(p.getTags()::contains)
                    .count();
            total += additionalMatches * WEIGHT_KEYWORD;
        }

        // Factor 4 — Price efficiency within budget (0–12 pts)
        // A product priced at 0% of the budget scores 12; at 100% it scores 0.
        if (parsed.getBudget() != null && parsed.getBudget() > 0
                && p.getPrice() <= parsed.getBudget()) {
            double efficiency = 1.0 - ((double) p.getPrice() / parsed.getBudget());
            total += efficiency * WEIGHT_PRICE_EFFICIENCY;
        }

        // Factor 5 — Popularity boost via log-scaled review count (0–~10 pts)
        if (p.getReviewCount() > 0) {
            total += Math.log10(p.getReviewCount()) * WEIGHT_POPULARITY;
        }

        return total;
    }

    /** Sorts the product list by score descending. */
    private List<Product> rankByScore(List<Product> products, ParsedQuery parsed) {
        return products.stream()
                .sorted(Comparator.comparingDouble((Product p) -> score(p, parsed)).reversed())
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STEP 4 — Summary generation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Builds a conversational, AI-style recommendation summary.
     * Purely template-driven — no external API calls.
     */
    private String buildSummary(ParsedQuery parsed, List<Product> results) {
        if (results.isEmpty()) return "I couldn't find any matching products.";

        Product top = results.get(0);
        StringBuilder sb = new StringBuilder();

        // ── Opening context line ──────────────────────────────────────────────
        sb.append("Here are my top picks");

        if (parsed.getCategory() != null) {
            sb.append(" for ").append(pluralCategory(parsed.getCategory()));
        }
        if (parsed.getBudget() != null) {
            sb.append(" under ").append(priceFormatter.format(parsed.getBudget()));
        }
        if (parsed.getUseCase() != null) {
            sb.append(", perfect for **").append(friendlyUseCase(parsed.getUseCase())).append("**");
        }
        sb.append(". ");

        // ── Top recommendation highlight ──────────────────────────────────────
        sb.append("My top pick is the **").append(top.getName()).append("**")
          .append(" by ").append(top.getBrand())
          .append(" at ").append(priceFormatter.format(top.getPrice()))
          .append(" with a ★ ").append(top.getRating()).append(" rating. ");

        // ── Use-case specific insight ─────────────────────────────────────────
        sb.append(useCaseInsight(parsed.getUseCase(), top));

        // ── Result count note ─────────────────────────────────────────────────
        if (results.size() > 1) {
            sb.append("I've found ")
              .append(results.size())
              .append(" great option").append(results.size() > 1 ? "s" : "")
              .append(" for you — scroll down to compare the top 3 side by side.");
        }

        return sb.toString();
    }

    /** Returns a use-case-specific sentence about the top product. */
    private String useCaseInsight(String useCase, Product top) {
        if (useCase == null) return "";

        return switch (useCase) {
            case "coding"   -> "It delivers excellent CPU performance and RAM for multitasking across IDEs and build tools. ";
            case "gaming"   -> "It handles demanding titles and heavy workloads with smooth framerates. ";
            case "camera"   -> "Its camera system is tuned for stunning photos in all lighting conditions. ";
            case "battery"  -> "Its long-lasting battery ensures you stay unplugged throughout the day. ";
            case "running"  -> "Its cushioning and lightweight build are engineered for performance runs. ";
            case "music"    -> "It delivers rich, immersive audio with deep bass and clear highs. ";
            case "office"   -> "It's ideal for calls, video meetings, and all-day productive work. ";
            case "anc"      -> "Its active noise cancellation blocks distractions so you can stay focused. ";
            case "travel"   -> "Its compact and foldable design makes it a great travel companion. ";
            case "student"  -> "It balances performance and affordability — ideal for students. ";
            case "budget"   -> "It offers excellent value for money without compromising on essentials. ";
            case "premium"  -> "It's a flagship-grade product built to impress with premium craftsmanship. ";
            default         -> "";
        };
    }

    /** Builds a "no match" response with a helpful hint. */
    private RecommendationResponse buildNoMatchResponse(ParsedQuery parsed) {
        StringBuilder msg = new StringBuilder("I couldn't find any products");

        if (parsed.getCategory() != null) {
            msg.append(" in **").append(pluralCategory(parsed.getCategory())).append("**");
        }
        if (parsed.getBudget() != null) {
            msg.append(" under ").append(priceFormatter.format(parsed.getBudget()));
        }
        msg.append(". Try increasing your budget or exploring a different category!");

        return RecommendationResponse.builder()
                .summary(msg.toString())
                .category(parsed.getCategory())
                .budget(parsed.getBudget())
                .useCase(parsed.getUseCase())
                .products(List.of())
                .topProducts(List.of())
                .totalMatches(0)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String pluralCategory(String category) {
        return switch (category) {
            case "laptop"     -> "laptops";
            case "phone"      -> "smartphones";
            case "headphones" -> "headphones";
            case "shoes"      -> "shoes";
            case "tablet"     -> "tablets";
            case "smartwatch" -> "smartwatches";
            default           -> category;
        };
    }

    private String friendlyUseCase(String useCase) {
        return switch (useCase) {
            case "coding"   -> "coding & development";
            case "gaming"   -> "gaming";
            case "camera"   -> "photography & camera";
            case "battery"  -> "all-day battery life";
            case "running"  -> "running & fitness";
            case "music"    -> "music & audio";
            case "office"   -> "office & productivity";
            case "anc"      -> "noise cancellation";
            case "travel"   -> "travel";
            case "student"  -> "student use";
            case "budget"   -> "value for money";
            case "premium"  -> "premium experience";
            default         -> useCase;
        };
    }
}
