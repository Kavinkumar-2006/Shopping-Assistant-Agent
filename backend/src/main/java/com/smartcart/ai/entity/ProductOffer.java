package com.smartcart.ai.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents an individual seller/retailer offer for a logical product.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductOffer {

    private String offerId;
    private ProductSource source;
    private String storeName;
    private long price;
    @Builder.Default
    private String currency = "INR";
    @Builder.Default
    private Boolean availability = true;
    private String productUrl;
    private String fetchedTime;
}
