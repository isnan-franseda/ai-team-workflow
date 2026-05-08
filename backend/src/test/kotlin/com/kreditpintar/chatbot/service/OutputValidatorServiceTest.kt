package com.kreditpintar.chatbot.service

import com.kreditpintar.chatbot.config.ChatClient
import com.kreditpintar.chatbot.config.ChatbotProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class OutputValidatorServiceTest {
    private lateinit var chatClient: ChatClient
    private lateinit var properties: ChatbotProperties
    private lateinit var outputValidatorService: OutputValidatorService

    @BeforeEach
    fun setUp() {
        chatClient = mock()
        properties =
            ChatbotProperties().apply {
                fallbackMessage = "Maaf, saya tidak dapat menjawab."
            }
        outputValidatorService = OutputValidatorService(chatClient, properties)
    }

    @Test
    fun `validation passes when LLM returns PASS`() {
        whenever(chatClient.validateOutput(org.mockito.kotlin.any(), org.mockito.kotlin.any()))
            .thenReturn("PASS\nAlasan: Respons sesuai standar")

        val result = outputValidatorService.validate("Pinjaman tersedia untuk Anda.", "session-1")

        assertTrue(result.passed)
        assertEquals(null, result.reason)
    }

    @Test
    fun `validation fails when LLM returns FAIL with loan promise`() {
        whenever(chatClient.validateOutput(org.mockito.kotlin.any(), org.mockito.kotlin.any()))
            .thenReturn("FAIL\nAlasan: Menjanjikan persetujuan pinjaman")

        val result = outputValidatorService.validate("Pinjaman Anda pasti disetujui!", "session-2")

        assertFalse(result.passed)
        assertEquals("Menjanjikan persetujuan pinjaman", result.reason)
    }

    @Test
    fun `validation fails when LLM call throws exception`() {
        whenever(chatClient.validateOutput(org.mockito.kotlin.any(), org.mockito.kotlin.any()))
            .thenThrow(RuntimeException("API error"))

        val result = outputValidatorService.validate("Some response", "session-3")

        assertFalse(result.passed)
    }

    @Test
    fun `validation is case insensitive for PASS`() {
        whenever(chatClient.validateOutput(org.mockito.kotlin.any(), org.mockito.kotlin.any()))
            .thenReturn("pass\nAlasan: OK")

        val result = outputValidatorService.validate("Test response", "session-4")

        assertTrue(result.passed)
    }
}
