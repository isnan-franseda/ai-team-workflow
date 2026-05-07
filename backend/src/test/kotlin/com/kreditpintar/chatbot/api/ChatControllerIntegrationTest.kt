package com.kreditpintar.chatbot.api

import com.kreditpintar.chatbot.BaseIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ChatControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `POST api v1 chat session creates a session`() {
        mockMvc.perform(post("/api/v1/chat/session"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.session_id").isNotEmpty)
            .andExpect(jsonPath("$.created_at").isNotEmpty)
    }

    @Test
    fun `POST api v1 chat send returns 404 for unknown session`() {
        val body = """{"session_id":"00000000-0000-0000-0000-000000000000","message":"Hello","language":"id"}"""
        mockMvc.perform(
            post("/api/v1/chat/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `POST api v1 chat send returns 400 for empty message`() {
        // First create a session
        val sessionResponse = mockMvc.perform(post("/api/v1/chat/session"))
            .andExpect(status().isOk)
            .andReturn()

        val sessionJson = sessionResponse.response.contentAsString
        val sessionId = com.fasterxml.jackson.databind.ObjectMapper().readTree(sessionJson)
            .get("session_id").asText()

        val body = """{"session_id":"$sessionId","message":"","language":"id"}"""
        mockMvc.perform(
            post("/api/v1/chat/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET api v1 chat history returns 404 for unknown session`() {
        mockMvc.perform(get("/api/v1/chat/history/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isNotFound)
    }
}
