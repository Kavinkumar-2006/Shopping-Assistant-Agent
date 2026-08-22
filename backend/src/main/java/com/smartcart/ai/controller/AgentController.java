package com.smartcart.ai.controller;

import com.smartcart.ai.dto.QueryRequest;
import com.smartcart.ai.dto.RecommendationResponse;
import com.smartcart.ai.dto.SessionChatRequest;
import com.smartcart.ai.service.ShoppingAgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller exposing the V2 AI Shopping Agent chat endpoints.
 *
 * <p>Endpoints:
 *   POST /api/agent/chat  — Main agent chat orchestrator endpoint
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final ShoppingAgentService shoppingAgentService;

    /**
     * Accepts a conversational user message, runs the AI Shopping Agent workflow,
     * and returns recommendations, interpreted constraints, intent, and specification-based reasons.
     *
     * <p>POST /api/agent/chat
     * Body: { "message": "Suggest a laptop under 60000 for coding" }
     */
    @PostMapping("/chat")
    public ResponseEntity<RecommendationResponse> chat(
            @Valid @RequestBody QueryRequest request,
            jakarta.servlet.http.HttpServletRequest httpServletRequest) {

        log.info("Received AI Shopping Agent query: '{}'", request.getMessage());

        String sessionId = request.getSessionId();
        if ((sessionId == null || sessionId.isBlank()) && httpServletRequest != null) {
            String headerId = httpServletRequest.getHeader("X-Session-ID");
            if (headerId == null || headerId.isBlank()) headerId = httpServletRequest.getHeader("Session-ID");
            if (headerId != null && !headerId.isBlank()) sessionId = headerId;
            else if (httpServletRequest.getSession(false) != null) sessionId = httpServletRequest.getSession(false).getId();
        }

        RecommendationResponse response = (sessionId != null && !sessionId.isBlank())
                ? shoppingAgentService.processConversation(sessionId, request.getMessage())
                : shoppingAgentService.processQuery(request.getMessage());

        if (response.getProducts() == null || response.getProducts().isEmpty()) {
            log.info("AI Shopping Agent: no matches found for query '{}'", request.getMessage());
            return ResponseEntity.ok(response);
        }

        log.info("AI Shopping Agent: returning {} products with intent {} for query '{}'",
                response.getProducts().size(), response.getIntent(), request.getMessage());
        return ResponseEntity.ok(response);
    }

    /** Session-aware conversational endpoint. The original /api/chat/recommend endpoint remains available. */
    @PostMapping("/session/chat")
    public ResponseEntity<RecommendationResponse> sessionChat(@Valid @RequestBody SessionChatRequest request) {
        return ResponseEntity.ok(shoppingAgentService.processConversation(request.getSessionId(), request.getMessage()));
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> resetSession(@PathVariable String sessionId) {
        shoppingAgentService.resetConversation(sessionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Handler for input validation constraints.
     */
    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationError(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {

        String errorMsg = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request body details");

        return ResponseEntity.badRequest().body(Map.of("error", errorMsg));
    }
}
