package com.smartcart.ai.entity;

/**
 * Identifies the source/origin of a discovered product.
 *
 * <p>Values ending in _A/_B are generic placeholders for future retailer integrations.
 * AMAZON is a named placeholder — no implementation exists yet; credentials are optional.
 */
public enum ProductSource {
    /** Local development catalog — always available, no credentials required. */
    LOCAL,

    /** Flipkart Affiliate API — requires FLIPKART_AFFILIATE_ID + FLIPKART_AFFILIATE_TOKEN. */
    FLIPKART,

    /** Amazon Product Advertising API — placeholder, requires AMAZON_ACCESS_KEY + AMAZON_SECRET_KEY + AMAZON_PARTNER_TAG. */
    AMAZON,

    /** Generic external product API — requires PRODUCT_PROVIDER_BASE_URL + optional PRODUCT_PROVIDER_API_KEY. */
    PRODUCT_API,

    /** Generic retailer slot A — for future integrations. */
    RETAILER_A,

    /** Generic retailer slot B — for future integrations. */
    RETAILER_B
}
