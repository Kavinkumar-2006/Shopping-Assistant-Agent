package com.smartcart.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound DTO for recommendation endpoints.
 * Carries the user's raw natural-language shopping query and an optional session ID.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {

    @NotBlank(message = "Message cannot be empty")
    private String message;

    private String sessionId;
}

