package com.smartcart.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detailed scoring breakdown for a product recommendation.
 * Provides transparency into why a product was ranked in a specific position.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductScore {
    
    /** Product ID this score belongs to */
    private String productId;
    
    /** Overall combined score (0-100+) */
    private double overallScore;
    
    /** Query relevance score (0-50) */
    private double relevanceScore;
    
    /** Budget fit score (0-20) */
    private double budgetScore;
    
    /** Rating quality score (0-75) */
    private double ratingScore;
    
    /** Popularity score (0-10) */
    private double popularityScore;
    
    /** Use case match score (0-30) */
    private double useCaseScore;
    
    /** Value for money score (0-25) */
    private double valueScore;
    
    /** Brand preference score (0-30) */
    private double brandScore;
    
    /** Specification quality score (0-20) */
    private double specScore;
    
    /** Match percentage (0-100%) */
    private double matchPercentage;
    
    /** Recommendation label (Best Overall, Best Value, etc.) */
    private String recommendationLabel;
}
