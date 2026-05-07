package com.kreditpintar.chatbot

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SecurityTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `admin documents endpoint requires X-Admin-Key header`() {
        mockMvc.perform(get("/admin/documents"))
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun `admin ingest endpoint rejects invalid key`() {
        mockMvc.perform(
            post("/admin/ingest")
                .header("X-Admin-Key", "invalid")
                .contentType(MediaType.MULTIPART_FORM_DATA)
        )
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun `chat endpoints do not require auth`() {
        mockMvc.perform(post("/api/v1/chat/session"))
            .andExpect(status().isOk)
    }

    @Test
    fun `non-existent endpoints return 404 not 500`() {
        mockMvc.perform(get("/api/v1/nonexistent"))
            .andExpect(status().isNotFound)
    }
}
