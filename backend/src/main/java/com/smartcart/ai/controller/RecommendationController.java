package com.smartcart.ai.controller;

import com.smartcart.ai.dto.QueryRequest;
import com.smartcart.ai.dto.RecommendationResponse;
import com.smartcart.ai.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller exposing the ShopSmart AI recommendation endpoints.
 *
 * Endpoints:
 *   POST /api/chat/recommend  — Main recommendation endpoint
 *   GET  /api/health          — Health check
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * Accepts a natural-language shopping query and returns
     * a ranked list of recommended products with a summary.
     *
     * POST /api/chat/recommend
     * Body: { "message": "Suggest a laptop under 60000 for coding" }
     */
    @PostMapping("/chat/recommend")
    public ResponseEntity<RecommendationResponse> recommend(
            @Valid @RequestBody QueryRequest request) {

        log.info("Received recommendation request: '{}'", request.getMessage());

        RecommendationResponse response = recommendationService.recommend(request.getMessage());

        // Return 404-style payload if no products matched (still HTTP 200 for graceful frontend handling)
        if (response.getProducts() == null || response.getProducts().isEmpty()) {
            log.info("No products matched query: '{}'", request.getMessage());
            return ResponseEntity.ok(response);
        }

        log.info("Returning {} products for query: '{}'", response.getProducts().size(), request.getMessage());
        return ResponseEntity.ok(response);
    }

    /**
     * Simple health check endpoint.
     * GET /api/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "service", "ShopSmart AI",
            "version", "1.0.0"
        ));
    }

    /**
     * Global handler for validation errors (e.g., blank message).
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationError(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request");

        return ResponseEntity.badRequest().body(Map.of("error", errorMessage));
    }
}
