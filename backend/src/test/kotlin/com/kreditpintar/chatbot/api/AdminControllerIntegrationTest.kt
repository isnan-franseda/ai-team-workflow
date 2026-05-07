package com.kreditpintar.chatbot.api

import com.kreditpintar.chatbot.BaseIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AdminControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `GET admin documents returns 401 without admin key`() {
        mockMvc.perform(get("/admin/documents"))
            .andExpect(status().is4xxClientError)
    }

    @Test
    fun `GET admin documents returns 401 for wrong admin key`() {
        mockMvc.perform(
            get("/admin/documents")
                .header("X-Admin-Key", "wrong-key")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET admin documents returns empty list for valid key`() {
        mockMvc.perform(
            get("/admin/documents")
                .header("X-Admin-Key", "test-admin-key")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.documents").isArray)
            .andExpect(jsonPath("$.total").value(0))
    }

    @Test
    fun `GET admin documents filters by doc_type`() {
        mockMvc.perform(
            get("/admin/documents?doc_type=FAQ")
                .header("X-Admin-Key", "test-admin-key")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.documents").isArray)
    }
}
