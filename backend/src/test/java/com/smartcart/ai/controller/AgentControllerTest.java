package com.smartcart.ai.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testLegacyEndpointCompatibility() throws Exception {
        mockMvc.perform(post("/api/chat/recommend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"Suggest a laptop under 60000 for coding\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.category").value("laptop"))
                .andExpect(jsonPath("$.budget").value(60000))
                .andExpect(jsonPath("$.useCase").value("coding"))
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.topProducts").isArray())
                .andExpect(jsonPath("$.totalMatches").exists());
    }

    @Test
    void testNewAgentEndpoint() throws Exception {
        mockMvc.perform(post("/api/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"Suggest a laptop under 60000 for coding\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.category").value("laptop"))
                .andExpect(jsonPath("$.budget").value(60000))
                .andExpect(jsonPath("$.useCase").value("coding"))
                .andExpect(jsonPath("$.intent").value("RECOMMEND"))
                .andExpect(jsonPath("$.appliedFilters").isArray())
                .andExpect(jsonPath("$.recommendationReasons").isArray())
                .andExpect(jsonPath("$.confidence").exists())
                .andExpect(jsonPath("$.products").isArray());
    }

    @Test
    void testEmptyMessageValidationError() throws Exception {
        mockMvc.perform(post("/api/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
