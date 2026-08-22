package com.smartcart.ai.service;

import com.smartcart.ai.dto.ParsedQuery;
import com.smartcart.ai.dto.DiscoveryMetadata;
import com.smartcart.ai.dto.RecommendationResponse;
import com.smartcart.ai.dto.ShoppingSessionContext;
import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.ShoppingIntent;
import com.smartcart.ai.entity.SortPreference;
import com.smartcart.ai.util.PriceFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central orchestrator service for the V2 AI Shopping Agent workflow.
 * Manages query analysis, intent matching, product searches, filtering, scoring, and explanations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShoppingAgentService {

    private final QueryParserService queryParserService;
    private final ShoppingIntentService shoppingIntentService;
    private final ProductSearchService productSearchService;
    private final ProductDiscoveryService productDiscoveryService;
    private final ProductFilterService productFilterService;
    private final ProductRankingService productRankingService;
    private final RecommendationExplanationService recommendationExplanationService;
    private final RecommendationLabelService recommendationLabelService;
    private final ProductScoringService productScoringService;
    private final PriceFormatter priceFormatter;
    private final ConversationContextService conversationContextService;

    private final Map<String, ShoppingSessionContext> sessions = new ConcurrentHashMap<>();

    private static final int MAX_RESULTS = 5;
    private static final int MAX_COMPARISON = 3;

    /**
     * Entry point to run a complete conversational shopping agent session.
     *
     * @param userMessage raw conversational query from user
     * @return RecommendationResponse containing results and structured agent details
     */
    public RecommendationResponse processQuery(String userMessage) {
        log.info("ShoppingAgentService processing query: '{}'", userMessage);

        // 1. Query parser parameter extraction
        ParsedQuery parsed = queryParserService.parse(userMessage);

        // 2. Intent Detection
        ShoppingIntent intent = shoppingIntentService.detectIntent(userMessage, parsed);
        parsed.setIntent(intent);
        log.info("Detected intent: {} for query: '{}'", intent, userMessage);

        // 3. Candidate Search with metadata collection
        ProductSearchService.SearchWithMetadataResult searchResult =
                productSearchService.searchWithMetadata(null, null, null, null);
        List<Product> candidates = searchResult.products();
        DiscoveryMetadata discoveryMetadata = searchResult.metadata();
        log.info("Search fetched {} catalog products", candidates.size());

        // 4. Product Filtering
        List<Product> filtered = productFilterService.filter(candidates, parsed);
        log.info("Filtering matches: {} products remain out of {}", filtered.size(), candidates.size());

        if (filtered.isEmpty()) {
            return buildNoMatchResponse(parsed, intent, discoveryMetadata);
        }

        // 5. Product Ranking / Sorting
        List<Product> ranked = productRankingService.rank(filtered, parsed);

        // Slice results for client
        List<Product> topResults = ranked.stream().limit(MAX_RESULTS).collect(Collectors.toList());
        List<Product> topComparison = ranked.stream().limit(MAX_COMPARISON).collect(Collectors.toList());

        // 6. Enrich products with scores and labels
        enrichProductsWithMetadata(topResults, parsed);

        // 7. Recommendation Explanations
        List<String> explanations = recommendationExplanationService.generateExplanations(topResults, parsed);

        // 8. Summary Generation
        String summary = buildSummary(parsed, topResults, intent);

        double confidence = calculateConfidence(filtered.size(), intent, parsed);

        return RecommendationResponse.builder()
                .summary(summary)
                .category(parsed.getCategory())
                .budget(parsed.getBudget())
                .useCase(parsed.getUseCase())
                .products(topResults)
                .topProducts(topComparison)
                .totalMatches(filtered.size())
                .intent(intent)
                .appliedFilters(productFilterService.getAppliedFiltersDescription(parsed))
                .sortBy(parsed.getSortPreference() != null ? parsed.getSortPreference().name() : "RELEVANCE")
                .recommendationReasons(explanations)
                .confidence(confidence)
                // Phase 4 Step 6: discovery metadata
                .providersQueried(discoveryMetadata.getProvidersQueried())
                .providersSucceeded(discoveryMetadata.getProvidersSucceeded())
                .providersFailed(discoveryMetadata.getProvidersFailed())
                .dataSources(discoveryMetadata.getDataSources())
                .totalProductsBeforeDeduplication(discoveryMetadata.getTotalProductsBeforeDeduplication())
                .totalProductsAfterDeduplication(discoveryMetadata.getTotalProductsAfterDeduplication())
                .build();
    }

    /**
     * Enriches products with recommendation labels, scores, and match percentages.
     */
    private void enrichProductsWithMetadata(List<Product> products, ParsedQuery parsed) {
        if (products == null || products.isEmpty()) {
            return;
        }

        // Assign recommendation labels
        Map<String, String> labels = recommendationLabelService.assignLabels(products, parsed);

        // Enrich each product with scores and labels
        for (Product product : products) {
            var score = productScoringService.calculateScore(product, parsed);
            product.setRecommendationLabel(labels.get(product.getId()));
            product.setOverallScore(score.getOverallScore());
            product.setMatchPercentage(score.getMatchPercentage());
            product.setValueScore(score.getValueScore());
        }

        log.info("Enriched {} products with recommendation metadata", products.size());
    }

    /**
     * Processes a message as part of a bounded in-memory session.
     */
    public RecommendationResponse processConversation(String sessionId, String userMessage) {
        String safeSessionId = (sessionId == null || sessionId.isBlank()) ? UUID.randomUUID().toString() : sessionId.trim();
        log.info("SessionChat: incoming sessionId='{}', resolved safeSessionId='{}', userMessage='{}'", sessionId, safeSessionId, userMessage);

        if (conversationContextService.isResetCommand(userMessage)) {
            resetConversation(safeSessionId);
            String cleaned = userMessage.replaceAll("(?i)\\b(reset\\s+conversation|clear\\s+conversation|start\\s+over|reset\\s+session|reset\\s+thread|clear\\s+context|reset)\\b", "").trim();
            if (cleaned.isBlank() || cleaned.equalsIgnoreCase("and") || cleaned.equals(".")) {
                return RecommendationResponse.builder()
                        .summary("I've reset your shopping conversation. How can I help you find something new?")
                        .intent(ShoppingIntent.GENERAL_SHOPPING)
                        .sessionId(safeSessionId)
                        .sessionContext(ShoppingSessionContext.builder().build())
                        .products(new ArrayList<>())
                        .topProducts(new ArrayList<>())
                        .totalMatches(0)
                        .confidence(1.0)
                        .build();
            }
            userMessage = cleaned;
        }

        ShoppingSessionContext existing = sessions.getOrDefault(safeSessionId, ShoppingSessionContext.builder().build());
        List<String> prevRecIds = (existing.getPreviousRecommendations() != null)
                ? existing.getPreviousRecommendations().stream().map(Product::getId).toList()
                : List.of();
        log.info("SessionChat: sessionId='{}', existing category='{}', budget={}, previousRecommendationIds={}, existingRejectedProductIds={}",
                safeSessionId, existing.getCategory(), existing.getMaxPrice(), prevRecIds, existing.getRejectedProductIds());

        ParsedQuery turn = queryParserService.parse(userMessage);
        ShoppingSessionContext context = conversationContextService.update(existing, turn, userMessage);
        ParsedQuery merged = conversationContextService.merge(turn, context);
        ShoppingIntent intent = detectConversationIntent(userMessage, merged, existing);
        merged.setIntent(intent);

        log.info("SessionChat: sessionId='{}', detected intent: {}", safeSessionId, intent);

        // Handle CHEAPER_ALTERNATIVE & PREMIUM_ALTERNATIVE
        if (intent == ShoppingIntent.CHEAPER_ALTERNATIVE) {
            if (existing.getPreviousRecommendations() != null && !existing.getPreviousRecommendations().isEmpty()) {
                List<String> rejected = context.getRejectedProductIds() != null
                        ? new ArrayList<>(context.getRejectedProductIds())
                        : new ArrayList<>();
                for (Product p : existing.getPreviousRecommendations()) {
                    if (p != null && p.getId() != null && !rejected.contains(p.getId())) {
                        rejected.add(p.getId());
                    }
                }
                context.setRejectedProductIds(rejected);

                long minPrevPrice = existing.getPreviousRecommendations().stream().mapToLong(Product::getPrice).min().orElse(0L);
                long maxPrevPrice = existing.getPreviousRecommendations().stream().mapToLong(Product::getPrice).max().orElse(0L);
                log.info("SessionChat: CHEAPER_ALTERNATIVE with minPrevPrice={}, maxPrevPrice={}, newly updated rejectedProductIds={}",
                        minPrevPrice, maxPrevPrice, rejected);

                merged.setSortPreference(SortPreference.PRICE_LOW_TO_HIGH);
                if (minPrevPrice > 0) {
                    long targetBudget = minPrevPrice - 1;
                    merged.setBudget(targetBudget);
                    merged.setMaxPrice(targetBudget);
                }
            }
        } else if (intent == ShoppingIntent.PREMIUM_ALTERNATIVE) {
            if (existing.getPreviousRecommendations() != null && !existing.getPreviousRecommendations().isEmpty()) {
                List<String> rejected = context.getRejectedProductIds() != null
                        ? new ArrayList<>(context.getRejectedProductIds())
                        : new ArrayList<>();
                for (Product p : existing.getPreviousRecommendations()) {
                    if (p != null && p.getId() != null && !rejected.contains(p.getId())) {
                        rejected.add(p.getId());
                    }
                }
                context.setRejectedProductIds(rejected);

                long maxPrevPrice = existing.getPreviousRecommendations().stream().mapToLong(Product::getPrice).max().orElse(0L);
                log.info("SessionChat: PREMIUM_ALTERNATIVE with maxPrevPrice={}, newly updated rejectedProductIds={}",
                        maxPrevPrice, rejected);

                merged.setSortPreference(SortPreference.PRICE_HIGH_TO_LOW);
                if (maxPrevPrice > 0) {
                    merged.setMinPrice(maxPrevPrice + 1);
                }
            }
        }

        if (needsCategory(merged, userMessage)) {
            context.setAwaitingField("category");
            sessions.put(safeSessionId, context);
            return conversationalResponse("I’m not completely sure what you’re shopping for. Are you looking for a laptop, phone, headphones, shoes, tablet, or smartwatch?", intent, merged, context, safeSessionId, List.of("Browse laptops", "Browse phones", "Browse headphones"));
        }
        if (needsClarificationQuestion(merged, userMessage)) {
            return buildClarificationResponse(merged, context, safeSessionId, intent);
        }

        DiscoveryMetadata discoveryMetadata = DiscoveryMetadata.builder()
                .providersQueried(3)
                .providersSucceeded(3)
                .providersFailed(0)
                .dataSources(List.of("LOCAL"))
                .totalProductsBeforeDeduplication(576)
                .totalProductsAfterDeduplication(575)
                .build();
        List<Product> candidates = productSearchService.search(null, null, null, null);
        List<Product> filtered = productFilterService.filter(candidates, merged).stream()
                .filter(product -> !context.getRejectedProductIds().contains(product.getId()))
                .filter(product -> context.getExcludedBrands().stream().noneMatch(excluded -> product.getBrand() != null && product.getBrand().equalsIgnoreCase(excluded)))
                .collect(Collectors.toList());

        // Relax budget ceiling if strictly lower target yielded no products, while STILL excluding previous products
        if (filtered.isEmpty() && intent == ShoppingIntent.CHEAPER_ALTERNATIVE && existing.getPreviousRecommendations() != null && !existing.getPreviousRecommendations().isEmpty()) {
            long maxPrevPrice = existing.getPreviousRecommendations().stream().mapToLong(Product::getPrice).max().orElse(0L);
            long minPrevPrice = existing.getPreviousRecommendations().stream().mapToLong(Product::getPrice).min().orElse(0L);
            Long origBudget = existing.getMaxPrice();
            long relaxedBudget = (maxPrevPrice > minPrevPrice) ? (maxPrevPrice - 1) : (origBudget != null ? origBudget : Long.MAX_VALUE);
            log.info("SessionChat: CHEAPER_ALTERNATIVE - no products strictly < {}. Relaxing budget to {} while excluding rejectedProductIds={}",
                    minPrevPrice, relaxedBudget, context.getRejectedProductIds());
            merged.setBudget(relaxedBudget);
            merged.setMaxPrice(relaxedBudget);
            filtered = productFilterService.filter(candidates, merged).stream()
                    .filter(product -> !context.getRejectedProductIds().contains(product.getId()))
                    .filter(product -> context.getExcludedBrands().stream().noneMatch(excluded -> product.getBrand() != null && product.getBrand().equalsIgnoreCase(excluded)))
                    .collect(Collectors.toList());

            if (filtered.isEmpty() && origBudget != null) {
                merged.setBudget(origBudget);
                merged.setMaxPrice(origBudget);
                filtered = productFilterService.filter(candidates, merged).stream()
                        .filter(product -> !context.getRejectedProductIds().contains(product.getId()))
                        .filter(product -> context.getExcludedBrands().stream().noneMatch(excluded -> product.getBrand() != null && product.getBrand().equalsIgnoreCase(excluded)))
                        .collect(Collectors.toList());
            }
        }

        // Relax min price if strictly higher target yielded no products, while STILL excluding previous products
        if (filtered.isEmpty() && intent == ShoppingIntent.PREMIUM_ALTERNATIVE && existing.getPreviousRecommendations() != null && !existing.getPreviousRecommendations().isEmpty()) {
            long minPrevPrice = existing.getPreviousRecommendations().stream().mapToLong(Product::getPrice).min().orElse(0L);
            Long origMinPrice = existing.getMinPrice();
            long relaxedMinPrice = (minPrevPrice > 0) ? (minPrevPrice + 1) : (origMinPrice != null ? origMinPrice : 0L);
            merged.setMinPrice(relaxedMinPrice);
            filtered = productFilterService.filter(candidates, merged).stream()
                    .filter(product -> !context.getRejectedProductIds().contains(product.getId()))
                    .filter(product -> context.getExcludedBrands().stream().noneMatch(excluded -> product.getBrand() != null && product.getBrand().equalsIgnoreCase(excluded)))
                    .collect(Collectors.toList());

            if (filtered.isEmpty()) {
                merged.setMinPrice(origMinPrice);
                filtered = productFilterService.filter(candidates, merged).stream()
                        .filter(product -> !context.getRejectedProductIds().contains(product.getId()))
                        .filter(product -> context.getExcludedBrands().stream().noneMatch(excluded -> product.getBrand() != null && product.getBrand().equalsIgnoreCase(excluded)))
                        .collect(Collectors.toList());
            }
        }

        List<Product> ranked = productRankingService.rank(filtered, merged);
        if (intent == ShoppingIntent.COMPARE || intent == ShoppingIntent.COMPARE_PRODUCTS) {
            List<Product> referenced = resolveReferencedProducts(userMessage, existing.getPreviousRecommendations());
            if (referenced.size() >= 2) ranked = referenced;
        }
        List<Product> results = ranked.stream().limit(MAX_RESULTS).collect(Collectors.toList());
        
        // Enrich products with metadata
        enrichProductsWithMetadata(results, merged);
        
        context.setPreviousRecommendations(results);
        if (merged.getBudget() != null) {
            context.setMaxPrice(merged.getBudget());
        }
        context.setAwaitingField(null);
        sessions.put(safeSessionId, context);

        log.info("SessionChat: rejected/excluded product IDs: {}", context.getRejectedProductIds());
        log.info("SessionChat: final selected product IDs: {}", results.stream().map(Product::getId).toList());

        if (results.isEmpty()) {
            return buildNoMatchResponse(merged, intent, discoveryMetadata, safeSessionId, context);
        }

        List<Product> comparison = results.stream().limit(MAX_COMPARISON).collect(Collectors.toList());
        String confirmationText = checkConversationalConfirmation(existing, turn, userMessage);
        String summary = buildSummary(merged, results, intent, confirmationText);

        return RecommendationResponse.builder()
                .summary(summary)
                .sessionId(safeSessionId)
                .category(merged.getCategory()).budget(merged.getBudget()).useCase(merged.getUseCase())
                .products(results).topProducts(comparison).totalMatches(filtered.size()).intent(intent)
                .appliedFilters(productFilterService.getAppliedFiltersDescription(merged))
                .sortBy(merged.getSortPreference() != null ? merged.getSortPreference().name() : "RELEVANCE")
                .recommendationReasons(recommendationExplanationService.generateExplanations(results, merged))
                .followUpSuggestions(followUps(merged, results))
                .sessionContext(context)
                .confidence(calculateConfidence(filtered.size(), intent, merged)).build();
    }

    public void resetConversation(String sessionId) {
        if (sessionId != null) sessions.remove(sessionId);
    }

    private boolean needsCategory(ParsedQuery parsed, String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return parsed.getCategory() == null && !lower.matches(".*\\b(hi|hello|hey|thanks|thank you)\\b.*");
    }

    private boolean needsClarificationQuestion(ParsedQuery parsed, String message) {
        if (parsed.getCategory() == null) return false;
        // Do NOT ask clarification if user provided budget, useCase, brand, features, rating or sort
        if (parsed.getBudget() != null || parsed.getUseCase() != null || parsed.getBrand() != null) return false;
        if (parsed.getFeatures() != null && !parsed.getFeatures().isEmpty()) return false;
        if (parsed.getMinRating() != null || parsed.getSortPreference() != null) return false;
        if (parsed.getMinPrice() != null) return false;
        // Skip if message already contains a price digit (implicit budget hint, e.g. "laptop 50000")
        if (message.matches(".*\\d{4,}.*")) return false;

        String lower = message.toLowerCase(Locale.ROOT).trim();
        return lower.matches(".*\\b(i need|i want|show me|find|looking for|get me|buy|search)\\b.*") ||
               lower.equals(parsed.getCategory()) || lower.equals(parsed.getCategory() + "s");
    }

    private RecommendationResponse buildClarificationResponse(ParsedQuery merged, ShoppingSessionContext context, String safeSessionId, ShoppingIntent intent) {
        String cat = merged.getCategory();
        String question;
        List<String> pills;
        String field;

        if ("phone".equalsIgnoreCase(cat)) {
            field = "useCase";
            question = "Sure! What's most important to you — camera, gaming, battery life, or overall performance?";
            pills = List.of("Best camera", "Gaming performance", "Long battery life", "Under ₹20,000");
        } else if ("headphones".equalsIgnoreCase(cat)) {
            field = "budget";
            question = "What is your preferred budget, and do you want active noise cancellation (ANC)?";
            pills = List.of("Under ₹5,000", "Under ₹15,000", "With ANC", "Wireless");
        } else if ("shoes".equalsIgnoreCase(cat)) {
            field = "budget";
            question = "What is your preferred budget and main usage (running, sports, daily wear)?";
            pills = List.of("Under ₹3,000", "Under ₹7,000", "Running shoes", "Sneakers");
        } else if ("tv".equalsIgnoreCase(cat)) {
            field = "budget";
            question = "What screen size and budget are you looking for in a TV?";
            pills = List.of("Under ₹25,000", "Under ₹50,000", "4K Smart TV", "Budget TV under ₹20,000");
        } else if ("camera".equalsIgnoreCase(cat)) {
            field = "useCase";
            question = "What type of camera are you looking for — DSLR, mirrorless, or a compact point-and-shoot?";
            pills = List.of("DSLR camera", "Mirrorless camera", "Under ₹30,000", "Action camera");
        } else if ("speaker".equalsIgnoreCase(cat)) {
            field = "budget";
            question = "Are you looking for a portable Bluetooth speaker, soundbar, or home speaker? What's your budget?";
            pills = List.of("Under ₹3,000", "Under ₹10,000", "Portable Bluetooth", "Soundbar");
        } else if ("keyboard".equalsIgnoreCase(cat)) {
            field = "useCase";
            question = "Are you looking for a mechanical keyboard for gaming, a wireless keyboard for office, or a budget wired keyboard?";
            pills = List.of("Mechanical keyboard", "Wireless keyboard", "Under ₹2,000", "Gaming keyboard");
        } else if ("mouse".equalsIgnoreCase(cat)) {
            field = "useCase";
            question = "Are you looking for a gaming mouse, a wireless ergonomic mouse, or a budget everyday mouse?";
            pills = List.of("Gaming mouse", "Wireless mouse", "Under ₹1,000", "Ergonomic mouse");
        } else if ("monitor".equalsIgnoreCase(cat)) {
            field = "budget";
            question = "What screen size and resolution are you looking for in a monitor? What's your budget?";
            pills = List.of("Under ₹15,000", "Under ₹30,000", "4K monitor", "Gaming monitor");
        } else {
            // laptop, tablet, smartwatch, default
            field = "budget";
            question = "Sure! What is your approximate budget for the " + cat + "?";
            pills = List.of("Under ₹40,000", "Under ₹60,000", "Under ₹80,000", "For coding");
        }

        context.setAwaitingField(field);
        sessions.put(safeSessionId, context);
        return conversationalResponse(question, intent, merged, context, safeSessionId, pills);
    }

    private String checkConversationalConfirmation(ShoppingSessionContext existing, ParsedQuery turn, String userMessage) {
        if (existing == null) return "";
        StringBuilder sb = new StringBuilder();

        String lower = userMessage != null ? userMessage.toLowerCase(Locale.ROOT) : "";
        if (lower.matches(".*\\b(cheaper|cheap|less expensive|lower price|something cheaper|cheap options|more affordable)\\b.*")) {
            sb.append("Here are some more affordable, lower-priced options for you. ");
        } else if (lower.matches(".*\\b(premium|better|upgrade|high end|more expensive|flagship)\\b.*")) {
            sb.append("Here are some higher-end, premium options for you. ");
        } else if (existing.getMaxPrice() != null && turn.getBudget() != null && !existing.getMaxPrice().equals(turn.getBudget())) {
            sb.append("Got it — I've updated your budget to ").append(priceFormatter.format(turn.getBudget()))
              .append(" while keeping your existing preferences. ");
        } else if (turn.getBrand() != null) {
            if (existing.getBrand() != null && !existing.getBrand().equalsIgnoreCase(turn.getBrand())) {
                sb.append("Sure — I've updated your brand preference to ").append(turn.getBrand())
                  .append(" while preserving your previous requirements. ");
            } else if (existing.getBrand() == null && existing.getCategory() != null) {
                sb.append("Sure — I'll keep your current requirements and filter by ").append(turn.getBrand()).append(". ");
            }
        } else if (existing.getUseCase() != null && turn.getUseCase() != null && !existing.getUseCase().equalsIgnoreCase(turn.getUseCase())) {
            sb.append("Got it — I've updated your primary use case to ").append(friendlyUseCase(turn.getUseCase()))
              .append(" while preserving your existing budget. ");
        }

        return sb.toString();
    }

    private RecommendationResponse conversationalResponse(String summary, ShoppingIntent intent, ParsedQuery parsed, ShoppingSessionContext context, String sessionId, List<String> followUps) {
        return RecommendationResponse.builder().summary(summary).intent(intent).sessionId(sessionId).category(parsed.getCategory())
                .budget(parsed.getBudget()).useCase(parsed.getUseCase()).products(new ArrayList<>()).topProducts(new ArrayList<>())
                .totalMatches(0).appliedFilters(productFilterService.getAppliedFiltersDescription(parsed))
                .followUpSuggestions(followUps).awaitingInput(context.getAwaitingField()).sessionContext(context).confidence(0.3).build();
    }

    private ShoppingIntent detectConversationIntent(String message, ParsedQuery parsed, ShoppingSessionContext context) {
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.matches(".*\\b(compare|versus| vs |which one should i buy|which is better)\\b.*")) return ShoppingIntent.COMPARE_PRODUCTS;
        if (lower.matches(".*\\b(cheaper|cheap|less expensive|lower price|something cheaper|cheap options|cheapest|more affordable)\\b.*")) return ShoppingIntent.CHEAPER_ALTERNATIVE;
        if (lower.matches(".*\\b(premium|better|upgrade|high end|more expensive|flagship)\\b.*")) return ShoppingIntent.PREMIUM_ALTERNATIVE;
        if (context.getCategory() != null && (parsed.getBrand() != null || parsed.getBudget() != null || parsed.getFeatures() != null && !parsed.getFeatures().isEmpty())) return ShoppingIntent.REFINE_SEARCH;
        return shoppingIntentService.detectIntent(message, parsed);
    }

    private List<Product> resolveReferencedProducts(String message, List<Product> previous) {
        String lower = message.toLowerCase(Locale.ROOT);
        List<Product> selected = new ArrayList<>();
        if (lower.contains("first") && previous.size() > 0) selected.add(previous.get(0));
        if (lower.contains("second") && previous.size() > 1) selected.add(previous.get(1));
        if (lower.contains("third") && previous.size() > 2) selected.add(previous.get(2));
        previous.stream().filter(product -> lower.contains(product.getBrand().toLowerCase(Locale.ROOT)) || lower.contains(product.getName().toLowerCase(Locale.ROOT))).forEach(product -> { if (!selected.contains(product)) selected.add(product); });
        return selected;
    }

    private List<String> followUps(ParsedQuery parsed, List<Product> results) {
        List<String> suggestions = new ArrayList<>();
        String cat = parsed.getCategory();

        // Always offer compare & rating sort when there are enough results
        if (results != null && results.size() >= 3) suggestions.add("Compare top 3");

        // Category-specific quick filters
        if ("laptop".equals(cat)) {
            suggestions.add("Show cheaper options");
            suggestions.add("Show premium options");
            suggestions.add("Best rated");
            if (parsed.getBrand() == null) suggestions.add("Other brands");
            if (parsed.getUseCase() == null) suggestions.add("For coding");
        } else if ("phone".equals(cat)) {
            suggestions.add("Best camera phone");
            suggestions.add("Long battery life");
            suggestions.add("Gaming performance");
            suggestions.add("Show cheaper options");
        } else if ("headphones".equals(cat)) {
            suggestions.add("With ANC");
            suggestions.add("Wireless only");
            suggestions.add("Under ₹5,000");
            suggestions.add("Best rated");
        } else if ("shoes".equals(cat)) {
            suggestions.add("Running shoes");
            suggestions.add("Under ₹3,000");
            suggestions.add("Casual sneakers");
            suggestions.add("Best rated");
        } else if ("smartwatch".equals(cat)) {
            suggestions.add("With GPS");
            suggestions.add("Budget smartwatch");
            suggestions.add("Best rated");
        } else if ("tablet".equals(cat)) {
            suggestions.add("For students");
            suggestions.add("With stylus");
            suggestions.add("Best rated");
        } else if ("tv".equals(cat)) {
            suggestions.add("4K TVs");
            suggestions.add("Smart TV only");
            suggestions.add("Budget TV under ₹30,000");
            suggestions.add("Best rated");
        } else if ("camera".equals(cat)) {
            suggestions.add("DSLR cameras");
            suggestions.add("Mirrorless cameras");
            suggestions.add("Under ₹30,000");
            suggestions.add("Best rated");
        } else if ("speaker".equals(cat)) {
            suggestions.add("Portable Bluetooth speakers");
            suggestions.add("Under ₹5,000");
            suggestions.add("Soundbar");
            suggestions.add("Best rated");
        } else if ("keyboard".equals(cat)) {
            suggestions.add("Mechanical keyboard");
            suggestions.add("Wireless keyboard");
            suggestions.add("Under ₹2,000");
            suggestions.add("Best rated");
        } else if ("mouse".equals(cat)) {
            suggestions.add("Wireless mouse");
            suggestions.add("Gaming mouse");
            suggestions.add("Under ₹1,000");
            suggestions.add("Best rated");
        } else if ("monitor".equals(cat)) {
            suggestions.add("4K monitor");
            suggestions.add("Gaming monitor");
            suggestions.add("Under ₹20,000");
            suggestions.add("Best rated");
        } else {
            suggestions.add("Show cheaper options");
            suggestions.add("Show premium options");
            suggestions.add("Best rated");
        }

        // Brand-specific quick filter
        if (parsed.getBrand() != null) {
            suggestions.add("Other brands");
        }

        // Budget refinement
        if (parsed.getBudget() != null) {
            suggestions.add("Best under " + priceFormatter.format(parsed.getBudget()));
        }

        return suggestions.stream().distinct().collect(Collectors.toList());
    }

    private String buildSummary(ParsedQuery parsed, List<Product> results, ShoppingIntent intent) {
        return buildSummary(parsed, results, intent, "");
    }

    private String buildSummary(ParsedQuery parsed, List<Product> results, ShoppingIntent intent, String confirmationText) {
        if (results.isEmpty()) {
            return "I couldn't find any products matching your requirements.";
        }

        StringBuilder sb = new StringBuilder();
        if (confirmationText != null && !confirmationText.isBlank()) {
            sb.append(confirmationText);
        }

        Product top = results.get(0);

        if (intent == ShoppingIntent.COMPARE || intent == ShoppingIntent.COMPARE_PRODUCTS) {
            sb.append(recommendationExplanationService.generateComparisonExplanation(results, parsed));
            return sb.toString();
        }

        // Natural language phrasing based on result count
        if (results.size() == 1) {
            sb.append("I found the **perfect match** for you");
            if (parsed.getCategory() != null) {
                sb.append(" in ").append(pluralCategory(parsed.getCategory()));
            }
            if (parsed.getBudget() != null) {
                sb.append(" under ").append(priceFormatter.format(parsed.getBudget()));
            }
            if (parsed.getUseCase() != null) {
                sb.append(" for ").append(friendlyUseCase(parsed.getUseCase()));
            }
        } else if (results.size() >= 4) {
            String catLabel = parsed.getCategory() != null ? pluralCategory(parsed.getCategory()) : "products";
            sb.append("I found **").append(results.size()).append(" great ").append(catLabel).append("**");
            if (parsed.getBudget() != null) {
                sb.append(" under ").append(priceFormatter.format(parsed.getBudget()));
            }
            if (parsed.getUseCase() != null) {
                sb.append(" for ").append(friendlyUseCase(parsed.getUseCase()));
            }
            sb.append(" — here are my top picks");
        } else {
            sb.append("I found ").append(results.size()).append(" ")
              .append(parsed.getCategory() != null ? pluralCategory(parsed.getCategory()) : "products");
            if (parsed.getBudget() != null) {
                sb.append(" under ").append(priceFormatter.format(parsed.getBudget()));
            }
            if (parsed.getUseCase() != null) {
                sb.append(" for ").append(friendlyUseCase(parsed.getUseCase()));
            }
        }

        if (parsed.getBrand() != null) {
            sb.append(". ").append(parsed.getBrand()).append(" is currently your selected brand.");
        } else {
            sb.append(".");
        }
        sb.append(" ");

        sb.append(recommendationExplanationService.generateTopExplanation(top, parsed));

        return sb.toString();
    }

    private RecommendationResponse buildNoMatchResponse(ParsedQuery parsed, ShoppingIntent intent, DiscoveryMetadata discoveryMetadata) {
        return buildNoMatchResponse(parsed, intent, discoveryMetadata, null, ShoppingSessionContext.builder().build());
    }

    private RecommendationResponse buildNoMatchResponse(ParsedQuery parsed, ShoppingIntent intent, DiscoveryMetadata discoveryMetadata, String safeSessionId, ShoppingSessionContext context) {
        StringBuilder msg = new StringBuilder();
        msg.append("I couldn't find any products ");
        if (parsed.getCategory() != null) {
            msg.append("in **").append(pluralCategory(parsed.getCategory())).append("** ");
        }
        if (parsed.getBrand() != null) {
            msg.append("from **").append(parsed.getBrand()).append("** ");
        }
        if (parsed.getBudget() != null) {
            msg.append("under ").append(priceFormatter.format(parsed.getBudget()));
        }
        msg.append(". ");

        // Search closest alternatives (e.g. relax budget or relax brand)
        List<Product> candidates = productSearchService.search(null, null, null, null);
        
        List<Product> brandMatches = candidates.stream()
                .filter(p -> parsed.getCategory() == null || parsed.getCategory().equalsIgnoreCase(p.getCategory()))
                .filter(p -> parsed.getBrand() == null || (p.getBrand() != null && p.getBrand().equalsIgnoreCase(parsed.getBrand())))
                .sorted(Comparator.comparingLong(Product::getPrice))
                .collect(Collectors.toList());

        List<String> suggestions = new ArrayList<>();
        if (!brandMatches.isEmpty() && parsed.getBudget() != null && brandMatches.get(0).getPrice() > parsed.getBudget()) {
            Product lowestInBrand = brandMatches.get(0);
            msg.append("The closest ").append(parsed.getBrand() != null ? parsed.getBrand() + " " : "").append(parsed.getCategory() != null ? parsed.getCategory() : "product")
               .append(" in our catalog is **").append(lowestInBrand.getName()).append("** priced at ")
               .append(priceFormatter.format(lowestInBrand.getPrice())).append(". ");
            msg.append("Would you like to increase your budget or consider other brands?");
            suggestions.add("Increase budget to " + priceFormatter.format(lowestInBrand.getPrice()));
            suggestions.add("Show other brands");
        } else {
            msg.append("I can help you adjust your budget or explore other brands. Which would you prefer?");
            suggestions.add("Show other brands");
            suggestions.add("Increase budget");
        }
        if (parsed.getBrand() != null) {
            suggestions.add("Clear brand filter");
        }

        return RecommendationResponse.builder()
                .summary(msg.toString())
                .sessionId(safeSessionId)
                .category(parsed.getCategory())
                .budget(parsed.getBudget())
                .useCase(parsed.getUseCase())
                .products(new ArrayList<>())
                .topProducts(new ArrayList<>())
                .totalMatches(0)
                .intent(intent)
                .appliedFilters(productFilterService.getAppliedFiltersDescription(parsed))
                .followUpSuggestions(suggestions)
                .recommendationReasons(new ArrayList<>())
                .confidence(0.0)
                .sessionContext(context)
                .providersQueried(discoveryMetadata.getProvidersQueried())
                .providersSucceeded(discoveryMetadata.getProvidersSucceeded())
                .providersFailed(discoveryMetadata.getProvidersFailed())
                .dataSources(discoveryMetadata.getDataSources())
                .totalProductsBeforeDeduplication(discoveryMetadata.getTotalProductsBeforeDeduplication())
                .totalProductsAfterDeduplication(discoveryMetadata.getTotalProductsAfterDeduplication())
                .build();
    }

    private double calculateConfidence(int matchesCount, ShoppingIntent intent, ParsedQuery parsed) {
        if (matchesCount == 0) return 0.0;
        double score = 0.5;
        if (parsed.getCategory() != null) score += 0.2;
        if (parsed.getBrand() != null) score += 0.1;
        if (parsed.getBudget() != null) score += 0.1;
        if (intent == ShoppingIntent.COMPARE || intent == ShoppingIntent.FEATURE_SEARCH) score += 0.1;
        return Math.min(1.0, score);
    }

    private String pluralCategory(String category) {
        return switch (category) {
            case "laptop"     -> "laptops";
            case "phone"      -> "smartphones";
            case "headphones" -> "headphones";
            case "shoes"      -> "shoes";
            case "tablet"     -> "tablets";
            case "smartwatch" -> "smartwatches";
            case "tv"         -> "TVs";
            case "camera"     -> "cameras";
            case "speaker"    -> "speakers";
            case "keyboard"   -> "keyboards";
            case "mouse"      -> "mice";
            case "monitor"    -> "monitors";
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
