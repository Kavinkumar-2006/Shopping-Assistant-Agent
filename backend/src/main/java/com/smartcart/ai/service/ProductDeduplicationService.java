package com.smartcart.ai.service;

import com.smartcart.ai.entity.Product;
import com.smartcart.ai.entity.ProductOffer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Reusable product deduplication service.
 * Identifies duplicate products across providers using normalized name, brand, and category signals.
 */
@Slf4j
@Service
public class ProductDeduplicationService {

    /**
     * Deduplicates a list of products while preserving order of first appearance.
     *
     * @param products candidate list from one or more providers
     * @return deduplicated list of products
     */
    public List<Product> deduplicate(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Product> uniqueMap = new LinkedHashMap<>();

        for (Product product : products) {
            if (product == null) continue;

            String key = generateDeduplicationKey(product);
            if (!uniqueMap.containsKey(key)) {
                uniqueMap.put(key, product);
            } else {
                Product existing = uniqueMap.get(key);
                Product merged = resolveDuplicate(existing, product);
                uniqueMap.put(key, merged);
            }
        }

        List<Product> result = new ArrayList<>(uniqueMap.values());
        if (result.size() < products.size()) {
            log.info("ProductDeduplicationService: reduced {} candidates to {} unique products",
                    products.size(), result.size());
        }
        return result;
    }

    /**
     * Generates a normalized deduplication fingerprint string for a product.
     */
    public String generateDeduplicationKey(Product p) {
        if (p == null) return "";
        String brand = p.getBrand() != null ? p.getBrand().trim().toLowerCase(Locale.ROOT) : "";
        String category = p.getCategory() != null ? p.getCategory().trim().toLowerCase(Locale.ROOT) : "";
        String name = p.getName() != null ? normalizeName(p.getName()) : "";

        return brand + "::" + category + "::" + name;
    }

    private String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "")
                .trim();
    }

    /**
     * Resolves between two duplicate products and collects merchant offers.
     */
    private Product resolveDuplicate(Product p1, Product p2) {
        Product winner = (p2.getRating() > p1.getRating() || (p2.getRating() == p1.getRating() && p2.getReviewCount() > p1.getReviewCount())) ? p2 : p1;
        Product secondary = winner == p1 ? p2 : p1;

        if (winner.getOffers() == null) {
            winner.setOffers(new ArrayList<>());
        }

        // Add primary offer
        if (winner.getOffers().isEmpty()) {
            winner.getOffers().add(ProductOffer.builder()
                    .offerId(winner.getId())
                    .source(winner.getSource())
                    .storeName(winner.getStoreName() != null ? winner.getStoreName() : "Primary Seller")
                    .price(winner.getPrice())
                    .currency(winner.getCurrency() != null ? winner.getCurrency() : "INR")
                    .availability(winner.getAvailability() != null ? winner.getAvailability() : true)
                    .productUrl(winner.getProductUrl())
                    .fetchedTime(winner.getFetchedTime())
                    .build());
        }

        // Add secondary offer
        winner.getOffers().add(ProductOffer.builder()
                .offerId(secondary.getId())
                .source(secondary.getSource())
                .storeName(secondary.getStoreName() != null ? secondary.getStoreName() : "Secondary Seller")
                .price(secondary.getPrice())
                .currency(secondary.getCurrency() != null ? secondary.getCurrency() : "INR")
                .availability(secondary.getAvailability() != null ? secondary.getAvailability() : true)
                .productUrl(secondary.getProductUrl())
                .fetchedTime(secondary.getFetchedTime())
                .build());

        return winner;
    }

}
