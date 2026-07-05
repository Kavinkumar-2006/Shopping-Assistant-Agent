package com.smartcart.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcart.ai.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Loads the product catalog from products.json into memory at startup.
 * Exposes the catalog as a Spring Bean for injection into services.
 */
@Slf4j
@Configuration
public class DataSeeder {

    /**
     * Reads products.json from the classpath and deserializes it into
     * a List<Product>. This bean is a singleton — loaded once at startup.
     */
    @Bean
    public List<Product> productCatalog(ObjectMapper objectMapper) {
        try {
            InputStream stream = new ClassPathResource("products.json").getInputStream();
            List<Product> products = objectMapper.readValue(stream, new TypeReference<List<Product>>() {});
            log.info("Successfully loaded {} products from catalog.", products.size());
            return products;
        } catch (IOException e) {
            log.error("Failed to load products.json: {}", e.getMessage());
            throw new RuntimeException("Could not initialize product catalog", e);
        }
    }
}
