package com.smartcart.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound DTO for the /api/chat/recommend endpoint.
 * Carries the user's raw natural-language shopping query.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryRequest {

    @NotBlank(message = "Message cannot be empty")
    private String message;
}
