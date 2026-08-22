package com.smartcart.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SessionChatRequest {
    @NotBlank(message = "Message cannot be empty")
    private String message;
    private String sessionId;
}
