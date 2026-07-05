package com.smartcart.ai.util;

import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility for formatting prices in Indian Rupee (₹) format.
 * Example: 52000 → "₹52,000"
 */
@Component
public class PriceFormatter {

    private static final Locale INDIA = new Locale("en", "IN");

    /**
     * Formats a long price value to Indian Rupee string.
     * @param price price in ₹
     * @return formatted string e.g. "₹52,000"
     */
    public String format(long price) {
        NumberFormat formatter = NumberFormat.getNumberInstance(INDIA);
        return "₹" + formatter.format(price);
    }

    /**
     * Formats a price range.
     * @return e.g. "₹18,000 – ₹60,000"
     */
    public String formatRange(long min, long max) {
        return format(min) + " – " + format(max);
    }
}
