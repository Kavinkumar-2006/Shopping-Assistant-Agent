package com.smartcart.ai.service;

import com.smartcart.ai.dto.ParsedQuery;
import com.smartcart.ai.dto.ProductScore;
import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.ProductSource;
import com.smartcart.ai.entity.SortPreference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for Phase 4 Step 5 intelligent ranking improvements.
 * Tests multi-factor scoring, budget intelligence, value-for-money, use-case matching,
 * brand preference, result diversity, and recommendation labels.
 */
class IntelligentRankingTest {

    private ProductScoringService scoringService;
    private ProductRankingService rankingService;
    private RecommendationLabelService labelService;
    private QueryParserService queryParserService;

    @BeforeEach
    void setUp() {
        scoringService = new ProductScoringService();
        rankingService = new ProductRankingService(scoringService);
        labelService = new RecommendationLabelService(scoringService);
        queryParserService = new QueryParserService();
    }

    @Test
    @DisplayName("Budget intelligence prefers products in 70-85% sweet spot")
    void testBudgetIntelligenceSweetSpot() {
        ParsedQuery query = ParsedQuery.builder()
                .budget(60000L)
                .category("laptop")
                .build();

        Product p1 = createLaptop("L1", "Budget Laptop", 20000, 4.5); // 33% of budget - too cheap
        Product p2 = createLaptop("L2", "Sweet Spot Laptop", 45000, 4.5); // 75% of budget - ideal
        Product p3 = createLaptop("L3", "Max Budget Laptop", 59000, 4.5); // 98% of budget - too high

        ProductScore score1 = scoringService.calculateScore(p1, query);
        ProductScore score2 = scoringService.calculateScore(p2, query);
        ProductScore score3 = scoringService.calculateScore(p3, query);

        // Product in sweet spot should have best budget score
        assertTrue(score2.getBudgetScore() > score1.getBudgetScore(), 
                "Sweet spot product should score higher than too-cheap product");
        assertTrue(score2.getBudgetScore() > score3.getBudgetScore(), 
                "Sweet spot product should score higher than max-budget product");
    }

    @Test
    @DisplayName("Use case matching validates specifications for coding")
    void testUseCaseSpecificationValidationCoding() {
        ParsedQuery query = ParsedQuery.builder()
                .category("laptop")
                .useCase("coding")
                .build();

        Product goodForCoding = createLaptop("L1", "Dev Laptop", 50000, 4.5);
        goodForCoding.setTags(List.of("coding", "office"));
        goodForCoding.setSpecs(Map.of(
                "RAM", "16 GB",
                "Storage", "512 GB SSD",
                "Processor", "Intel Core i7"
        ));

        Product poorForCoding = createLaptop("L2", "Basic Laptop", 30000, 4.5);
        poorForCoding.setTags(List.of("coding", "budget"));
        poorForCoding.setSpecs(Map.of(
                "RAM", "4 GB",
                "Storage", "1 TB HDD",
                "Processor", "Intel Core i3"
        ));

        ProductScore score1 = scoringService.calculateScore(goodForCoding, query);
        ProductScore score2 = scoringService.calculateScore(poorForCoding, query);

        // Laptop with better coding specs should have higher use case score
        assertTrue(score1.getUseCaseScore() > score2.getUseCaseScore(),
                "Laptop with 16GB RAM and SSD should score higher for coding");
    }

    @Test
    @DisplayName("Use case matching validates camera specs for photography")
    void testUseCaseSpecificationValidationPhotography() {
        ParsedQuery query = ParsedQuery.builder()
                .category("phone")
                .useCase("camera")
                .build();

        Product highMPCamera = createPhone("P1", "Camera Phone", 40000, 4.6);
        highMPCamera.setTags(List.of("camera", "photography"));
        highMPCamera.setSpecs(Map.of("Camera", "108MP + 8MP + 2MP"));

        Product lowMPCamera = createPhone("P2", "Basic Phone", 15000, 4.3);
        lowMPCamera.setTags(List.of("camera", "budget"));
        lowMPCamera.setSpecs(Map.of("Camera", "13MP"));

        ProductScore score1 = scoringService.calculateScore(highMPCamera, query);
        ProductScore score2 = scoringService.calculateScore(lowMPCamera, query);

        assertTrue(score1.getUseCaseScore() > score2.getUseCaseScore(),
                "Phone with 108MP camera should score higher for photography");
    }

    @Test
    @DisplayName("Value for money calculation balances rating and price")
    void testValueForMoneyCalculation() {
        ParsedQuery query = ParsedQuery.builder()
                .category("laptop")
                .build();

        Product expensiveLowRated = createLaptop("L1", "Expensive Laptop", 80000, 3.8);
        Product affordableHighRated = createLaptop("L2", "Value Laptop", 35000, 4.6);
        affordableHighRated.setReviewCount(5000);

        ProductScore score1 = scoringService.calculateScore(expensiveLowRated, query);
        ProductScore score2 = scoringService.calculateScore(affordableHighRated, query);

        // Affordable high-rated product should have better value score
        assertTrue(score2.getValueScore() > score1.getValueScore(),
                "Affordable high-rated product should have better value score");
    }

    @Test
    @DisplayName("Brand preference significantly boosts score when specified")
    void testBrandPreferenceBoost() {
        ParsedQuery query = ParsedQuery.builder()
                .category("laptop")
                .brand("Lenovo")
                .build();

        Product lenovo = createLaptop("L1", "Lenovo ThinkPad", 50000, 4.5);
        lenovo.setBrand("Lenovo");

        Product dell = createLaptop("L2", "Dell Inspiron", 50000, 4.6);
        dell.setBrand("Dell");

        ProductScore lenovoScore = scoringService.calculateScore(lenovo, query);
        ProductScore dellScore = scoringService.calculateScore(dell, query);

        // Lenovo should score higher despite slightly lower rating
        assertTrue(lenovoScore.getBrandScore() > 0, "Lenovo should have brand score");
        assertEquals(0.0, dellScore.getBrandScore(), "Dell should have no brand score");
        assertTrue(lenovoScore.getOverallScore() > dellScore.getOverallScore(),
                "Preferred brand should rank higher overall");
    }

    @Test
    @DisplayName("Rating quality is heavily weighted in scoring")
    void testRatingWeighting() {
        ParsedQuery query = ParsedQuery.builder().category("laptop").build();

        Product highRated = createLaptop("L1", "Premium Laptop", 50000, 4.8);
        Product lowRated = createLaptop("L2", "Budget Laptop", 50000, 3.5);

        ProductScore score1 = scoringService.calculateScore(highRated, query);
        ProductScore score2 = scoringService.calculateScore(lowRated, query);

        // Rating score should differ significantly
        double ratingDifference = score1.getRatingScore() - score2.getRatingScore();
        assertTrue(ratingDifference > 15.0, 
                "Rating difference of 1.3 stars should result in significant score difference");
    }

    @Test
    @DisplayName("Popularity uses logarithmic scale to prevent bias")
    void testPopularityLogarithmicScale() {
        ParsedQuery query = ParsedQuery.builder().category("laptop").build();

        Product p1 = createLaptop("L1", "Laptop 1", 50000, 4.5);
        p1.setReviewCount(1000);

        Product p2 = createLaptop("L2", "Laptop 2", 50000, 4.5);
        p2.setReviewCount(50000);

        ProductScore score1 = scoringService.calculateScore(p1, query);
        ProductScore score2 = scoringService.calculateScore(p2, query);

        // Popularity difference should exist but not be overwhelming
        double popDifference = score2.getPopularityScore() - score1.getPopularityScore();
        assertTrue(popDifference > 0, "More reviews should increase popularity score");
        assertTrue(popDifference < 5.0, "Logarithmic scale should limit popularity impact");
    }

    @Test
    @DisplayName("VALUE sort mode ranks by value score")
    void testValueSortMode() {
        ParsedQuery query = ParsedQuery.builder()
                .category("laptop")
                .sortPreference(SortPreference.VALUE)
                .build();

        // L1: expensive, mediocre rating, few reviews → low value
        Product expensive = createLaptop("L1", "Premium", 80000, 4.0);
        expensive.setReviewCount(200);

        // L2: moderate price, very high rating, many reviews → best value
        // value = (4.9²/4.0)*2.5 + 3.5 = 15.0 + 3.5 = 18.5
        Product value = createLaptop("L2", "Value", 40000, 4.9);
        value.setReviewCount(15000);

        // L3: moderate-cheap, decent rating, very few reviews → lower value than L2
        // effective floor = max(2.5, 1.5) = 2.5
        // value = (4.3²/2.5)*2.5 + 0.3 = 18.49 + 0.3 = 18.79 → about same as L2
        // Let's keep L3 at low reviews (100) and rating 4.0 to ensure L2 wins
        // L3: (4.0²/2.5)*2.5 + 0.8 = 16.0 + 0.8 = 16.8 → L2 wins
        Product midsizer = createLaptop("L3", "Mid", 25000, 4.0);
        midsizer.setReviewCount(150);

        List<Product> products = new ArrayList<>(List.of(expensive, midsizer, value));
        List<Product> ranked = rankingService.rank(products, query);

        // L2 (rating 4.9, 15k reviews) should beat L1 and L3 on value
        assertEquals("L2", ranked.get(0).getId(), "High-rated product with many reviews should rank first by value");
    }

    @Test
    @DisplayName("Result diversity prevents consecutive same-brand products")
    void testResultDiversity() {
        ParsedQuery query = ParsedQuery.builder()
                .category("laptop")
                .build();

        // Create 8 Lenovo laptops with slightly decreasing scores
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Product p = createLaptop("L" + i, "Lenovo Laptop " + i, 50000, 4.5 - i * 0.05);
            p.setBrand("Lenovo");
            products.add(p);
        }

        // Dell positioned to score between 2nd and 3rd Lenovo (so it naturally appears in top 5)
        Product dell1 = createLaptop("D1", "Dell Alpha", 50000, 4.4);
        dell1.setBrand("Dell");
        Product dell2 = createLaptop("D2", "Dell Beta", 50000, 4.3);
        dell2.setBrand("Dell");
        products.add(dell1);
        products.add(dell2);

        List<Product> ranked = rankingService.rank(products, query);

        // Verify diversity: no 3+ consecutive same-brand in first 7 results
        int maxConsecutive = 0;
        int currentConsecutive = 0;
        String prevBrand = null;
        for (Product p : ranked.subList(0, Math.min(7, ranked.size()))) {
            if (p.getBrand().equals(prevBrand)) {
                currentConsecutive++;
            } else {
                currentConsecutive = 1;
            }
            maxConsecutive = Math.max(maxConsecutive, currentConsecutive);
            prevBrand = p.getBrand();
        }

        assertTrue(maxConsecutive <= 2,
                "Diversity should prevent 3+ consecutive same-brand products, max consecutive was: " + maxConsecutive);
    }

    @Test
    @DisplayName("Explicit brand filter disables diversity")
    void testExplicitBrandFilterDisablesDiversity() {
        ParsedQuery query = ParsedQuery.builder()
                .category("laptop")
                .brand("Lenovo")
                .build();

        List<Product> products = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Product p = createLaptop("L" + i, "Lenovo Laptop " + i, 50000 + i * 1000, 4.5);
            p.setBrand("Lenovo");
            products.add(p);
        }

        List<Product> ranked = rankingService.rank(products, query);

        // All products should be Lenovo (no diversity applied)
        assertTrue(ranked.stream().allMatch(p -> "Lenovo".equals(p.getBrand())),
                "When brand is explicitly filtered, diversity should not apply");
    }

    @Test
    @DisplayName("Recommendation labels are assigned correctly")
    void testRecommendationLabels() {
        ParsedQuery query = ParsedQuery.builder()
                .category("laptop")
                .useCase("coding")
                .budget(60000L)
                .build();

        // L1: no coding tag — available for Best Overall after Best for Coding is assigned
        Product p1 = createLaptop("L1", "Premium Laptop", 55000, 4.8);
        p1.setTags(List.of("business", "premium"));
        p1.setSpecs(Map.of("RAM", "16 GB", "Storage", "512 GB SSD"));
        p1.setReviewCount(10000);

        // L2: coding tag + decent specs — candidate for Best for Coding
        Product p2 = createLaptop("L2", "Dev Laptop", 45000, 4.5);
        p2.setTags(List.of("coding", "office"));
        p2.setSpecs(new HashMap<>(Map.of("RAM", "16 GB", "Storage", "512 GB SSD")));
        p2.setReviewCount(5000);

        // L3: cheapest with good rating — candidate for Best Budget
        Product p3 = createLaptop("L3", "Budget Laptop", 22000, 4.2);
        p3.setTags(List.of("student", "everyday"));
        p3.setReviewCount(3000);

        // L4: highest rating — candidate for Best Rated
        Product p4 = createLaptop("L4", "Top Rated", 48000, 4.9);
        p4.setTags(List.of("premium", "business"));
        p4.setReviewCount(12000);

        // L5: extra product to ensure labels spread across more candidates
        Product p5 = createLaptop("L5", "Value Laptop", 30000, 4.6);
        p5.setTags(List.of("everyday", "travel"));
        p5.setReviewCount(6000);

        List<Product> products = List.of(p1, p2, p3, p4, p5);
        Map<String, String> labels = labelService.assignLabels(products, query);

        // Verify core label types are assigned
        assertTrue(labels.containsValue("Best Overall"), "Should assign Best Overall label");
        assertTrue(labels.containsValue("Best Value") || labels.containsValue("Best Budget"),
                "Should assign at least one value/budget label");

        // Each product has at most one label
        assertEquals(labels.values().stream().distinct().count(), labels.size(),
                "All assigned labels should be unique");

        // No product appears more than once
        assertEquals(labels.keySet().stream().distinct().count(), labels.size(),
                "No product should have more than one label");

        // Label count should be reasonable (3-5 labels for 5 products)
        assertTrue(labels.size() >= 3 && labels.size() <= 5,
                "Should assign 3-5 labels for 5 products, got: " + labels.size());
    }

    @Test
    @DisplayName("Best for use case label is assigned when use case specified")
    void testBestForUseCaseLabel() {
        ParsedQuery query = ParsedQuery.builder()
                .category("laptop")
                .useCase("coding")
                .build();

        // L1: best overall and best value (will claim those labels)
        Product topProduct = createLaptop("L1", "Top Laptop", 50000, 4.9);
        topProduct.setTags(List.of("office", "student"));
        topProduct.setReviewCount(20000);

        // L2: generic high-rated laptop (will get best value)
        Product genericLaptop = createLaptop("L2", "Generic Laptop", 30000, 4.7);
        genericLaptop.setTags(List.of("office", "student"));
        genericLaptop.setReviewCount(5000);

        // L3: coding-optimized with strong specs — should get "Best for Coding"
        Product bestForCoding = createLaptop("L3", "Dev Laptop", 55000, 4.5);
        bestForCoding.setTags(List.of("coding", "power-user"));
        bestForCoding.setSpecs(new HashMap<>(Map.of(
                "RAM", "32 GB",
                "Storage", "1 TB SSD",
                "Processor", "Intel Core i9"
        )));
        bestForCoding.setReviewCount(3000);

        // L4: another generic laptop so L3 isn't consumed by "Best Budget" or "Best Rated"
        Product genericLaptop2 = createLaptop("L4", "Mid Laptop", 35000, 4.4);
        genericLaptop2.setTags(List.of("student", "travel"));
        genericLaptop2.setReviewCount(2000);

        List<Product> products = List.of(topProduct, genericLaptop, bestForCoding, genericLaptop2);
        Map<String, String> labels = labelService.assignLabels(products, query);

        assertTrue(labels.containsValue("Best for Coding"),
                "Should assign 'Best for Coding' label to the product with highest use case score");
        // L3 specifically should get the coding label
        assertEquals("Best for Coding", labels.get("L3"),
                "L3 with coding tag and strong specs should get Best for Coding label");
    }

    @Test
    @DisplayName("Lakh notation budget parsing works correctly")
    void testLakhBudgetParsing() {
        ParsedQuery query1 = queryParserService.parse("laptop under 1 lakh");
        assertEquals(100000L, query1.getBudget(), "1 lakh should parse to 100000");

        ParsedQuery query2 = queryParserService.parse("phone under 0.6 lakh");
        assertEquals(60000L, query2.getBudget(), "0.6 lakh should parse to 60000");

        ParsedQuery query3 = queryParserService.parse("headphones under 2 lac");
        assertEquals(200000L, query3.getBudget(), "2 lac should parse to 200000");
    }

    @Test
    @DisplayName("Price range parsing extracts min and max correctly")
    void testPriceRangeParsing() {
        ParsedQuery query1 = queryParserService.parse("laptop between 40k and 60k");
        assertEquals(40000L, query1.getMinPrice(), "Min price should be 40000");
        assertEquals(60000L, query1.getBudget(), "Max price should be 60000");

        ParsedQuery query2 = queryParserService.parse("phone from 20000 to 50000");
        assertEquals(20000L, query2.getMinPrice(), "Min price should be 20000");
        assertEquals(50000L, query2.getBudget(), "Max price should be 50000");

        ParsedQuery query3 = queryParserService.parse("headphones 5k to 15k");
        assertEquals(5000L, query3.getMinPrice(), "Min price should be 5000");
        assertEquals(15000L, query3.getBudget(), "Max price should be 15000");
    }

    @Test
    @DisplayName("Best value sort preference is detected from query")
    void testBestValueSortDetection() {
        ParsedQuery query1 = queryParserService.parse("show me best value laptops");
        assertEquals(SortPreference.VALUE, query1.getSortPreference(), 
                "Should detect VALUE sort from 'best value'");

        ParsedQuery query2 = queryParserService.parse("laptop with value for money");
        assertEquals(SortPreference.VALUE, query2.getSortPreference(), 
                "Should detect VALUE sort from 'value for money'");

        ParsedQuery query3 = queryParserService.parse("best deal phone");
        assertEquals(SortPreference.VALUE, query3.getSortPreference(), 
                "Should detect VALUE sort from 'best deal'");
    }

    @Test
    @DisplayName("Match percentage is calculated and within 0-100 range")
    void testMatchPercentageCalculation() {
        ParsedQuery query = ParsedQuery.builder()
                .category("laptop")
                .useCase("coding")
                .budget(60000L)
                .brand("Lenovo")
                .build();

        Product perfectMatch = createLaptop("L1", "Lenovo Coding Laptop", 45000, 4.8);
        perfectMatch.setBrand("Lenovo");
        perfectMatch.setTags(List.of("coding", "power-user"));
        perfectMatch.setSpecs(Map.of("RAM", "16 GB", "Storage", "512 GB SSD"));
        perfectMatch.setReviewCount(5000);

        ProductScore score = scoringService.calculateScore(perfectMatch, query);

        assertTrue(score.getMatchPercentage() >= 0.0, "Match percentage should be >= 0");
        assertTrue(score.getMatchPercentage() <= 100.0, "Match percentage should be <= 100");
        assertTrue(score.getMatchPercentage() > 50.0, "Perfect match should have high percentage");
    }

    @Test
    @DisplayName("Combined scoring factors work together correctly")
    void testCombinedScoringFactors() {
        ParsedQuery query = ParsedQuery.builder()
                .category("laptop")
                .useCase("coding")
                .budget(60000L)
                .brand("Lenovo")
                .build();

        Product wellRounded = createLaptop("L1", "Lenovo ThinkPad", 48000, 4.7);
        wellRounded.setBrand("Lenovo");
        wellRounded.setTags(List.of("coding", "office", "business"));
        wellRounded.setSpecs(Map.of(
                "RAM", "16 GB",
                "Storage", "512 GB SSD",
                "Processor", "Intel Core i7"
        ));
        wellRounded.setReviewCount(8000);

        ProductScore score = scoringService.calculateScore(wellRounded, query);

        // Verify all score components are positive
        assertTrue(score.getRelevanceScore() > 0, "Should have relevance score");
        assertTrue(score.getBudgetScore() > 0, "Should have budget score");
        assertTrue(score.getRatingScore() > 0, "Should have rating score");
        assertTrue(score.getPopularityScore() > 0, "Should have popularity score");
        assertTrue(score.getUseCaseScore() > 0, "Should have use case score");
        assertTrue(score.getValueScore() > 0, "Should have value score");
        assertTrue(score.getBrandScore() > 0, "Should have brand score");
        assertTrue(score.getSpecScore() > 0, "Should have spec score");

        // Overall score should be sum of weighted components
        assertTrue(score.getOverallScore() > 100.0, 
                "Well-rounded product should have high overall score");
    }

    // Helper methods
    private Product createLaptop(String id, String name, long price, double rating) {
        return Product.builder()
                .id(id)
                .name(name)
                .brand("Generic")
                .category("laptop")
                .price(price)
                .rating(rating)
                .reviewCount(1000)
                .description("Test laptop")
                .source(ProductSource.LOCAL)
                .tags(List.of("laptop", "office"))
                .specs(new HashMap<>())
                .build();
    }

    private Product createPhone(String id, String name, long price, double rating) {
        return Product.builder()
                .id(id)
                .name(name)
                .brand("Generic")
                .category("phone")
                .price(price)
                .rating(rating)
                .reviewCount(1000)
                .description("Test phone")
                .source(ProductSource.LOCAL)
                .tags(List.of("phone", "everyday"))
                .specs(new HashMap<>())
                .build();
    }
}
